package de.westnordost.streetcomplete.data.download

import de.westnordost.streetcomplete.ApplicationConstants
import de.westnordost.streetcomplete.data.AuthorizationException
import de.westnordost.streetcomplete.data.download.tiles.DownloadedTilesController
import de.westnordost.streetcomplete.data.download.tiles.DownloadedTilesSource
import de.westnordost.streetcomplete.data.download.tiles.TilesRect
import de.westnordost.streetcomplete.data.download.tiles.enclosingTilesRect
import de.westnordost.streetcomplete.data.maptiles.MapTilesDownloader
import de.westnordost.streetcomplete.data.osm.mapdata.BoundingBox
import de.westnordost.streetcomplete.data.osm.mapdata.MapDataDownloader
import de.westnordost.streetcomplete.data.osmnotes.NotesDownloader
import de.westnordost.streetcomplete.data.user.UserLoginController
import de.westnordost.streetcomplete.util.Listeners
import de.westnordost.streetcomplete.util.ktx.format
import de.westnordost.streetcomplete.util.ktx.nowAsEpochMilliseconds
import de.westnordost.streetcomplete.util.logs.Log
import de.westnordost.streetcomplete.util.math.area
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.Volatile
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.max

/** Downloads all the things */
class Downloader(
    private val notesDownloader: NotesDownloader,
    private val mapDataDownloader: MapDataDownloader,
    private val mapTilesDownloader: MapTilesDownloader,
    private val downloadedTilesController: DownloadedTilesController,
    private val userLoginController: UserLoginController,
    private val mutex: Mutex
) : DownloadProgressSource {

    private val listeners = Listeners<DownloadProgressSource.Listener>()

    override var isUserInitiatedDownloadInProgress: Boolean = false
        private set

    override var isDownloadInProgress: Boolean = false
        private set

    /** The running download and DownloadedTilesController.invalidationCount when it started. */
    @Volatile private var runningDownload: Job? = null
    @Volatile private var invalidationCountAtStart = 0

    // all tiles invalidated while downloading (e.g. workspace switched): the running download is
    // outdated - cancel it rather than wait for it, a hanging map tiles download (tile server
    // unreachable) would otherwise keep the next workspace from downloading anything
    private val downloadedTilesListener = object : DownloadedTilesSource.Listener {
        override fun onUpdated() {
            if (downloadedTilesController.invalidationCount != invalidationCountAtStart) {
                runningDownload?.cancel()
            }
        }
    }

    init {
        downloadedTilesController.addListener(downloadedTilesListener)
    }

    suspend fun download(bbox: BoundingBox, isUserInitiated: Boolean) {
        var hasError = false
        try {
            isDownloadInProgress = true
            isUserInitiatedDownloadInProgress = isUserInitiated
            listeners.forEach { it.onStarted() }

            val tiles = bbox.enclosingTilesRect(ApplicationConstants.DOWNLOAD_TILE_ZOOM)
            val tilesBbox = tiles.asBoundingBox(ApplicationConstants.DOWNLOAD_TILE_ZOOM)
            val bboxString = listOf(
                tilesBbox.min.latitude.format(7),
                tilesBbox.min.longitude.format(7),
                tilesBbox.max.latitude.format(7),
                tilesBbox.max.longitude.format(7)
            ).joinToString(",")
            val sqkm = (tilesBbox.area() / 1000 / 1000).format(1)

            if (!isUserInitiated && hasDownloadedAlready(tiles)) {
                Log.i(TAG, "Not downloading ($sqkm km², bbox: $bboxString), data still fresh")
                return
            }
            Log.i(TAG, "Starting download ($sqkm km², bbox: $bboxString)")

            val time = nowAsEpochMilliseconds()
            val invalidationCount = downloadedTilesController.invalidationCount
            invalidationCountAtStart = invalidationCount

            coroutineScope {
                runningDownload = coroutineContext[Job]
                // all downloaders run concurrently. Only the map data download holds the lock
                // shared with the uploader (both write OSM data) - map tiles don't touch it, and
                // MapLibre's tile download waits for connectivity rather than failing, so with the
                // tile server unreachable it would otherwise block uploads indefinitely
                // launch { notesDownloader.download(tilesBbox) }
                launch { mutex.withLock { mapDataDownloader.download(tilesBbox) } }
                launch { mapTilesDownloader.download(tilesBbox) }
            }
            // invalidated while downloading (e.g. workspace switched): the next auto-download
            // (triggered by onSuccess below) has to fetch it again instead of skipping it as fresh
            if (downloadedTilesController.invalidationCount == invalidationCount) {
                putDownloadedAlready(tiles)
            } else {
                Log.i(TAG, "Downloaded tiles were invalidated during the download, not marking them as downloaded")
            }

            val seconds = (nowAsEpochMilliseconds() - time) / 1000.0
            Log.i(TAG, "Finished download ($sqkm km², bbox: $bboxString) in ${seconds.format(1)}s")
        } catch (e: CancellationException) {
            if (currentCoroutineContext().isActive) {
                // only the download itself was cancelled, as outdated (see downloadedTilesListener)
                // - not an error: onSuccess below lets the auto-download start a fresh one
                Log.i(TAG, "Download cancelled, tiles were invalidated during the download")
            } else {
                hasError = true
                Log.i(TAG, "Download cancelled")
            }
        } catch (e: Exception) {
            hasError = true
            Log.e(TAG, "Unable to download", e)
            if (e is AuthorizationException) {
                userLoginController.logOut()
            }
            listeners.forEach { it.onError(e) }
            throw e
        } finally {
            runningDownload = null
            isDownloadInProgress = false
            isUserInitiatedDownloadInProgress = false
            listeners.forEach { it.onFinished() }
            if (!hasError) listeners.forEach { it.onSuccess() }
        }
    }

    override fun addListener(listener: DownloadProgressSource.Listener) {
        listeners.add(listener)
    }
    override fun removeListener(listener: DownloadProgressSource.Listener) {
        listeners.remove(listener)
    }

    private fun hasDownloadedAlready(tiles: TilesRect): Boolean {
        val freshTime = ApplicationConstants.REFRESH_DATA_AFTER
        val ignoreOlderThan = max(0, nowAsEpochMilliseconds() - freshTime)
        return downloadedTilesController.contains(tiles, ignoreOlderThan)
    }

    private fun putDownloadedAlready(tiles: TilesRect) {
        downloadedTilesController.put(tiles)
    }

    companion object {
        const val TAG = "Download"
    }
}
