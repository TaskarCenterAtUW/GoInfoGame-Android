package de.westnordost.streetcomplete.util.satellite_layers


import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Imagery(
    @SerialName("attribution")
    val attribution: Attribution,
    @SerialName("description")
    val description: String,
    @SerialName("end_date")
    val endDate: String,
    @SerialName("extent")
    val extent: Extent,
    @SerialName("icon")
    val icon: String,
    @SerialName("id")
    val id: String,
    @SerialName("name")
    val name: String,
    @SerialName("start_date")
    val startDate: String,
    @SerialName("type")
    val type: String,
    @SerialName("url")
    val url: String
)

@Serializable
data class Attribution(
    @SerialName("required")
    val required: Boolean,
    @SerialName("text")
    val text: String,
    @SerialName("url")
    val url: String
)

@Serializable
data class LatLon(
    val lat: Double,
    val lon: Double
)

@Serializable
data class Extent(
    @SerialName("max_zoom")
    val maxZoom: Int,
    @SerialName("polygon")
    val polygon: List<List<List<Double>>>
)

@Serializable
data class ImageryResponse(
    @SerialName("imageryLayers")
    val imagery: List<Imagery>
)


class ImageryRepository(private val httpClient: HttpClient) {

    private val mutex = Mutex()
    private var cache: List<Imagery>? = null

    private val url = "http://tmpfiles.org/dl/2082757/gig_aerial_imagery.json" // Update this

    suspend fun getImageryList(): List<Imagery> = withContext(Dispatchers.IO) {
        mutex.withLock {
            cache?.let { return@withContext it }

            val response = httpClient.get(url)
            val responseBody = response.body<List<Imagery>>()
            cache = responseBody
            responseBody
        }
    }
}
