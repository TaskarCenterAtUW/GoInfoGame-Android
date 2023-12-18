package com.tcatuw.goinfo.quests.existence

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.quests.AbstractOsmQuestForm
import com.tcatuw.goinfo.quests.AnswerItem

class CheckExistenceForm : AbstractOsmQuestForm<Unit>() {

    override val buttonPanelAnswers = listOf(
        AnswerItem(R.string.quest_generic_hasFeature_no) { deletePoiNode() },
        AnswerItem(R.string.quest_generic_hasFeature_yes) { applyAnswer(Unit) }
    )
}
