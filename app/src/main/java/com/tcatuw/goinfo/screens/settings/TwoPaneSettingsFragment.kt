package com.tcatuw.goinfo.screens.settings

import androidx.fragment.app.Fragment
import androidx.preference.PreferenceFragmentCompat
import com.tcatuw.goinfo.screens.TwoPaneHeaderFragment
import com.tcatuw.goinfo.screens.settings.questselection.QuestPresetsFragment
import com.tcatuw.goinfo.screens.settings.questselection.QuestSelectionFragment

/** Shows the settings lists and details in a two pane layout. */
class TwoPaneSettingsFragment : TwoPaneHeaderFragment() {

    override fun onCreatePreferenceHeader(): PreferenceFragmentCompat {
        return SettingsFragment()
    }

    override fun onCreateInitialDetailFragment(): Fragment {
        val launchQuestSettings = requireActivity().intent.getBooleanExtra(
            SettingsActivity.EXTRA_LAUNCH_QUEST_SETTINGS,
            false
        )
        return if (launchQuestSettings) {
            slidingPaneLayout.open()
            QuestSelectionFragment()
        } else {
            QuestPresetsFragment()
        }
    }
}
