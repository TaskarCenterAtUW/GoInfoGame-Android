package com.tcatuw.goinfo.quests.memorial_type

import com.tcatuw.goinfo.quests.AImageListQuestForm

class AddMemorialTypeForm : AImageListQuestForm<MemorialType, MemorialType>() {

    override val items = MemorialType.values().map { it.asItem() }
    override val itemsPerRow = 3

    override fun onClickOk(selectedItems: List<MemorialType>) {
        applyAnswer(selectedItems.single())
    }
}
