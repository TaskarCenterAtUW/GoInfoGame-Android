package com.tcatuw.goinfo.util

import android.content.res.Configuration
import androidx.core.os.ConfigurationCompat
import com.tcatuw.goinfo.util.ktx.toList
import java.util.Locale

fun getLocalesForFeatureDictionary(configuration: Configuration): Array<Locale?> {
    val result = ArrayList<Locale?>()
    result.addAll(ConfigurationCompat.getLocales(configuration).toList())
    /* add fallback to English if (some) English is not part of the locale list already as the
       fallback for text is also always English in this app (strings.xml) independent of, or rather
       additionally to what is in the user's LocaleList. */
    if (result.none { it?.language == Locale.ENGLISH.language }) {
        result.add(Locale.ENGLISH)
    }
    // add null to allow unlocalized features
    result.add(null)
    return result.toTypedArray()
}
