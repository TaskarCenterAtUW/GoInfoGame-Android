package com.tcatuw.goinfo.screens.about

import android.os.Bundle
import com.tcatuw.goinfo.screens.FragmentContainerActivity

class AboutActivity : FragmentContainerActivity() {

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        if (savedInstanceState == null) {
            replaceMainFragment(TwoPaneAboutFragment())
        }
    }
}
