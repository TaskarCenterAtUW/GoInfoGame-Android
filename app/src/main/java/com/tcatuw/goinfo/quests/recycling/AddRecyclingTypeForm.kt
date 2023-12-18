package com.tcatuw.goinfo.quests.recycling

import com.tcatuw.goinfo.quests.AImageListQuestForm

class AddRecyclingTypeForm : AImageListQuestForm<RecyclingType, RecyclingType>() {

    override val items = RecyclingType.values().map { it.asItem() }
    override val itemsPerRow = 3

    override fun onClickOk(selectedItems: List<RecyclingType>) {
        applyAnswer(selectedItems.single())
    }
}
