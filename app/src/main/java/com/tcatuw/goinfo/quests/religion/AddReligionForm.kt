package com.tcatuw.goinfo.quests.religion

import android.os.Bundle
import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.quests.AImageListQuestForm
import com.tcatuw.goinfo.quests.AnswerItem
import com.tcatuw.goinfo.quests.religion.Religion.MULTIFAITH

class AddReligionForm : AImageListQuestForm<Religion, Religion>() {

    override val otherAnswers = listOf(
        AnswerItem(R.string.quest_religion_for_place_of_worship_answer_multi) { applyAnswer(MULTIFAITH) }
    )

    override val items get() = Religion.values()
        .mapNotNull { it.asItem() }
        .sortedBy { religionPosition(it.value!!.osmValue) }

    fun religionPosition(osmValue: String): Int {
        val position = countryInfo.popularReligions.indexOf(osmValue)
        if (position < 0) {
            // not present at all in config, so should be put at the end
            return Integer.MAX_VALUE
        }
        return position
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imageSelector.cellLayoutId = R.layout.cell_icon_select_with_label_below
    }

    override fun onClickOk(selectedItems: List<Religion>) {
        applyAnswer(selectedItems.single())
    }
}
