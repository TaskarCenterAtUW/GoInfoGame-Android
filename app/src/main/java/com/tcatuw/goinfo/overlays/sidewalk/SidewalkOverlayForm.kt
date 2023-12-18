package com.tcatuw.goinfo.overlays.sidewalk

import android.os.Bundle
import android.view.View
import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.osm.edits.update_tags.StringMapChangesBuilder
import com.tcatuw.goinfo.data.osm.edits.update_tags.UpdateElementTagsAction
import com.tcatuw.goinfo.osm.sidewalk.LeftAndRightSidewalk
import com.tcatuw.goinfo.osm.sidewalk.Sidewalk
import com.tcatuw.goinfo.osm.sidewalk.Sidewalk.NO
import com.tcatuw.goinfo.osm.sidewalk.Sidewalk.SEPARATE
import com.tcatuw.goinfo.osm.sidewalk.Sidewalk.YES
import com.tcatuw.goinfo.osm.sidewalk.applyTo
import com.tcatuw.goinfo.osm.sidewalk.asItem
import com.tcatuw.goinfo.osm.sidewalk.asStreetSideItem
import com.tcatuw.goinfo.osm.sidewalk.createSidewalkSides
import com.tcatuw.goinfo.osm.sidewalk.validOrNullValues
import com.tcatuw.goinfo.overlays.AStreetSideSelectOverlayForm
import com.tcatuw.goinfo.view.image_select.ImageListPickerDialog

class SidewalkOverlayForm : AStreetSideSelectOverlayForm<Sidewalk>() {

    private var originalSidewalk: LeftAndRightSidewalk? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        originalSidewalk = createSidewalkSides(element!!.tags)?.validOrNullValues()
        if (savedInstanceState == null) {
            initStateFromTags()
        }
    }

    private fun initStateFromTags() {
        streetSideSelect.setPuzzleSide(originalSidewalk?.left?.asStreetSideItem(), false)
        streetSideSelect.setPuzzleSide(originalSidewalk?.right?.asStreetSideItem(), true)
    }

    override fun onClickSide(isRight: Boolean) {
        val items = listOf(YES, NO, SEPARATE).mapNotNull { it.asItem() }
        ImageListPickerDialog(requireContext(), items, R.layout.cell_icon_select_with_label_below, 2) { item ->
            streetSideSelect.replacePuzzleSide(item.value!!.asStreetSideItem()!!, isRight)
        }.show()
    }

    override fun onClickOk() {
        streetSideSelect.saveLastSelection()
        val sidewalks = LeftAndRightSidewalk(streetSideSelect.left?.value, streetSideSelect.right?.value)
        val tagChanges = StringMapChangesBuilder(element!!.tags)
        sidewalks.applyTo(tagChanges)
        applyEdit(UpdateElementTagsAction(element!!, tagChanges.create()))
    }

    override fun hasChanges(): Boolean =
        streetSideSelect.left?.value != originalSidewalk?.left ||
        streetSideSelect.right?.value != originalSidewalk?.right

    override fun serialize(item: Sidewalk) = item.name
    override fun deserialize(str: String) = Sidewalk.valueOf(str)
    override fun asStreetSideItem(item: Sidewalk, isRight: Boolean) = item.asStreetSideItem()!!
}
