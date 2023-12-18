package com.tcatuw.goinfo.quests.surface

import androidx.appcompat.app.AlertDialog
import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.osm.surface.SELECTABLE_WAY_SURFACES
import com.tcatuw.goinfo.osm.surface.Surface
import com.tcatuw.goinfo.osm.surface.SurfaceAndNote
import com.tcatuw.goinfo.osm.surface.isSurfaceAndTracktypeConflicting
import com.tcatuw.goinfo.osm.surface.toItems
import com.tcatuw.goinfo.quests.AImageListQuestForm

class AddRoadSurfaceForm : AImageListQuestForm<Surface, SurfaceAndNote>() {
    override val items get() = SELECTABLE_WAY_SURFACES.toItems()

    override val itemsPerRow = 3

    override fun onClickOk(selectedItems: List<Surface>) {
        val surface = selectedItems.single()
        confirmPotentialTracktypeMismatch(surface) {
            collectSurfaceDescriptionIfNecessary(requireContext(), surface) { description ->
                applyAnswer(SurfaceAndNote(surface, description))
            }
        }
    }

    private fun confirmPotentialTracktypeMismatch(surface: Surface, onConfirmed: () -> Unit) {
        val tracktype = element.tags["tracktype"]
        if (isSurfaceAndTracktypeConflicting(surface.osmValue!!, tracktype)) {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.quest_generic_confirmation_title)
                .setMessage(R.string.quest_surface_tractypeMismatchInput_confirmation_description)
                .setPositiveButton(R.string.quest_generic_confirmation_yes) { _, _ ->
                    onConfirmed()
                }
                .setNegativeButton(R.string.quest_generic_confirmation_no, null)
                .show()
        } else {
            onConfirmed()
        }
    }
}
