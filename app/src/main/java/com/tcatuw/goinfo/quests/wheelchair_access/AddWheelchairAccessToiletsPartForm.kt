package com.tcatuw.goinfo.quests.wheelchair_access

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.quests.AbstractOsmQuestForm
import com.tcatuw.goinfo.quests.AnswerItem
import com.tcatuw.goinfo.quests.wheelchair_access.WheelchairAccess.LIMITED
import com.tcatuw.goinfo.quests.wheelchair_access.WheelchairAccess.NO
import com.tcatuw.goinfo.quests.wheelchair_access.WheelchairAccess.YES

class AddWheelchairAccessToiletsPartForm : AbstractOsmQuestForm<WheelchairAccessToiletsPartAnswer>() {
    override val contentLayoutResId = R.layout.quest_wheelchair_toilets_explanation

    override val buttonPanelAnswers = listOf(
        AnswerItem(R.string.quest_generic_hasFeature_no) { applyAnswer(WheelchairAccessToiletsPart(NO)) },
        AnswerItem(R.string.quest_generic_hasFeature_yes) { applyAnswer(WheelchairAccessToiletsPart(YES)) },
        AnswerItem(R.string.quest_wheelchairAccess_limited) { applyAnswer(WheelchairAccessToiletsPart(LIMITED)) },
    )

    override val otherAnswers get() = listOf(
        AnswerItem(R.string.quest_wheelchairAccessPat_noToilet) { applyAnswer(NoToilet) }
    )
}
