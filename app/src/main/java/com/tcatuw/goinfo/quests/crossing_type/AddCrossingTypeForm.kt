package com.tcatuw.goinfo.quests.crossing_type

import com.tcatuw.goinfo.quests.AImageListQuestForm

class AddCrossingTypeForm : AImageListQuestForm<CrossingType, CrossingType>() {

    override val items = CrossingType.values().map { it.asItem() }
    override val itemsPerRow = 3

    override fun onClickOk(selectedItems: List<CrossingType>) {
        applyAnswer(selectedItems.single())
    }
}
