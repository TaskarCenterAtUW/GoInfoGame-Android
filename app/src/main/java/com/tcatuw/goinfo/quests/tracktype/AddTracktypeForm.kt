package com.tcatuw.goinfo.quests.tracktype

import com.tcatuw.goinfo.quests.AImageListQuestForm

class AddTracktypeForm : AImageListQuestForm<Tracktype, Tracktype>() {

    override val items = Tracktype.values().map { it.asItem() }

    override val itemsPerRow = 3

    override val moveFavoritesToFront = false

    override fun onClickOk(selectedItems: List<Tracktype>) {
        applyAnswer(selectedItems.single())
    }
}
