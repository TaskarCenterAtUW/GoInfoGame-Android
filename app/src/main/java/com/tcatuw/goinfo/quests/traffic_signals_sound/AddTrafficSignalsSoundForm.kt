package com.tcatuw.goinfo.quests.traffic_signals_sound

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.quests.AbstractOsmQuestForm
import com.tcatuw.goinfo.quests.AnswerItem

class AddTrafficSignalsSoundForm : AbstractOsmQuestForm<Boolean>() {

    override val contentLayoutResId = R.layout.quest_traffic_lights_sound

    override val buttonPanelAnswers = listOf(
        AnswerItem(R.string.quest_generic_hasFeature_no) { applyAnswer(false) },
        AnswerItem(R.string.quest_generic_hasFeature_yes) { applyAnswer(true) }
    )
}
