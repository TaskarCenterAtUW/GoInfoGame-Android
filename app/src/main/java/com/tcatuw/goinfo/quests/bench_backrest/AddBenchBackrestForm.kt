package com.tcatuw.goinfo.quests.bench_backrest

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.quests.AbstractOsmQuestForm
import com.tcatuw.goinfo.quests.AnswerItem
import com.tcatuw.goinfo.quests.bench_backrest.BenchBackrestAnswer.NO
import com.tcatuw.goinfo.quests.bench_backrest.BenchBackrestAnswer.PICNIC_TABLE
import com.tcatuw.goinfo.quests.bench_backrest.BenchBackrestAnswer.YES

class AddBenchBackrestForm : AbstractOsmQuestForm<BenchBackrestAnswer>() {

    override val buttonPanelAnswers = listOf(
        AnswerItem(R.string.quest_generic_hasFeature_no) { applyAnswer(NO) },
        AnswerItem(R.string.quest_generic_hasFeature_yes) { applyAnswer(YES) }
    )

    override val otherAnswers = listOf(
        AnswerItem(R.string.quest_bench_answer_picnic_table) { applyAnswer(PICNIC_TABLE) }
    )
}
