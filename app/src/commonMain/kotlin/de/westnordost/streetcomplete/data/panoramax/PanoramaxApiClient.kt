package de.westnordost.streetcomplete.data.panoramax

import de.westnordost.streetcomplete.data.osm.mapdata.LatLon
import de.westnordost.streetcomplete.data.panoramax.domain.model.CreateUploadSetRequest
import de.westnordost.streetcomplete.data.panoramax.domain.model.PatchPictureRequest
import de.westnordost.streetcomplete.data.panoramax.domain.model.SemanticTag
import de.westnordost.streetcomplete.data.panoramax.domain.model.UploadSet
import de.westnordost.streetcomplete.data.panoramax.domain.model.UploadSetFile
import de.westnordost.streetcomplete.data.photos.CapturedPhoto
import de.westnordost.streetcomplete.data.preferences.EnvironmentManager
import de.westnordost.streetcomplete.util.ktx.nowAsEpochMilliseconds
import de.westnordost.streetcomplete.util.ktx.systemTimeNow
import de.westnordost.streetcomplete.util.logs.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.io.buffered
import kotlinx.io.files.FileSystem
import kotlinx.io.files.Path
import kotlinx.io.readByteArray
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** A photo to upload: the image file at [path], the compass [bearing] (0-359 clockwise from
 *  true north) the camera was facing at capture time - null if unknown, then none is sent - and
 *  semantic tags to attach to the picture, on top of the ones every picture gets (see
 *  [PanoramaxApiClient.upload]). */
data class PanoramaxPhoto(
    val path: String,
    val bearing: Float? = null,
    val semantics: Map<String, String> = emptyMap(),
    /** when the photo was taken, epoch milliseconds - the upload time if null */
    val captureTime: Long? = null,
)

/** This photo as uploaded to Panoramax, with [semantics]. If when it was taken is unknown (e.g.
 *  queued before that was recorded), [fallbackCaptureTime] is used: the time its edit was made,
 *  right after the photo was taken, which is much closer than the upload time. */
fun CapturedPhoto.toPanoramaxPhoto(
    fallbackCaptureTime: Long,
    semantics: Map<String, String> = emptyMap(),
) = PanoramaxPhoto(path, bearing, semantics, capturedAt ?: fallbackCaptureTime)

/** Uploads photos to our Panoramax instance, see
 *  https://docs.panoramax.fr/backend/api/api/ - via the upload set API: create an upload set, add
 *  the files to it, complete it. The server then turns it into a sequence (collection) and
 *  processes (blurs) the pictures in the background. */
