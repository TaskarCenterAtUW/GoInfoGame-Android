package de.westnordost.streetcomplete.data.osm.mapdata

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.sqrt

@Serializable
sealed class Element {
    abstract val id: Long
    abstract val version: Int
    abstract val tags: Map<String, String>
    abstract val timestampEdited: Long
    abstract val type: ElementType
}

@Serializable
@SerialName("node")
data class Node(
    override val id: Long,
    val position: LatLon,
    override val tags: Map<String, String> = emptyMap(),
    override val version: Int = 1,
    override val timestampEdited: Long = 0
) : Element() {
    @SerialName("elementType")
    override val type get() = ElementType.NODE
}

@Serializable
@SerialName("way")
data class Way(
    override val id: Long,
    val nodeIds: List<Long>,
    override val tags: Map<String, String> = emptyMap(),
    override val version: Int = 1,
    override val timestampEdited: Long = 0
) : Element() {
    @SerialName("elementType")
    override val type = ElementType.WAY

    val isClosed get() = nodeIds.size >= 3 && nodeIds.first() == nodeIds.last()
}

@Serializable
@SerialName("relation")
data class Relation(
    override val id: Long,
    val members: List<RelationMember>,
    override val tags: Map<String, String> = emptyMap(),
    override val version: Int = 1,
    override val timestampEdited: Long = 0
) : Element() {
    @SerialName("elementType")
    override val type = ElementType.RELATION
}

@Serializable
data class RelationMember(
    @SerialName("elementType")
    val type: ElementType,
    val ref: Long,
    val role: String
)

enum class ElementType { NODE, WAY, RELATION }

@Serializable
data class LatLon(
    @SerialName("lat")
    val latitude: Double,
    @SerialName("lon")
    val longitude: Double
) {
    init {
        checkValidity(latitude, longitude)
    }

    companion object {
        fun checkValidity(latitude: Double, longitude: Double) {
            require(
                latitude >= -90.0 && latitude <= +90
                    && longitude >= -180 && longitude <= +180
            ) { "Latitude $latitude, longitude $longitude is not a valid position" }
        }

        fun metersToLatitudeDegrees(radiusMeters: Double): Double {
            return (radiusMeters / 6378137.0) * (180.0 / PI)
        }

        fun metersToLongitudeDegrees(radiusMeters: Double, atLatitude: Double): Double {
            require(atLatitude.absoluteValue < 90.0) { "Latitude must be less than 90° in absolute value" }
            return (radiusMeters / (6378137.0 * cos(atLatitude * PI / 180.0))) * (180.0 / PI)
        }

        fun distanceInMeters(a: LatLon, b: LatLon): Double {
            val latDistance = metersToLatitudeDegrees(a.latitude - b.latitude)
            val lonDistance = metersToLongitudeDegrees(a.longitude - b.longitude, (a.latitude + b.latitude) / 2)
            return sqrt(latDistance * latDistance + lonDistance * lonDistance)
        }
    }
}
