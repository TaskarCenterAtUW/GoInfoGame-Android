package com.tcatuw.goinfo.quests.tactile_paving

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.quests.AbstractOsmQuestForm
import com.tcatuw.goinfo.quests.AnswerItem

class TactilePavingForm : AbstractOsmQuestForm<Boolean>() {

    override val contentLayoutResId = R.layout.quest_tactile_paving

    override val buttonPanelAnswers = listOf(
        AnswerItem(R.string.quest_generic_hasFeature_no) { applyAnswer(false) },
        AnswerItem(R.string.quest_generic_hasFeature_yes) { applyAnswer(true) }
    )
}
