package com.tcatuw.goinfo.quests.camping

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.quests.AListQuestForm
import com.tcatuw.goinfo.quests.AnswerItem
import com.tcatuw.goinfo.quests.TextItem
import com.tcatuw.goinfo.quests.camping.CampType.BACKCOUNTRY
import com.tcatuw.goinfo.quests.camping.CampType.CARAVANS_ONLY
import com.tcatuw.goinfo.quests.camping.CampType.TENTS_AND_CARAVANS
import com.tcatuw.goinfo.quests.camping.CampType.TENTS_ONLY

class AddCampTypeForm : AListQuestForm<CampType>() {

    override val items = listOf(
        TextItem(TENTS_AND_CARAVANS, R.string.quest_camp_type_tents_and_caravans),
        TextItem(TENTS_ONLY, R.string.quest_camp_type_tents_only),
        TextItem(CARAVANS_ONLY, R.string.quest_camp_type_caravans_only),
    )

    override val otherAnswers get() = listOfNotNull(
        AnswerItem(R.string.quest_camp_type_backcountry) { applyAnswer(BACKCOUNTRY) },
    )
}
