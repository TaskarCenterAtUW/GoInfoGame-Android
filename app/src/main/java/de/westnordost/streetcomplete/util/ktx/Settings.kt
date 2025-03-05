package de.westnordost.streetcomplete.util.ktx

import com.russhwolf.settings.Settings

fun Settings.putStringOrNull(key: String, value: String?) {
    if (value != null) putString(key, value) else remove(key)
}

fun Settings.putLongOrNull(key: String, value: Long?) {
    if (value != null) putLong(key, value) else remove(key)
}
