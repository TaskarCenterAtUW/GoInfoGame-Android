package com.tcatuw.goinfo.data.osm.edits.upload

import android.content.SharedPreferences
import androidx.core.content.edit
import com.tcatuw.goinfo.Prefs
import com.tcatuw.goinfo.util.ktx.nowAsEpochMilliseconds

class LastEditTimeStore(private val prefs: SharedPreferences) {

    fun touch() {
        prefs.edit { putLong(Prefs.LAST_EDIT_TIME, nowAsEpochMilliseconds()) }
    }

    fun get(): Long =
        prefs.getLong(Prefs.LAST_EDIT_TIME, 0)
}
