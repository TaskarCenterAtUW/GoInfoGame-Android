package com.tcatuw.goinfo.quests.seating

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.quests.AListQuestForm
import com.tcatuw.goinfo.quests.TextItem
import com.tcatuw.goinfo.quests.seating.Seating.INDOOR_AND_OUTDOOR
import com.tcatuw.goinfo.quests.seating.Seating.NO
import com.tcatuw.goinfo.quests.seating.Seating.ONLY_INDOOR
import com.tcatuw.goinfo.quests.seating.Seating.ONLY_OUTDOOR

class AddSeatingForm : AListQuestForm<Seating>() {
    override val items = listOf(
        TextItem(INDOOR_AND_OUTDOOR, R.string.quest_seating_indoor_and_outdoor),
        TextItem(ONLY_INDOOR, R.string.quest_seating_indoor_only),
        TextItem(ONLY_OUTDOOR, R.string.quest_seating_outdoor_only),
        TextItem(NO, R.string.quest_seating_takeaway),
    )
}
