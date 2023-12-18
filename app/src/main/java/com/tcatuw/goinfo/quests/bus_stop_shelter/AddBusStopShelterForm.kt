package com.tcatuw.goinfo.quests.bus_stop_shelter

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.quests.AbstractOsmQuestForm
import com.tcatuw.goinfo.quests.AnswerItem
import com.tcatuw.goinfo.quests.bus_stop_shelter.BusStopShelterAnswer.COVERED
import com.tcatuw.goinfo.quests.bus_stop_shelter.BusStopShelterAnswer.NO_SHELTER
import com.tcatuw.goinfo.quests.bus_stop_shelter.BusStopShelterAnswer.SHELTER

class AddBusStopShelterForm : AbstractOsmQuestForm<BusStopShelterAnswer>() {

    override val buttonPanelAnswers = listOf(
        AnswerItem(R.string.quest_generic_hasFeature_no) { applyAnswer(NO_SHELTER) },
        AnswerItem(R.string.quest_generic_hasFeature_yes) { applyAnswer(SHELTER) }
    )

    override val otherAnswers = listOf(
        AnswerItem(R.string.quest_busStopShelter_covered) { applyAnswer(COVERED) }
    )
}
