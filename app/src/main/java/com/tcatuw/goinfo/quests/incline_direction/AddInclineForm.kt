package com.tcatuw.goinfo.quests.incline_direction

import android.os.Bundle
import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.osm.geometry.ElementPolylinesGeometry
import com.tcatuw.goinfo.quests.AImageListQuestForm
import com.tcatuw.goinfo.util.math.getOrientationAtCenterLineInDegrees
import kotlin.math.PI

class AddInclineForm : AImageListQuestForm<Incline, Incline>() {
    override val items get() =
        Incline.values().map { it.asItem(requireContext(), wayRotation + mapRotation) }

    override val itemsPerRow = 2

    private var mapRotation: Float = 0f
    private var wayRotation: Float = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wayRotation = (geometry as ElementPolylinesGeometry).getOrientationAtCenterLineInDegrees()
        imageSelector.cellLayoutId = R.layout.cell_icon_select_with_label_below
    }

    override fun onMapOrientation(rotation: Float, tilt: Float) {
        mapRotation = (rotation * 180 / PI).toFloat()
        imageSelector.items = items
    }

    override fun onClickOk(selectedItems: List<Incline>) {
        applyAnswer(selectedItems.first())
    }
}
