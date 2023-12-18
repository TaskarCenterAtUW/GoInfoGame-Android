package com.tcatuw.goinfo.quests.building_entrance

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.quests.AListQuestForm
import com.tcatuw.goinfo.quests.TextItem
import com.tcatuw.goinfo.quests.building_entrance.EntranceExistsAnswer.EMERGENCY_EXIT
import com.tcatuw.goinfo.quests.building_entrance.EntranceExistsAnswer.EXIT
import com.tcatuw.goinfo.quests.building_entrance.EntranceExistsAnswer.GENERIC
import com.tcatuw.goinfo.quests.building_entrance.EntranceExistsAnswer.MAIN
import com.tcatuw.goinfo.quests.building_entrance.EntranceExistsAnswer.SERVICE
import com.tcatuw.goinfo.quests.building_entrance.EntranceExistsAnswer.SHOP
import com.tcatuw.goinfo.quests.building_entrance.EntranceExistsAnswer.STAIRCASE

class AddEntranceForm : AListQuestForm<EntranceAnswer>() {
    override val items: List<TextItem<EntranceAnswer>> = listOf(
        TextItem(MAIN, R.string.quest_building_entrance_main),
        TextItem(STAIRCASE, R.string.quest_building_entrance_staircase),
        TextItem(SERVICE, R.string.quest_building_entrance_service),
        TextItem(EXIT, R.string.quest_building_entrance_exit),
        TextItem(EMERGENCY_EXIT, R.string.quest_building_entrance_emergency_exit),
        TextItem(SHOP, R.string.quest_building_entrance_shop),
        TextItem(GENERIC, R.string.quest_building_entrance_yes),
        TextItem(DeadEnd, R.string.quest_building_entrance_dead_end),
    )
}
