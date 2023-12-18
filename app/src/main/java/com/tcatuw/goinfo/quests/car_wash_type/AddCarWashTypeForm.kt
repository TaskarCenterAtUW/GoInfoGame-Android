package com.tcatuw.goinfo.quests.car_wash_type

import com.tcatuw.goinfo.quests.AImageListQuestForm

class AddCarWashTypeForm : AImageListQuestForm<CarWashType, List<CarWashType>>() {

    override val items = CarWashType.values().map { it.asItem() }
    override val itemsPerRow = 3
    override val maxSelectableItems = -1

    override fun onClickOk(selectedItems: List<CarWashType>) {
        applyAnswer(selectedItems)
    }
}