class PanoramaxApiClient(
    private val fileSystem: FileSystem,
    private val httpClient: HttpClient,
    private val environmentManager: EnvironmentManager,
    /** overrides the current environment's token (tests) */
    private val accessTokenOverride: String? = null,
) {
    private val baseUrl: String get() = environmentManager.currentEnvironment.panoramaxBaseUrl
    private val accessToken: String
        get() = accessTokenOverride ?: environmentManager.currentEnvironment.panoramaxAccessToken
    private val preferences get() = environmentManager.preferences

    /** Uploads [photos] into one new Panoramax upload set at [position], in list order, completes
     *  the upload set and returns the HD picture URL for each uploaded image, in the same order.
     *  Photos whose file doesn't exist are skipped.
     *
     *  Every picture gets the semantic tag [WORKSPACE_ID] of the logged-in user, plus the photo's
     *  own [PanoramaxPhoto.semantics]. Nothing identifying the user (e.g. their email) is sent -
     *  the pictures are public.
     *
     *  The URLs are known right away (they only depend on the picture id), but only answer with
     *  the image once the server has processed the picture, usually within seconds.
     *
     *  @throws PanoramaxException naming the step that failed */
    suspend fun upload(
        photos: List<PanoramaxPhoto>,
        position: LatLon,
    ): List<String> {
        val images = photos.mapNotNull { photo ->
            val file = Path(photo.path)
            if (fileSystem.exists(file)) {
                photo to fileSystem.source(file).buffered().use { it.readByteArray() }
            } else null
        }
        if (images.isEmpty()) return emptyList()
        try {
            val commonSemantics = getCommonSemantics()
            val uploadSetId = createUploadSet(images.size)
            val pictureIds = images.mapIndexed { index, (photo, image) ->
                uploadPhoto(
                    uploadSetId, index + 1, image, position, photo.bearing,
                    photo.captureTime ?: nowAsEpochMilliseconds(),
                    commonSemantics + photo.semantics
                )
            }
            completeUploadSet(uploadSetId)
            return pictureIds.map { pictureId -> "${baseUrl}api/pictures/$pictureId/hd.jpg" }
        } catch (e: CancellationException) {
            throw e
        } catch (e: PanoramaxException) {
            throw e
        }
        // Panoramax not answering at all (no connection, DNS failure - an
        // UnresolvedAddressException, which isn't an IOException - timeout, reset) or answering
        // with something unparseable is a photo upload failure like any error status: callers
        // treat PanoramaxException as "leave the edit for the next sync" and count it towards the
        // stuck-photo notice. Anything else would abort the whole upload run.
        catch (e: Exception) {
            throw PanoramaxException(
                "Could not reach Panoramax. Image upload failed. Please try again later: $e",
                isUnreachable = true
            )
        }
    }

    /** Adds [semantics] to the already uploaded picture at [pictureUrl] (as returned by
     *  [upload]). Returns false if [pictureUrl] isn't a picture of this Panoramax instance (e.g. a
     *  photo uploaded to KartaView before the switch) - nothing to do then.
     *
     *  @throws PanoramaxException if it failed */
    suspend fun addSemantics(pictureUrl: String, semantics: Map<String, String>): Boolean {
        val pictureId = getPictureId(pictureUrl) ?: return false
        val response = try {
            httpClient.patch(baseUrl + "api/pictures/$pictureId") {
                bearerAuth(accessToken)
                contentType(ContentType.Application.Json)
                setBody(PatchPictureRequest(semantics.toSemanticTags()))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw PanoramaxException("Could not reach Panoramax: $e", isUnreachable = true)
        }
        if (response.status != HttpStatusCode.OK) {
            throw PanoramaxException(
                "Failed to add semantics to Panoramax picture $pictureId " + response.errorText()
            )
        }
        Log.d(TAG, "Added semantics ${semantics.keys} to picture $pictureId")
        return true
    }

    private fun getPictureId(pictureUrl: String): String? {
        val prefix = baseUrl + "api/pictures/"
        if (!pictureUrl.startsWith(prefix)) return null
        return pictureUrl.removePrefix(prefix).substringBefore('/').takeIf { it.isNotEmpty() }
    }

    private fun getCommonSemantics(): Map<String, String> = buildMap {
        preferences.workspaceId?.let { put(WORKSPACE_ID, it.toString()) }
    }

    private suspend fun createUploadSet(fileCount: Int): String {
        val response = httpClient.post(baseUrl + "api/upload_sets") {
            bearerAuth(accessToken)
            contentType(ContentType.Application.Json)
            setBody(CreateUploadSetRequest(
                title = "AVIV ScoutRoute ${systemTimeNow()}",
                estimatedNbFiles = fileCount,
            ))
        }
        if (response.status != HttpStatusCode.OK) {
            throw PanoramaxException(
                "Failed to create Panoramax upload set. Image upload failed. Please try again later " +
                    response.errorText()
            )
        }
        val uploadSet = response.body<UploadSet>()
        Log.d(TAG, "Upload set created: ${uploadSet.id}")
        return uploadSet.id
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun uploadPhoto(
        uploadSetId: String,
        index: Int,
        image: ByteArray,
        position: LatLon,
        bearing: Float?,
        /** epoch milliseconds */
        captureTime: Long,
        semantics: Map<String, String>,
    ): String {
        // the (rescaled) photos carry no position and time in their EXIF any more, so these are
        // given as overrides - Panoramax rejects pictures without. The time is when it was taken,
        // not now: photos are often uploaded much later, see ElementEditsUploader. Same for the
        // heading - but only if known: 0 would claim the photo faces north
        val heading = bearing?.roundToInt()?.mod(360)
        val response = httpClient.post(baseUrl + "api/upload_sets/$uploadSetId/files") {
            bearerAuth(accessToken)
            setBody(MultiPartFormDataContent(formData {
                append("override_latitude", position.latitude.toString())
                append("override_longitude", position.longitude.toString())
                // ISO 8601, e.g. 2026-11-10T08:30:00Z
                append("override_capture_time", Instant.fromEpochMilliseconds(captureTime).toString())
                if (heading != null) {
                    append("override_Exif.GPSInfo.GPSImgDirection", "$heading/1")
                    append("override_Exif.GPSInfo.GPSImgDirectionRef", "T")
                }
                if (semantics.isNotEmpty()) {
                    // must be a JSON part, see the API docs
                    append(
                        "semantics",
                        json.encodeToString(semantics.toSemanticTags()),
                        Headers.build { append(HttpHeaders.ContentType, "application/json") }
                    )
                }
                append("file", image, Headers.build {
                    append(HttpHeaders.ContentType, "image/jpeg")
                    append(HttpHeaders.ContentDisposition, "filename=\"asr-$index.jpg\"")
                })
            }))
        }
        if (response.status != HttpStatusCode.Accepted) {
            throw PanoramaxException(
                "Failed to upload image to Panoramax. Please try again later " + response.errorText()
            )
        }
        val file = response.body<UploadSetFile>()
        file.rejected?.let { rejection ->
            throw PanoramaxException(
                "Panoramax rejected the image (${rejection.reason}): ${rejection.message}"
            )
        }
        val pictureId = file.pictureId ?: throw PanoramaxException(
            "Panoramax did not return a picture id for the uploaded image"
        )
        Log.d(TAG, "Image $index uploaded to upload set $uploadSetId as picture $pictureId")
        return pictureId
    }

    private suspend fun completeUploadSet(uploadSetId: String) {
        val response = httpClient.post(baseUrl + "api/upload_sets/$uploadSetId/complete") {
            bearerAuth(accessToken)
        }
        if (response.status != HttpStatusCode.OK) {
            throw PanoramaxException(
                "Failed to complete Panoramax upload set. Please try again later " + response.errorText()
            )
        }
        Log.d(TAG, "Upload set $uploadSetId completed")
    }

    private fun Map<String, String>.toSemanticTags() = map { (key, value) -> SemanticTag(key, value) }

    private suspend fun HttpResponse.errorText(): String =
        "$status: ${runCatching { bodyAsText().take(500) }.getOrDefault("")}"

    companion object {
        private const val TAG = "PanoramaxApiClient"
        private val json = Json

        /** semantic tag keys put on the uploaded pictures */
        const val WORKSPACE_ID = "workspace_id"
        /** the OSM element the photo is of, e.g. "way/123" */
        const val ELEMENT_ID = "element_id"
        /** the long-form question the photo was taken for */
        const val QUESTION = "question"
        /** the quest_id of that long-form question */
        const val QUEST_ID = "quest_id"
        /** the feature preset name of the new feature the photo is of, e.g. "Intersection" */
        const val FEATURE_TYPE = "feature_type"
        /** what the photo was taken for, see [SOURCE_NEW_FEATURE] */
        const val SOURCE = "source"
        /** [SOURCE] of the photos of a new feature (Add Feature) */
        const val SOURCE_NEW_FEATURE = "new_feature"
    }
}

/** @property isUnreachable Panoramax didn't answer at all (no connection, timeout, ...) - unlike an
 *  error status for this one upload, the next upload right after would most likely fail the same */
class PanoramaxException(
    message: String,
    val isUnreachable: Boolean = false,
) : RuntimeException(message)
