package com.tcatuw.goinfo.quests.wheelchair_access

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.quests.AbstractOsmQuestForm
import com.tcatuw.goinfo.quests.AnswerItem
import com.tcatuw.goinfo.quests.wheelchair_access.WheelchairAccess.LIMITED
import com.tcatuw.goinfo.quests.wheelchair_access.WheelchairAccess.NO
import com.tcatuw.goinfo.quests.wheelchair_access.WheelchairAccess.YES

open class WheelchairAccessForm : AbstractOsmQuestForm<WheelchairAccess>() {

    override val buttonPanelAnswers = listOf(
        AnswerItem(R.string.quest_generic_hasFeature_no) { applyAnswer(NO) },
        AnswerItem(R.string.quest_generic_hasFeature_yes) { applyAnswer(YES) },
        AnswerItem(R.string.quest_wheelchairAccess_limited) { applyAnswer(LIMITED) },
    )
}
