package com.tcatuw.goinfo.quests.shop_type

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.quests.AbstractOsmQuestForm
import com.tcatuw.goinfo.quests.AnswerItem

class CheckShopExistenceForm : AbstractOsmQuestForm<Unit>() {
    override val buttonPanelAnswers = listOf(
        AnswerItem(R.string.quest_generic_hasFeature_no) { replaceShop() },
        AnswerItem(R.string.quest_generic_hasFeature_yes) { applyAnswer(Unit) },
    )
}
