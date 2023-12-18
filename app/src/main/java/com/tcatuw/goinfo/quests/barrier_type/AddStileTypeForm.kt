package com.tcatuw.goinfo.quests.barrier_type

import com.tcatuw.goinfo.quests.AImageListQuestForm

class AddStileTypeForm : AImageListQuestForm<StileTypeAnswer, StileTypeAnswer>() {

    override val items =
        StileType.values().map { it.asItem() } +
        ConvertedStile.values().map { it.asItem() }

    override val itemsPerRow = 2

    override fun onClickOk(selectedItems: List<StileTypeAnswer>) {
        applyAnswer(selectedItems.single())
    }
}
