package com.tcatuw.goinfo.screens.settings

import android.content.SharedPreferences
import com.tcatuw.goinfo.Prefs
import com.tcatuw.goinfo.Prefs.ResurveyIntervals.DEFAULT
import com.tcatuw.goinfo.Prefs.ResurveyIntervals.LESS_OFTEN
import com.tcatuw.goinfo.Prefs.ResurveyIntervals.MORE_OFTEN
import com.tcatuw.goinfo.Prefs.ResurveyIntervals.valueOf
import com.tcatuw.goinfo.data.elementfilter.filters.RelativeDate

/** This class is just to access the user's preference about which multiplier for the resurvey
 *  intervals to use */
class ResurveyIntervalsUpdater(private val prefs: SharedPreferences) {
    fun update() {
        RelativeDate.MULTIPLIER = multiplier
    }

    private val multiplier: Float get() = when (intervalsPreference) {
        LESS_OFTEN -> 2.0f
        DEFAULT -> 1.0f
        MORE_OFTEN -> 0.5f
    }

    private val intervalsPreference: Prefs.ResurveyIntervals get() =
        valueOf(prefs.getString(Prefs.RESURVEY_INTERVALS, "DEFAULT")!!)
}
