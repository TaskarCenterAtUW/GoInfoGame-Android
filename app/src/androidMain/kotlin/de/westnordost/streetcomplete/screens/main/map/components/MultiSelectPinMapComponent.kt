package de.westnordost.streetcomplete.screens.main.map.components

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import androidx.annotation.DrawableRes
import androidx.annotation.UiThread
import androidx.lifecycle.DefaultLifecycleObserver
import com.google.gson.JsonObject
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.osm.mapdata.LatLon
import de.westnordost.streetcomplete.quests.create_feature.customPinIconName
import de.westnordost.streetcomplete.screens.main.map.createPinBitmap
import de.westnordost.streetcomplete.screens.main.map.maplibre.MapImages
import de.westnordost.streetcomplete.screens.main.map.maplibre.clear
import de.westnordost.streetcomplete.screens.main.map.maplibre.toPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import java.io.File
import org.maplibre.android.style.expressions.Expression.get
import org.maplibre.android.style.layers.Layer
import org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.PropertyFactory.iconOffset
import org.maplibre.android.style.layers.PropertyFactory.iconPadding
import org.maplibre.android.style.layers.PropertyFactory.iconSize
import org.maplibre.android.style.layers.PropertyFactory.symbolSortKey
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection

/** Takes care of displaying "selected" pins. Those pins are always shown on top of pins displayed
 *  by the [PinsMapComponent] */
class MultiSelectPinMapComponent(
    private val context: Context,
    private val map: MapLibreMap,
    private val mapImages: MapImages,
    private val onClickPin: (properties: Map<String, String>) -> Unit,
) : DefaultLifecycleObserver {

    private val selectedPinsSource = GeoJsonSource("multi-selected-pins-source")
    private val customPinIconNames = HashSet<String>()

    val layers: List<Layer> = listOf(
        SymbolLayer("multi-selected-pins-layer", "multi-selected-pins-source")
            .withProperties(
                iconImage(get("icon-image")),
                // constant icon size because click area would become a bit too small and more
                // importantly, dynamic size per zoom + collision doesn't work together well, it
                // results in a lot of flickering.
                iconSize(1f),

                iconPadding(arrayOf(-2.5f, 0f, -7f, 2.5f)),
                iconOffset(arrayOf(-4.5f, -34.5f)),
                iconAllowOverlap(false),
                iconIgnorePlacement(false),
                symbolSortKey(get("icon-order")),
            )
    )

    init {
        selectedPinsSource.isVolatile = true
        map.style?.addSource(selectedPinsSource)
        map.addOnMapClickListener(::onClick)
    }

    /** Show selected pins with the given icon at the given positions. "Selected pins" are not
     *  related to pins, they are just visuals that are displayed on top of the normal pins and look
     *  highlighted/selected. */
    suspend fun set(
        @DrawableRes iconResId: Int,
        pinPositions: Collection<Pair<LatLon, Map<String, String>>>,
    ) {
        val combinedIconId = iconResId * 100000 + R.drawable.checkbox
        val iconName = "pin_with_tick_${combinedIconId}"
        mapImages.addOnce(combinedIconId, iconName) {
            createPinBitmap(
                context,
                iconResId,
                R.drawable.checkbox
            ) to false
        }
        showPins(iconName, pinPositions)
    }

    /** Like [set], but with an already-cached custom icon file (e.g. a workspace's URL-based
     *  quest icon) instead of a drawable resource - combined with the checkbox tick mark the
     *  same way as the resId version. */
    suspend fun set(
        iconFile: File,
        pinPositions: Collection<Pair<LatLon, Map<String, String>>>,
    ) {
        val combinedName = "pin_with_tick_" + customPinIconName(iconFile)
        if (combinedName !in customPinIconNames) {
            val iconBitmap = withContext(Dispatchers.IO) { BitmapFactory.decodeFile(iconFile.path) }
            if (iconBitmap != null) {
                val pinBitmap = createPinBitmap(
                    context,
                    icon = BitmapDrawable(context.resources, iconBitmap),
                    insetIcon = true, // custom icon files are full-bleed, like the preset_* glyphs
                    tickMarkResId = R.drawable.checkbox
                )
                withContext(Dispatchers.Main) { map.style?.addImage(combinedName, pinBitmap) }
                customPinIconNames.add(combinedName)
            }
        }
        showPins(combinedName, pinPositions)
    }

    private suspend fun showPins(
        iconName: String,
        pinPositions: Collection<Pair<LatLon, Map<String, String>>>,
    ) {
        val points = pinPositions.map { (latLon, properties) ->
            val p = JsonObject()
            properties.forEach { (key, value) ->
                p.addProperty(key, value)
                if (key == "icon-image") {
                    p.addProperty("icon-image-temp", value)
                }
            }
            p.addProperty("icon-image", iconName)
            Feature.fromGeometry(latLon.toPoint(), p)
        }
        withContext(Dispatchers.Main) {
            selectedPinsSource.setGeoJson(FeatureCollection.fromFeatures(points))
        }
    }

    private fun onClick(position: LatLng): Boolean {
        val feature = map.queryRenderedFeatures(
            map.projection.toScreenLocation(position),
            *arrayOf("multi-selected-pins-layer")
        ).firstOrNull() ?: return false

        val properties = feature.properties()
        properties?.add("icon-image", properties.get("icon-image-temp"))
        properties?.remove("icon-image-temp")
        onClickPin(properties?.toMap().orEmpty())
        return true
    }

    /** Clear the display of any selected pins */
    @UiThread
    fun clear() {
        selectedPinsSource.clear()
    }
}
