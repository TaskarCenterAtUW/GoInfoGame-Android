package de.westnordost.streetcomplete.quests.create_feature

import android.content.Context
import android.graphics.BitmapFactory
import de.westnordost.streetcomplete.util.logs.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/** Persistent store for workspace-defined custom icons (see the "custom-icons" key of the
 *  long-form schema). Each icon URL is downloaded at most once and kept in the app's files
 *  directory (not the cache directory, so the system never evicts it), keyed by a hash of the
 *  URL - if the workspace admin changes an icon's URL, the new URL is simply a new cache entry. */
class CustomIconCache(
    private val context: Context,
    private val httpClient: HttpClient,
) {
    private val mutex = Mutex()
    private val inFlight = mutableMapOf<String, Deferred<File?>>()

    private val dir: File
        get() = File(context.filesDir, "custom_icons").apply { mkdirs() }

    /* ------------------------------- index table (url -> file) ------------------------------- */

    private val indexFile: File get() = File(dir, INDEX_FILE_NAME)
    private var index: MutableMap<String, String>? = null

    /** Table of what is cached, keyed by URL, persisted as index.json in the cache dir */
    private fun loadIndex(): MutableMap<String, String> = synchronized(this) {
        index ?: run {
            val loaded: MutableMap<String, String> = try {
                if (indexFile.exists()) Json.decodeFromString(indexFile.readText()) else mutableMapOf()
            } catch (e: Exception) {
                Log.w(TAG, "Could not read custom icon index, starting fresh", e)
                mutableMapOf()
            }
            index = loaded
            loaded
        }
    }

    private fun recordInIndex(url: String, file: File) = synchronized(this) {
        val table = loadIndex()
        if (table[url] != file.name) {
            table[url] = file.name
            saveIndex(table)
        }
    }

    private fun removeFromIndex(url: String) = synchronized(this) {
        val table = loadIndex()
        if (table.remove(url) != null) saveIndex(table)
    }

    private fun saveIndex(table: Map<String, String>) {
        try {
            val tmp = File(dir, "$INDEX_FILE_NAME.tmp")
            tmp.writeText(Json.encodeToString(table))
            if (!tmp.renameTo(indexFile)) tmp.delete()
        } catch (e: Exception) {
            Log.w(TAG, "Could not persist custom icon index", e)
        }
    }

    /* -------------------------------------- lookup ------------------------------------------- */

    /** Returns the already-downloaded icon file for the given URL, or null if not cached yet.
     *  Consults the index table first; on a table miss, falls back to the URL-hash file mapping
     *  (covers icons cached before the table existed) and backfills the table. A cached file
     *  that turns out not to be a decodable image (e.g. an HTML error page an earlier version
     *  cached from a dead link) is deleted, so it gets re-downloaded. */
    fun getCached(url: String): File? {
        val fromTable = loadIndex()[url]
            ?.let { File(dir, it) }
            ?.takeIf { it.exists() && it.length() > 0 }
        val file = fromTable
            ?: fileFor(url).takeIf { it.exists() && it.length() > 0 }
            ?: run {
                removeFromIndex(url) // table pointed at a file that no longer exists
                return null
            }
        if (!isImage(file.readBytes())) {
            Log.w(TAG, "Deleting cached custom icon that is not a decodable image: $url")
            file.delete()
            removeFromIndex(url)
            return null
        }
        if (fromTable == null) recordInIndex(url, file)
        return file
    }

    /** Returns the icon file for the given URL, downloading it first if this URL has never been
     *  fetched before. Returns null if the download fails (will be retried on the next call). */
    suspend fun getOrDownload(url: String): File? {
        getCached(url)?.let { return it }

        // concurrent requests for the same URL (e.g. two presets sharing an icon) await the
        // same download instead of racing each other
        val (deferred, isOwner) = mutex.withLock {
            val existing = inFlight[url]
            if (existing != null) {
                existing to false
            } else {
                val d = CompletableDeferred<File?>()
                inFlight[url] = d
                d to true
            }
        }
        if (!isOwner) return deferred.await()

        val result = download(url)
        (deferred as CompletableDeferred).complete(result)
        mutex.withLock { inFlight.remove(url) }
        return result
    }

    private suspend fun download(url: String): File? = withContext(Dispatchers.IO) {
        try {
            val response = httpClient.get(url)
            if (!response.status.isSuccess()) {
                Log.w(TAG, "Downloading custom icon failed with ${response.status}: $url")
                return@withContext null
            }
            val bytes = response.body<ByteArray>()
            if (!isImage(bytes)) {
                // e.g. expired file-sharing links answer 200 with an HTML page - never cache that
                Log.w(TAG, "Custom icon URL did not return a decodable image: $url")
                return@withContext null
            }
            // write to a temp file first so a partial write can never be mistaken for a cached icon
            val file = fileFor(url)
            val tmp = File(dir, file.name + ".tmp")
            tmp.writeBytes(bytes)
            if (!tmp.renameTo(file)) {
                tmp.delete()
                return@withContext null
            }
            recordInIndex(url, file)
            file
        } catch (e: Exception) {
            Log.w(TAG, "Downloading custom icon failed: $url", e)
            null
        }
    }

    /** Whether the bytes are something the app can display as an icon: any bitmap format
     *  BitmapFactory recognizes, or an SVG (rendered via coil-svg) */
    private fun isImage(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return false
        val head = bytes.decodeToString(0, minOf(bytes.size, 256), throwOnInvalidSequence = false)
            .trimStart()
        if (head.startsWith("<svg", ignoreCase = true) ||
            (head.startsWith("<?xml", ignoreCase = true) && bytes.decodeToString(throwOnInvalidSequence = false).contains("<svg", ignoreCase = true))
        ) {
            return true
        }
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        return options.outWidth > 0 && options.outHeight > 0
    }

    private fun fileFor(url: String): File {
        val hash = MessageDigest.getInstance("MD5").digest(url.encodeToByteArray())
            .joinToString("") { "%02x".format(it) }
        return File(dir, hash)
    }

    companion object {
        private const val TAG = "CustomIconCache"
        private const val INDEX_FILE_NAME = "index.json"
    }
}
