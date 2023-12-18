package com.tcatuw.goinfo.screens.main.map

import androidx.annotation.DrawableRes
import com.tcatuw.goinfo.data.osm.geometry.ElementGeometry

interface ShowsGeometryMarkers {
    fun putMarkerForCurrentHighlighting(
        geometry: ElementGeometry,
        @DrawableRes drawableResId: Int?,
        title: String?
    )
    fun deleteMarkerForCurrentHighlighting(geometry: ElementGeometry)

    fun clearMarkersForCurrentHighlighting()
}
