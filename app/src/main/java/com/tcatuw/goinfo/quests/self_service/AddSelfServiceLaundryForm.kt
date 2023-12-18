package com.tcatuw.goinfo.quests.self_service

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.quests.AbstractOsmQuestForm
import com.tcatuw.goinfo.quests.AnswerItem
import com.tcatuw.goinfo.quests.self_service.SelfServiceLaundry.NO
import com.tcatuw.goinfo.quests.self_service.SelfServiceLaundry.ONLY
import com.tcatuw.goinfo.quests.self_service.SelfServiceLaundry.OPTIONAL

class AddSelfServiceLaundryForm : AbstractOsmQuestForm<SelfServiceLaundry>() {

    override val buttonPanelAnswers = listOf(
        AnswerItem(R.string.quest_generic_hasFeature_no) { applyAnswer(NO) },
        AnswerItem(R.string.quest_generic_hasFeature_optional) { applyAnswer(OPTIONAL) },
        AnswerItem(R.string.quest_hasFeature_only) { applyAnswer(ONLY) }
    )
}
