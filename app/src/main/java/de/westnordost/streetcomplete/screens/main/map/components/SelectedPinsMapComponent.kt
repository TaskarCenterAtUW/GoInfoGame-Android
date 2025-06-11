package de.westnordost.streetcomplete.screens.main.map.components

import android.content.Context
import androidx.annotation.DrawableRes
import com.mapzen.tangram.MapData
import com.mapzen.tangram.geometry.Point
import de.westnordost.streetcomplete.data.osm.mapdata.LatLon
import de.westnordost.streetcomplete.screens.main.map.tangram.KtMapController
import de.westnordost.streetcomplete.screens.main.map.tangram.toLngLat
import java.util.Collections

/** Takes care of displaying "selected" pins. Those pins are always shown on top of pins displayed
 *  by the [PinsMapComponent] */
class SelectedPinsMapComponent(private val ctx: Context, private val ctrl: KtMapController) {

    private val selectedPinsLayer: MapData = ctrl.addDataLayer(SELECTED_PINS_LAYER)
    private val pins = Collections.synchronizedSet(mutableSetOf<LatLon>())

    /** Show selected pins with the given icon at the given positions. "Selected pins" are not
     *  related to pins, they are just visuals that are displayed on top of the normal pins and look
     *  highlighted/selected. */
    fun set(@DrawableRes iconResId: Int, pinPositions: Collection<LatLon>) {

        val points = pinPositions.map { position ->
            Point(position.toLngLat(), mapOf(
                "type" to "point",
                "kind" to ctx.resources.getResourceEntryName(iconResId)
            ))
        }
        synchronized(this.pins) {
            this.pins.addAll(pinPositions)
        }
        selectedPinsLayer.setFeatures(points)
    }

    fun getPins(): Collection<LatLon> {
        return synchronized(pins) {
            pins.toList()
        }
    }

    /** Clear the display of any selected pins */
    fun clear() {
        synchronized(pins) {
            pins.clear()
        }
        selectedPinsLayer.clear()
    }

    companion object {
        // see streetcomplete.yaml for the definitions of the below layers
        private const val SELECTED_PINS_LAYER = "streetcomplete_selected_pins"
    }
}
