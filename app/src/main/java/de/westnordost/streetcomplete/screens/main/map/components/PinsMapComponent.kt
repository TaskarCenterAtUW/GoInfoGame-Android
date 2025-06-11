package de.westnordost.streetcomplete.screens.main.map.components

import com.mapzen.tangram.MapData
import com.mapzen.tangram.geometry.Point
import de.westnordost.streetcomplete.data.osm.mapdata.LatLon
import de.westnordost.streetcomplete.screens.main.map.tangram.KtMapController
import de.westnordost.streetcomplete.screens.main.map.tangram.toLngLat
import java.util.Collections

/** Takes care of displaying pins on the map, e.g. quest pins or pins for recent edits */
class PinsMapComponent(private val ctrl: KtMapController) {

    private val pinsLayer: MapData = ctrl.addDataLayer(PINS_LAYER)

    private val pins = Collections.synchronizedSet(mutableSetOf<Pin>())

    /** Shows/hides the pins */
    var isVisible: Boolean
        get() = pinsLayer.visible
        set(value) {
            pinsLayer.visible = value
            ctrl.requestRender()
        }

    /** Show given pins. Previously shown pins are replaced with these.  */
    fun set(pins: Collection<Pin>) {
        synchronized(this.pins) {
            this.pins.clear()
            this.pins.addAll(pins)
        }
        pinsLayer.setFeatures(pins.map { it.tangramPoint })
    }

    fun getPins(): Collection<Pin> {
        return synchronized(pins) {
            pins.toList()
        }
    }

    /** Clear pins */
    fun clear() {
        synchronized(pins) {
            pins.clear()
        }
        pinsLayer.clear()
    }

    companion object {
        // see streetcomplete.yaml for the definitions of the below layers
        private const val PINS_LAYER = "streetcomplete_pins"
    }
}

data class Pin(
    val position: LatLon,
    val iconName: String,
    val properties: Collection<Pair<String, String>> = emptyList(),
    val importance: Int = 0,
    var enabled : Boolean = true
) {

    override fun equals(other: Any?): Boolean {
        return this.position == (other as Pin).position
    }

    override fun toString(): String {
        return "Pin(position=$position, iconName='$iconName')"
    }


    val tangramPoint by lazy {
        // avoid creation of intermediate HashMaps.
        val tangramProperties = listOf(
            "type" to "point",
            "kind" to iconName,
            "importance" to importance.toString(),
            "enabled" to enabled.toString()
        )
        val props = HashMap<String, String>(properties.size + tangramProperties.size, 1f)
        props.putAll(tangramProperties)
        props.putAll(properties)
        Point(position.toLngLat(), props)
    }
}
