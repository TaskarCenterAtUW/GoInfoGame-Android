package com.tcatuw.goinfo.quests.bollard_type

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.quests.AImageListQuestForm
import com.tcatuw.goinfo.quests.AnswerItem

class AddBollardTypeForm : AImageListQuestForm<BollardType, BollardTypeAnswer>() {

    override val items = BollardType.values().map { it.asItem() }
    override val itemsPerRow = 3

    override fun onClickOk(selectedItems: List<BollardType>) {
        applyAnswer(selectedItems.single())
    }

    override val otherAnswers = listOf(
        AnswerItem(R.string.quest_bollard_type_not_bollard) {
            applyAnswer(BarrierTypeIsNotBollard)
        },
    )
}
