package com.tcatuw.goinfo.quests.bike_shop

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.quests.AListQuestForm
import com.tcatuw.goinfo.quests.TextItem
import com.tcatuw.goinfo.quests.bike_shop.SecondHandBicycleAvailability.NEW_AND_SECOND_HAND
import com.tcatuw.goinfo.quests.bike_shop.SecondHandBicycleAvailability.NO_BICYCLES_SOLD
import com.tcatuw.goinfo.quests.bike_shop.SecondHandBicycleAvailability.ONLY_NEW
import com.tcatuw.goinfo.quests.bike_shop.SecondHandBicycleAvailability.ONLY_SECOND_HAND

class AddSecondHandBicycleAvailabilityForm : AListQuestForm<SecondHandBicycleAvailability>() {

    override val items = listOf(
        TextItem(ONLY_NEW, R.string.quest_bicycle_shop_second_hand_only_new),
        TextItem(NEW_AND_SECOND_HAND, R.string.quest_bicycle_shop_second_hand_new_and_used),
        TextItem(ONLY_SECOND_HAND, R.string.quest_bicycle_shop_second_hand_only_used),
        TextItem(NO_BICYCLES_SOLD, R.string.quest_bicycle_shop_second_hand_no_bicycles),
    )
}
