package com.tcatuw.goinfo.quests.shoulder

import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.quests.AStreetSideSelectForm
import com.tcatuw.goinfo.util.ktx.shoulderLineStyleResId
import com.tcatuw.goinfo.view.controller.StreetSideDisplayItem
import com.tcatuw.goinfo.view.controller.StreetSideItem
import com.tcatuw.goinfo.view.image_select.DisplayItem
import com.tcatuw.goinfo.view.image_select.ImageListPickerDialog
import com.tcatuw.goinfo.view.image_select.Item

class AddShoulderForm : AStreetSideSelectForm<Boolean, ShoulderSides>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<TextView>(R.id.descriptionLabel).setText(R.string.quest_shoulder_explanation2)
    }

    override fun onClickSide(isRight: Boolean) {
        val items = listOf(false, true).map { it.asItem() }
        ImageListPickerDialog(requireContext(), items, R.layout.cell_icon_select_with_label_below, 2) { item ->
            streetSideSelect.replacePuzzleSide(item.value!!.asStreetSideItem(), isRight)
        }.show()
    }

    override fun onClickOk() {
        streetSideSelect.saveLastSelection()
        applyAnswer(ShoulderSides(streetSideSelect.left!!.value, streetSideSelect.right!!.value))
    }

    override fun serialize(item: Boolean) = if (item) "yes" else "no"
    override fun deserialize(str: String) = (str == "yes")
    override fun asStreetSideItem(item: Boolean, isRight: Boolean) = item.asStreetSideItem()

    private fun Boolean.asStreetSideItem(): StreetSideDisplayItem<Boolean> = when (this) {
        true -> StreetSideItem(true, countryInfo.shoulderLineStyleResId, R.string.quest_shoulder_value_yes)
        false -> StreetSideItem(false, R.drawable.ic_shoulder_no, R.string.quest_shoulder_value_no, R.drawable.ic_bare_road_without_feature)
    }

    private fun Boolean.asItem(): DisplayItem<Boolean> = when (this) {
        true -> Item(true, countryInfo.shoulderLineStyleResId, R.string.quest_shoulder_value_yes)
        false -> Item(false, R.drawable.ic_bare_road_without_feature, R.string.quest_shoulder_value_no)
    }
}
