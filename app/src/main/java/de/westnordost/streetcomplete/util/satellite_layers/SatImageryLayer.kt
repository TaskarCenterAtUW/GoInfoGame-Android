package de.westnordost.streetcomplete.util.satellite_layers


import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Imagery(
    @SerialName("attribution")
    val attribution: String,
    @SerialName("geographicLocation")
    val geographicLocation: GeographicLocation,
    @SerialName("name")
    val name: String,
    @SerialName("serverType")
    val serverType: String,
    @SerialName("serverUrl")
    val serverUrl: String
)

@Serializable
data class GeographicLocation(
    @SerialName("coordinates")
    val coordinates: List<List<List<Double>>>,
    @SerialName("type")
    val type: String
)

object ImageryRepository {

    private const val JSON_URL = "https://example.com/imagery.json" // Replace with real URL

    private val httpClient by lazy { HttpClient() }

    private val jsonParser = Json { ignoreUnknownKeys = true }

    private var cachedImageryList: List<Imagery>? = null

    private val fetchMutex = Mutex()

    suspend fun getImageryList(): List<Imagery> = withContext(Dispatchers.IO) {
        // Only allow one fetch at a time
        fetchMutex.withLock {
            if (cachedImageryList != null) return@withContext cachedImageryList!!

            val response: HttpResponse = httpClient.get(JSON_URL)
            val jsonText: String = response.bodyAsText()

            val parsedList = jsonParser.decodeFromString<List<Imagery>>(jsonText)
            cachedImageryList = parsedList
            parsedList
        }
    }
}
