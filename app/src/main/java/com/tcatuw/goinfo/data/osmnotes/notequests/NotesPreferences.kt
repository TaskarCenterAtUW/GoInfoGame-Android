package com.tcatuw.goinfo.data.osmnotes.notequests

import android.content.SharedPreferences
import com.tcatuw.goinfo.Prefs

class NotesPreferences(private val prefs: SharedPreferences) {

    interface Listener {
        fun onNotesPreferencesChanged()
    }

    var listener: Listener? = null

    private val sharedPreferencesListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == Prefs.SHOW_NOTES_NOT_PHRASED_AS_QUESTIONS) {
            listener?.onNotesPreferencesChanged()
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(sharedPreferencesListener)
    }

    val showOnlyNotesPhrasedAsQuestions: Boolean get() =
        !prefs.getBoolean(Prefs.SHOW_NOTES_NOT_PHRASED_AS_QUESTIONS, false)
}
