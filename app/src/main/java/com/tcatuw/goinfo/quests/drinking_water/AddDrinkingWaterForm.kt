package com.tcatuw.goinfo.quests.drinking_water

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.quests.AListQuestForm
import com.tcatuw.goinfo.quests.TextItem
import com.tcatuw.goinfo.quests.drinking_water.DrinkingWater.NOT_POTABLE_SIGNED
import com.tcatuw.goinfo.quests.drinking_water.DrinkingWater.NOT_POTABLE_UNSIGNED
import com.tcatuw.goinfo.quests.drinking_water.DrinkingWater.POTABLE_SIGNED
import com.tcatuw.goinfo.quests.drinking_water.DrinkingWater.POTABLE_UNSIGNED

class AddDrinkingWaterForm : AListQuestForm<DrinkingWater>() {

    override val items = listOf(
        TextItem(POTABLE_SIGNED, R.string.quest_drinking_water_potable_signed),
        TextItem(POTABLE_UNSIGNED, R.string.quest_drinking_water_potable_unsigned),
        TextItem(NOT_POTABLE_SIGNED, R.string.quest_drinking_water_not_potable_signed),
        TextItem(NOT_POTABLE_UNSIGNED, R.string.quest_drinking_water_not_potable_unsigned),
    )
}
