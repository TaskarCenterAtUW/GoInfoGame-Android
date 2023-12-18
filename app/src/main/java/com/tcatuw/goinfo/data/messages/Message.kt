package com.tcatuw.goinfo.data.messages

import com.tcatuw.goinfo.data.user.achievements.Achievement

sealed class Message

data class OsmUnreadMessagesMessage(val unreadMessages: Int) : Message()
data class NewAchievementMessage(val achievement: Achievement, val level: Int) : Message()
data class NewVersionMessage(val sinceVersion: String) : Message()
object QuestSelectionHintMessage : Message()
