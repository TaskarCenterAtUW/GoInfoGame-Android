package com.tcatuw.goinfo.screens.about

import androidx.preference.PreferenceFragmentCompat
import com.tcatuw.goinfo.screens.TwoPaneHeaderFragment

/** Shows the about screen lists and details in a two pane layout. */
class TwoPaneAboutFragment : TwoPaneHeaderFragment() {

    override fun onCreatePreferenceHeader(): PreferenceFragmentCompat {
        return AboutFragment()
    }
}
