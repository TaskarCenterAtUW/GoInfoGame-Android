package com.tcatuw.goinfo.quests.drinking_water_type

import com.tcatuw.goinfo.quests.AImageListQuestForm

class AddDrinkingWaterTypeForm : AImageListQuestForm<DrinkingWaterType, DrinkingWaterType>() {

    override val items = DrinkingWaterType.values().map { it.asItem() }

    override val itemsPerRow = 3

    override fun onClickOk(selectedItems: List<DrinkingWaterType>) {
        applyAnswer(selectedItems.single())
    }
}
