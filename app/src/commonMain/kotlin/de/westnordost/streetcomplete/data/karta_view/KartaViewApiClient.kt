package de.westnordost.streetcomplete.data.karta_view

import de.westnordost.streetcomplete.data.karta_view.domain.model.CreateSequenceResponse
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
    /** Uploads the image files at [imagePaths] into one new KartaView sequence at [position],
     *  in list order. Paths that don't exist are skipped. Returns the lth photo URL for each
     *  uploaded image, in the same order.
     *
     *  @throws KartaViewException naming the step that failed */
    suspend fun upload(imagePaths: List<String>, position: LatLon, bearing: Float = 0f): List<String> {
        val images = imagePaths.mapNotNull { path ->
            val file = Path(path)
            if (fileSystem.exists(file)) fileSystem.source(file).buffered().readByteArray() else null
        }
        return uploadImages(images, position, bearing)
    }

    /** Uploads [images] (JPEG-encoded) into one new KartaView sequence at [position], in list
     *  order (sequenceIndex 1..n), closes the sequence and returns the lth photo URL for each
     *  image, in the same order.
     *
     *  @throws KartaViewException naming the step that failed */
    suspend fun uploadImages(images: List<ByteArray>, position: LatLon, bearing: Float = 0f): List<String> {
        if (images.isEmpty()) return emptyList()
        val sequenceId = createSequence()
        images.forEachIndexed { index, image ->
            uploadPhoto(sequenceId, index + 1, image, position, bearing)
        }
        closeSequence(sequenceId)
        return List(images.size) { index -> getPhotoLthUrl(sequenceId, index + 1) }
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
    ) {
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
        }
        Log.d(TAG, "Image $sequenceIndex uploaded to sequence $sequenceId")
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

    private suspend fun getPhotoLthUrl(sequenceId: String, sequenceIndex: Int): String {
        val response = httpClient.get(BASE_URL + "2.0/photo/") {
            parameter("access_token", prefs.kartaViewAccessToken)
            parameter("sequenceId", sequenceId)
            parameter("sequenceIndex", sequenceIndex)
        }
        if (response.status == HttpStatusCode.OK) {
            val url = response.body<PhotoLookupResponse>().result?.data?.firstOrNull()?.imageLthUrl
            if (url != null) return url
        }
        throw KartaViewException(
            "Failed to retrieve photo URL. Please try again later " + response.status
        )
    }

    companion object {
        private const val TAG = "KartaViewApiClient"
        private const val BASE_URL = "https://api.openstreetcam.org/"
    }
}

class KartaViewException(message: String) : RuntimeException(message)
