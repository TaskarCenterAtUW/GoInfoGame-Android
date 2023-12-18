package com.tcatuw.goinfo.quests.roof_shape

import android.os.Bundle
import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.quests.AImageListQuestForm
import com.tcatuw.goinfo.quests.AnswerItem
import com.tcatuw.goinfo.quests.roof_shape.RoofShape.MANY

class AddRoofShapeForm : AImageListQuestForm<RoofShape, RoofShape>() {

    override val otherAnswers = listOf(
        AnswerItem(R.string.quest_roofShape_answer_many) { applyAnswer(MANY) }
    )

    override val items = RoofShape.values().mapNotNull { it.asItem() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imageSelector.cellLayoutId = R.layout.cell_labeled_icon_select
    }

    override fun onClickOk(selectedItems: List<RoofShape>) {
        applyAnswer(selectedItems.single())
    }
}
