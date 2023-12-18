package com.tcatuw.goinfo.quests.bbq_fuel

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.quests.AListQuestForm
import com.tcatuw.goinfo.quests.TextItem

class AddBbqFuelForm : AListQuestForm<BbqFuel>() {

    override val items = listOf(
        TextItem(BbqFuel.WOOD, R.string.quest_bbq_fuel_wood),
        TextItem(BbqFuel.ELECTRIC, R.string.quest_bbq_fuel_electric),
        TextItem(BbqFuel.CHARCOAL, R.string.quest_bbq_fuel_charcoal),
    )
}
