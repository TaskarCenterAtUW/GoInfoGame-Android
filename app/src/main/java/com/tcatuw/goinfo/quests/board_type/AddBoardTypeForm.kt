package com.tcatuw.goinfo.quests.board_type

import androidx.appcompat.app.AlertDialog
import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.quests.AListQuestForm
import com.tcatuw.goinfo.quests.AnswerItem
import com.tcatuw.goinfo.quests.TextItem
import com.tcatuw.goinfo.quests.board_type.BoardType.GEOLOGY
import com.tcatuw.goinfo.quests.board_type.BoardType.HISTORY
import com.tcatuw.goinfo.quests.board_type.BoardType.MAP
import com.tcatuw.goinfo.quests.board_type.BoardType.NATURE
import com.tcatuw.goinfo.quests.board_type.BoardType.NOTICE
import com.tcatuw.goinfo.quests.board_type.BoardType.PLANTS
import com.tcatuw.goinfo.quests.board_type.BoardType.PUBLIC_TRANSPORT
import com.tcatuw.goinfo.quests.board_type.BoardType.SPORT
import com.tcatuw.goinfo.quests.board_type.BoardType.WILDLIFE

class AddBoardTypeForm : AListQuestForm<BoardType>() {

    override val otherAnswers = listOf(
        AnswerItem(R.string.quest_board_type_map) { confirmOnMap() }
    )

    override val items = listOf(
        TextItem(HISTORY, R.string.quest_board_type_history),
        TextItem(GEOLOGY, R.string.quest_board_type_geology),
        TextItem(PLANTS, R.string.quest_board_type_plants),
        TextItem(WILDLIFE, R.string.quest_board_type_wildlife),
        TextItem(NATURE, R.string.quest_board_type_nature),
        TextItem(PUBLIC_TRANSPORT, R.string.quest_board_type_public_transport),
        TextItem(SPORT, R.string.quest_board_type_sport),
        TextItem(NOTICE, R.string.quest_board_type_notice_board),
    )

    private fun confirmOnMap() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.quest_board_type_map_title)
            .setMessage(R.string.quest_board_type_map_description)
            .setPositiveButton(R.string.quest_generic_hasFeature_yes) { _, _ -> applyAnswer(MAP) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
