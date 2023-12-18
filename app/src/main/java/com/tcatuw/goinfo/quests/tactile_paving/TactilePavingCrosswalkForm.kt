package com.tcatuw.goinfo.quests.tactile_paving

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.quests.AbstractOsmQuestForm
import com.tcatuw.goinfo.quests.AnswerItem
import com.tcatuw.goinfo.quests.tactile_paving.TactilePavingCrosswalkAnswer.INCORRECT
import com.tcatuw.goinfo.quests.tactile_paving.TactilePavingCrosswalkAnswer.NO
import com.tcatuw.goinfo.quests.tactile_paving.TactilePavingCrosswalkAnswer.YES

class TactilePavingCrosswalkForm : AbstractOsmQuestForm<TactilePavingCrosswalkAnswer>() {

    override val contentLayoutResId = R.layout.quest_tactile_paving

    override val otherAnswers get() = listOf(
        AnswerItem(R.string.quest_tactilePaving_incorrect) { applyAnswer(INCORRECT) }
    )

    override val buttonPanelAnswers = listOf(
        AnswerItem(R.string.quest_generic_hasFeature_no) { applyAnswer(NO) },
        AnswerItem(R.string.quest_generic_hasFeature_yes) { applyAnswer(YES) }
    )
}
