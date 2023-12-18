package com.tcatuw.goinfo.quests.leaf_detail

import com.tcatuw.goinfo.quests.AImageListQuestForm

class AddForestLeafTypeForm : AImageListQuestForm<ForestLeafType, ForestLeafType>() {

    override val items = ForestLeafType.values().map { it.asItem() }
    override val itemsPerRow = 3

    override fun onClickOk(selectedItems: List<ForestLeafType>) {
        applyAnswer(selectedItems.single())
    }
}
