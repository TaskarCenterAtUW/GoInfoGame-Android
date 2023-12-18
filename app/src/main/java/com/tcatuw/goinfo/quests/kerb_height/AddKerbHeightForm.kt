package com.tcatuw.goinfo.quests.kerb_height

import com.tcatuw.goinfo.quests.AImageListQuestForm

class AddKerbHeightForm : AImageListQuestForm<KerbHeight, KerbHeight>() {

    override val items = KerbHeight.values().map { it.asItem() }
    override val itemsPerRow = 2
    override val moveFavoritesToFront = false

    override fun onClickOk(selectedItems: List<KerbHeight>) {
        applyAnswer(selectedItems.single())
    }
}
