package com.tcatuw.goinfo.quests.recycling_glass

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.quests.AbstractOsmQuestForm
import com.tcatuw.goinfo.quests.AnswerItem
import com.tcatuw.goinfo.quests.recycling_glass.RecyclingGlass.ANY
import com.tcatuw.goinfo.quests.recycling_glass.RecyclingGlass.BOTTLES

class DetermineRecyclingGlassForm : AbstractOsmQuestForm<RecyclingGlass>() {
    override val contentLayoutResId = R.layout.quest_determine_recycling_glass_explanation

    override val buttonPanelAnswers = listOf(
        AnswerItem(R.string.quest_recycling_type_any_glass) { applyAnswer(ANY) },
        AnswerItem(R.string.quest_recycling_type_glass_bottles_short) { applyAnswer(BOTTLES) }
    )
}
