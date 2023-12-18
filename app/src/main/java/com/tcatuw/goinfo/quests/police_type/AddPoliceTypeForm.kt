package com.tcatuw.goinfo.quests.police_type

import android.os.Bundle
import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.quests.AImageListQuestForm

class AddPoliceTypeForm : AImageListQuestForm<PoliceType, PoliceType>() {

    override val items = PoliceType.values().map { it.asItem() }
    override val itemsPerRow = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imageSelector.cellLayoutId = R.layout.cell_icon_select_with_label_below
    }

    override fun onClickOk(selectedItems: List<PoliceType>) {
        applyAnswer(selectedItems.single())
    }
}
