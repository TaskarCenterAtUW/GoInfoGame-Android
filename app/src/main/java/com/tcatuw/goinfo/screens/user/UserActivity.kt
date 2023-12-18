package com.tcatuw.goinfo.screens.user

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.osm.edits.EditType
import com.tcatuw.goinfo.data.user.UserLoginStatusSource
import com.tcatuw.goinfo.data.user.achievements.Achievement
import com.tcatuw.goinfo.screens.FragmentContainerActivity
import com.tcatuw.goinfo.screens.HasTitle
import com.tcatuw.goinfo.screens.user.achievements.AchievementInfoFragment
import com.tcatuw.goinfo.screens.user.achievements.AchievementsFragment
import com.tcatuw.goinfo.screens.user.login.LoginFragment
import com.tcatuw.goinfo.screens.user.statistics.CountryInfoFragment
import com.tcatuw.goinfo.screens.user.statistics.EditStatisticsFragment
import com.tcatuw.goinfo.screens.user.statistics.EditTypeInfoFragment
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/** Shows all the user information, login etc.
 *  This activity coordinates quite a number of fragments, which all call back to this one. In order
 *  of appearance:
 *  The LoginFragment, the UserFragment (which contains the viewpager with more
 *  fragments) and the "fake" dialogs AchievementInfoFragment and QuestTypeInfoFragment.
 * */
class UserActivity :
    FragmentContainerActivity(R.layout.activity_user),
    AchievementsFragment.Listener,
    EditStatisticsFragment.Listener {

    private val userLoginStatusSource: UserLoginStatusSource by inject()

    private val countryDetailsFragment get() =
        supportFragmentManager.findFragmentById(R.id.countryDetailsFragment) as CountryInfoFragment?

    private val editTypeDetailsFragment get() =
        supportFragmentManager.findFragmentById(R.id.editTypeDetailsFragment) as EditTypeInfoFragment?

    private val achievementDetailsFragment get() =
        supportFragmentManager.findFragmentById(R.id.achievementDetailsFragment) as AchievementInfoFragment?

    private val loginStatusListener = object : UserLoginStatusSource.Listener {
        override fun onLoggedIn() { lifecycleScope.launch { replaceMainFragmentAnimated(UserFragment()) } }
        override fun onLoggedOut() { lifecycleScope.launch { replaceMainFragmentAnimated(LoginFragment()) } }
    }

    private val fragmentLifecycleCallbacks = object : FragmentManager.FragmentLifecycleCallbacks() {
        override fun onFragmentStarted(fragmentManager: FragmentManager, fragment: Fragment) {
            if (fragment.id == R.id.fragment_container && fragment is HasTitle) {
                title = fragment.title
                supportActionBar?.subtitle = fragment.subtitle
            }
        }
    }

    /* --------------------------------------- Lifecycle --------------------------------------- */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            replaceMainFragment(when {
                intent.getBooleanExtra(EXTRA_LAUNCH_AUTH, false) -> LoginFragment.create(true)
                userLoginStatusSource.isLoggedIn -> UserFragment()
                else -> LoginFragment.create()
            })
        }
        userLoginStatusSource.addListener(loginStatusListener)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        if (toolbar != null) {
            setSupportActionBar(toolbar)
            supportActionBar!!.setDisplayShowHomeEnabled(true)
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }

        supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentLifecycleCallbacks, false)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            finish()
            return true
        } else super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        userLoginStatusSource.removeListener(loginStatusListener)
    }

    /* ---------------------------- AchievementsFragment.Listener ------------------------------- */

    override fun onClickedAchievement(achievement: Achievement, level: Int, achievementBubbleView: View) {
        achievementDetailsFragment?.show(achievement, level, achievementBubbleView)
    }

    /* --------------------------- QuestStatisticsFragment.Listener ----------------------------- */

    override fun onClickedEditType(editType: EditType, editCount: Int, questBubbleView: View) {
        editTypeDetailsFragment?.show(editType, editCount, questBubbleView)
    }

    override fun onClickedCountryFlag(country: String, editCount: Int, rank: Int?, countryBubbleView: View) {
        countryDetailsFragment?.show(country, editCount, rank, countryBubbleView)
    }

    /* ------------------------------------------------------------------------------------------ */

    private fun replaceMainFragmentAnimated(fragment: Fragment) {
        replaceMainFragment(fragment) {
            setCustomAnimations(
                R.anim.fade_in_from_bottom, R.anim.fade_out_to_top,
                R.anim.fade_in_from_bottom, R.anim.fade_out_to_top
            )
        }
    }

    companion object {
        const val EXTRA_LAUNCH_AUTH = "com.tcatuw.goinfo.screens.user.launch_auth"
    }
}
