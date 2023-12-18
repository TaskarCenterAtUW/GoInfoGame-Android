package com.tcatuw.goinfo.screens.main.messages

import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.messages.Message
import com.tcatuw.goinfo.data.messages.NewAchievementMessage
import com.tcatuw.goinfo.data.messages.NewVersionMessage
import com.tcatuw.goinfo.data.messages.OsmUnreadMessagesMessage
import com.tcatuw.goinfo.data.messages.QuestSelectionHintMessage
import com.tcatuw.goinfo.screens.about.WhatsNewDialog
import com.tcatuw.goinfo.screens.settings.SettingsActivity
import com.tcatuw.goinfo.screens.user.achievements.AchievementInfoFragment

/** A fragment that contains any fragments that would show messages.
 *  Usually, messages are shown as dialogs, however there is currently one exception which
 *  makes this necessary as a fragment */
class MessagesContainerFragment : Fragment(R.layout.fragment_messages_container) {

    fun showMessage(message: Message) {
        val ctx = context ?: return
        when (message) {
            is OsmUnreadMessagesMessage -> {
                OsmUnreadMessagesFragment
                    .create(message.unreadMessages)
                    .show(childFragmentManager, null)
            }
            is NewVersionMessage -> {
                WhatsNewDialog(ctx, message.sinceVersion)
                    .show()
            }
            is NewAchievementMessage -> {
                val f: Fragment = childFragmentManager.findFragmentById(R.id.achievement_info_fragment)!!
                (f as AchievementInfoFragment).showNew(message.achievement, message.level)
            }
            is QuestSelectionHintMessage -> {
                AlertDialog.Builder(ctx)
                    .setTitle(R.string.quest_selection_hint_title)
                    .setMessage(R.string.quest_selection_hint_message)
                    .setPositiveButton(R.string.quest_streetName_cantType_open_settings) { _, _ ->
                        startActivity(SettingsActivity.createLaunchQuestSettingsIntent(ctx))
                    }
                    .setNegativeButton(android.R.string.ok, null)
                    .show()
            }
        }
    }
}
