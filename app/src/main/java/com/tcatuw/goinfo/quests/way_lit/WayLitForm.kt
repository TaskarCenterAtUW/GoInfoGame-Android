package com.tcatuw.goinfo.quests.way_lit

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.osm.lit.LitStatus.AUTOMATIC
import com.tcatuw.goinfo.osm.lit.LitStatus.NIGHT_AND_DAY
import com.tcatuw.goinfo.osm.lit.LitStatus.NO
import com.tcatuw.goinfo.osm.lit.LitStatus.YES
import com.tcatuw.goinfo.quests.AbstractOsmQuestForm
import com.tcatuw.goinfo.quests.AnswerItem
import com.tcatuw.goinfo.util.ktx.couldBeSteps

class WayLitForm : AbstractOsmQuestForm<WayLitOrIsStepsAnswer>() {

    override val buttonPanelAnswers = listOf(
        AnswerItem(R.string.quest_generic_hasFeature_no) { applyAnswer(WayLit(NO)) },
        AnswerItem(R.string.quest_generic_hasFeature_yes) { applyAnswer(WayLit(YES)) }
    )

    override val otherAnswers get() = listOfNotNull(
        AnswerItem(R.string.lit_value_24_7) { applyAnswer(WayLit(NIGHT_AND_DAY)) },
        AnswerItem(R.string.lit_value_automatic) { applyAnswer(WayLit(AUTOMATIC)) },
        createConvertToStepsAnswer(),
    )

    private fun createConvertToStepsAnswer(): AnswerItem? {
        return if (element.couldBeSteps()) {
            AnswerItem(R.string.quest_generic_answer_is_actually_steps) {
                applyAnswer(IsActuallyStepsAnswer)
            }
        } else null
    }
}
