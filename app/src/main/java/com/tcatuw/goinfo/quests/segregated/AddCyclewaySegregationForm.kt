package com.tcatuw.goinfo.quests.segregated

import android.os.Bundle
import android.view.View
import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.quests.AImageListQuestForm

class AddCyclewaySegregationForm : AImageListQuestForm<CyclewaySegregation, CyclewaySegregation>() {

    override val items get() =
        CyclewaySegregation.values().map { it.asItem(countryInfo.isLeftHandTraffic) }

    override val itemsPerRow = 2

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        imageSelector.cellLayoutId = R.layout.cell_labeled_icon_select_right
    }

    override fun onClickOk(selectedItems: List<CyclewaySegregation>) {
        applyAnswer(selectedItems.single())
    }
}
