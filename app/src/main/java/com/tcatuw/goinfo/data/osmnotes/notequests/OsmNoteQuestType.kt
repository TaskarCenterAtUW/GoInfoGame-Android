package com.tcatuw.goinfo.data.osmnotes.notequests

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.quest.QuestType
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement
import com.tcatuw.goinfo.quests.note_discussion.NoteDiscussionForm

object OsmNoteQuestType : QuestType {
    override val icon = R.drawable.ic_quest_notes
    override val title = R.string.quest_noteDiscussion_title
    override val wikiLink = "Notes"
    override val achievements = emptyList<EditTypeAchievement>()

    override fun createForm() = NoteDiscussionForm()
}
