package com.tcatuw.goinfo.quests.surface

import com.tcatuw.goinfo.osm.surface.SELECTABLE_WAY_SURFACES
import com.tcatuw.goinfo.osm.surface.Surface
import com.tcatuw.goinfo.osm.surface.SurfaceAndNote
import com.tcatuw.goinfo.osm.surface.toItems
import com.tcatuw.goinfo.quests.AImageListQuestForm

class AddPathPartSurfaceForm : AImageListQuestForm<Surface, SurfaceAndNote>() {
    override val items get() = SELECTABLE_WAY_SURFACES.toItems()

    override val itemsPerRow = 3

    override fun onClickOk(selectedItems: List<Surface>) {
        val value = selectedItems.single()

        collectSurfaceDescriptionIfNecessary(requireContext(), value) {
            applyAnswer(SurfaceAndNote(value, it))
        }
    }
}
