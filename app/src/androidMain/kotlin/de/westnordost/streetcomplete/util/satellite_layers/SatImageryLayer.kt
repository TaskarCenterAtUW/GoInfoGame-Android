package de.westnordost.streetcomplete.util.satellite_layers


import android.content.Context
import android.os.Parcelable
import de.westnordost.streetcomplete.data.osm.mapdata.LatLon
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Parcelize
@Serializable
data class Imagery(
    @SerialName("attribution")
    val attribution: Attribution,
    @SerialName("description")
    val description: String,
    @SerialName("extent")
    val extent: Extent,
    @SerialName("icon")
    val icon: String,
    @SerialName("id")
    val id: String,
    @SerialName("name")
    val name: String,
    @SerialName("type")
    val type: String,
    @SerialName("url")
    val url: String
) : Parcelable

@Parcelize
@Serializable
data class Attribution(
    @SerialName("required")
    val required: Boolean,
    @SerialName("text")
    val text: String,
    @SerialName("url")
    val url: String
) : Parcelable

@Parcelize
@Serializable
data class Extent(
    @SerialName("max_zoom")
    val maxZoom: Int,
    @SerialName("polygon")
    val polygon: List<List<List<Double>>>
) : Parcelable


class ImageryRepository(private val httpClient: HttpClient, private val context: Context,
                        private val jsonParser: Json) {

    private val mutex = Mutex()
    private var cache: List<Imagery>? = null

    private val url = "http://10.0.2.2:8080/gig-imagery-example.json" // Update this

    fun getImageryForLocation(location: LatLon, imagerList: List<Imagery>) =
        imagerList.filter { imagery ->
            imagery.extent.polygon.any { polygon ->
                isPointInPolygon(location, polygon)
            }
        }

    private fun isPointInPolygon(point: LatLon, polygon: List<List<Double>>): Boolean {
        var inside = false
        var j = polygon.size - 1
        for (i in polygon.indices) {
            val xi = polygon[i][0]
            val yi = polygon[i][1]
            val xj = polygon[j][0]
            val yj = polygon[j][1]
            if (((yi > point.latitude) != (yj > point.latitude)) &&
                (point.longitude < (xj - xi) * (point.latitude - yi) / (yj - yi + 0.0) + xi)
            ) {
                inside = !inside
            }
            j = i
        }
        return inside
    }

    private suspend fun getLocalImageryList(context : Context, jsonParser: Json): List<Imagery> = withContext(Dispatchers.IO) {
        val jsonString = withContext(Dispatchers.IO) {
            context.assets.open("imagery.json").bufferedReader().use { it.readText() }
        }
        jsonParser.decodeFromString(jsonString)
    }

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
