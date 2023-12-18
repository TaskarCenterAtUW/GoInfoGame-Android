package com.tcatuw.goinfo.screens.settings.questselection

import com.tcatuw.goinfo.data.osmnotes.notequests.OsmNoteQuestType
import com.tcatuw.goinfo.data.quest.QuestType

data class QuestVisibility(val questType: QuestType, var visible: Boolean) {
    val isInteractionEnabled get() = questType !is OsmNoteQuestType
}
