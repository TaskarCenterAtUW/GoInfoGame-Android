package de.westnordost.streetcomplete.screens.main.map.components

import android.animation.ValueAnimator
import android.content.Context
import android.view.animation.OvershootInterpolator
import androidx.annotation.DrawableRes
import androidx.annotation.UiThread
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.gson.JsonObject
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.osm.mapdata.LatLon
import de.westnordost.streetcomplete.screens.main.map.createPinBitmap
import de.westnordost.streetcomplete.screens.main.map.maplibre.MapImages
import de.westnordost.streetcomplete.screens.main.map.maplibre.clear
import de.westnordost.streetcomplete.screens.main.map.maplibre.toPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
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
    private val animation: ValueAnimator

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
        animation = ValueAnimator.ofFloat(0.5f, 1.5f)
        animation.duration = 300
        animation.interpolator = OvershootInterpolator()
        animation.addUpdateListener { animatePin(it.animatedValue as Float) }
        map.addOnMapClickListener(::onClick)
    }

    override fun onPause(owner: LifecycleOwner) {
        animation.pause()
    }

    override fun onResume(owner: LifecycleOwner) {
        animation.resume()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        animation.cancel()
    }

    /** Show selected pins with the given icon at the given positions. "Selected pins" are not
     *  related to pins, they are just visuals that are displayed on top of the normal pins and look
     *  highlighted/selected. */
    suspend fun set(@DrawableRes iconResId: Int, pinPositions: Collection<Pair<LatLon, Map<String, String>>>) {
        val combinedIconId = iconResId * 100000 + R.drawable.checkbox
        val iconName = "pin_with_tick_${combinedIconId}"
        mapImages.addOnce(combinedIconId, iconName) { createPinBitmap(context, iconResId, R.drawable.checkbox) to false }
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
            animation.start()
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

    private fun animatePin(value: Float) {
        map.style?.getLayerAs<SymbolLayer>("multi-selected-pins-layer")?.setProperties(
            iconSize(value),
        )
    }

    /** Clear the display of any selected pins */
    @UiThread fun clear() {
        selectedPinsSource.clear()
    }
}
