package de.westnordost.streetcomplete.data.karta_view

import de.westnordost.streetcomplete.data.karta_view.domain.model.CreateSequenceResponse
import de.westnordost.streetcomplete.data.karta_view.domain.model.ImageUploadResponse
import de.westnordost.streetcomplete.data.karta_view.domain.model.PhotoLookupResponse
import de.westnordost.streetcomplete.data.osm.mapdata.LatLon
import de.westnordost.streetcomplete.data.preferences.Preferences
import de.westnordost.streetcomplete.util.logs.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
import kotlinx.io.buffered
import kotlinx.io.files.FileSystem
import kotlinx.io.files.Path
import kotlinx.io.readByteArray

/** Uploads photos to KartaView (api.openstreetcam.org). */
class KartaViewApiClient(
    private val fileSystem: FileSystem,
    private val httpClient: HttpClient,
    private val prefs: Preferences,
) {
    /** Uploads the image files at [photos] (path to the compass bearing, 0-359 clockwise from
     *  north, the device was facing at capture time - 0f where a caller doesn't track it) into
     *  one new KartaView sequence at [position], in list order. Paths that don't exist are
     *  skipped, along with their bearing. Returns the lth photo URL for each uploaded image, in
     *  the same order.
     *
     *  @throws KartaViewException naming the step that failed */
    suspend fun upload(
        photos: List<Pair<String, Float>>,
        position: LatLon,
    ): List<String> {
        val images = photos.mapNotNull { (path, bearing) ->
            val file = Path(path)
            if (fileSystem.exists(file)) {
                fileSystem.source(file).buffered().readByteArray() to bearing
            } else null
        }
        return uploadImages(images, position)
    }

    /** Uploads [images] (JPEG-encoded bytes, paired with the capture bearing for each) into one
     *  new KartaView sequence at [position], in list order (sequenceIndex 1..n), closes the
     *  sequence and returns the lth photo URL for each image, in the same order.
     *
     *  @throws KartaViewException naming the step that failed */
    suspend fun uploadImages(
        images: List<Pair<ByteArray, Float>>,
        position: LatLon,
    ): List<String> {
        if (images.isEmpty()) return emptyList()
        val sequenceId = createSequence()
        val photoIds = images.mapIndexed { index, (image, bearing) ->
            uploadPhoto(sequenceId, index + 1, image, position, bearing)
        }
        closeSequence(sequenceId)
        return photoIds.map { photoId -> getPhotoLthUrl(photoId) }
    }

    private suspend fun createSequence(): String {
        val response = httpClient.post(BASE_URL + "1.0/sequence/") {
            setBody(MultiPartFormDataContent(formData {
                append("access_token", prefs.kartaViewAccessToken)
            }))
        }
        if (response.status == HttpStatusCode.OK) {
            val sequence = response.body<CreateSequenceResponse>()
            Log.d(TAG, "Sequence created: ${sequence.status.httpMessage}")
            sequence.osv.sequence?.id?.let { return it }
        }
        throw KartaViewException(
            "Failed to create KartaView sequence. Image upload failed. Please try again later " + response.status
        )
    }

    private suspend fun uploadPhoto(
        sequenceId: String,
        sequenceIndex: Int,
        image: ByteArray,
        position: LatLon,
        bearing: Float,
    ): String {
        val response = httpClient.post(BASE_URL + "1.0/photo/") {
            setBody(MultiPartFormDataContent(formData {
                append("access_token", prefs.kartaViewAccessToken)
                append("sequenceId", sequenceId)
                append("sequenceIndex", sequenceIndex)
                append("coordinate", "${position.latitude},${position.longitude}")
                append("headers", bearing.toInt().toString())
                append("photo", image, Headers.build {
                    append(HttpHeaders.ContentType, "image/jpeg")
                    append(HttpHeaders.ContentDisposition, "filename=\"wework-kartaview.jpg\"")
                })
            }))
        }
        if (response.status != HttpStatusCode.OK) {
            throw KartaViewException(
                "Failed to upload image to KartaView. Please try again later " + response.status
            )
        } else {
            val imageResponse = response.body<ImageUploadResponse>()
            val imageId = imageResponse.osv.photo.id
            Log.d(TAG, "Image $sequenceIndex uploaded to sequence $sequenceId and photo ID $imageId")
            return imageId
        }
    }

    private suspend fun closeSequence(sequenceId: String) {
        val response = httpClient.post(BASE_URL + "1.0/sequence/finished-uploading/") {
            setBody(MultiPartFormDataContent(formData {
                append("access_token", prefs.kartaViewAccessToken)
                append("sequenceId", sequenceId)
            }))
        }
        if (response.status != HttpStatusCode.OK) {
            throw KartaViewException(
                "Failed to close KartaView Sequence. Please try again later " + response.status
            )
        }
        Log.d(TAG, "Sequence closed: ${response.body<CreateSequenceResponse>().status.httpMessage}")
    }

    private suspend fun getPhotoLthUrl(photoId: String): String {
        // KartaView's lookup endpoint is eventually consistent - right after closeSequence()
        // succeeds, a lookup for the photo that was just uploaded can still come back 200 OK
        // with no data yet because the server hasn't indexed it. That is not a real failure, so
        // retry with backoff before giving up, instead of failing the whole edit upload (and
        // leaving the photo/sequence already created on KartaView orphaned, since the caller
        // will re-upload from scratch on the next attempt).
        var attempt = 1
        var delayMillis = 1000L
        while (true) {
            val response = httpClient.get(BASE_URL + "2.0/photo/$photoId") {
                parameter("access_token", prefs.kartaViewAccessToken)
            }
            if (response.status == HttpStatusCode.OK) {
                val url = response.body<PhotoLookupResponse>().result?.data?.imageLthUrl
                if (url != null) return url
            }
            if (attempt >= MAX_PHOTO_LOOKUP_ATTEMPTS) {
                throw KartaViewException(
                    "Failed to retrieve photo URL. Please try again later " + response.status
                )
            }
            Log.w(
                TAG,
                "Photo lookup for photo $photoId returned no data yet " +
                    "(attempt $attempt/$MAX_PHOTO_LOOKUP_ATTEMPTS), retrying in ${delayMillis}ms"
            )
            delay(delayMillis)
            delayMillis *= 2
            attempt++
        }
    }

    companion object {
        private const val TAG = "KartaViewApiClient"
        private const val BASE_URL = "https://api.openstreetcam.org/"

        // 1s, 2s, 4s, 8s between the 5 attempts - covers the observed sub-second indexing lag
        // with headroom, without blowing up the upload worker's run time if it takes longer.
        private const val MAX_PHOTO_LOOKUP_ATTEMPTS = 5
    }
}

class KartaViewException(message: String) : RuntimeException(message)
