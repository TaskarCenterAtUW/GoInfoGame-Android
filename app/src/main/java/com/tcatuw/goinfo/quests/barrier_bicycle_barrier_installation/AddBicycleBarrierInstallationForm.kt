package com.tcatuw.goinfo.quests.barrier_bicycle_barrier_installation

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.quests.AImageListQuestForm
import com.tcatuw.goinfo.quests.AnswerItem

class AddBicycleBarrierInstallationForm :
    AImageListQuestForm<BicycleBarrierInstallation, BicycleBarrierInstallationAnswer>() {

    override val items = BicycleBarrierInstallation.values().map { it.asItem() }
    override val itemsPerRow = 3
    override val moveFavoritesToFront = false

    override fun onClickOk(selectedItems: List<BicycleBarrierInstallation>) {
        applyAnswer(selectedItems.single())
    }

    override val otherAnswers = listOf(
        AnswerItem(R.string.quest_barrier_bicycle_type_not_cycle_barrier) {
            applyAnswer(BarrierTypeIsNotBicycleBarrier)
        },
    )
}
