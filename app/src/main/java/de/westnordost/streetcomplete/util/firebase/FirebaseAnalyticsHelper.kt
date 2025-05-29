package de.westnordost.streetcomplete.util.firebase

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

object FirebaseAnalyticsHelper {

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    fun init(context: Context) {
        firebaseAnalytics = FirebaseAnalytics.getInstance(context)
    }

    fun setUserId(userId: String) {
        if (::firebaseAnalytics.isInitialized) {
            firebaseAnalytics.setUserId(userId)
        }
    }

    fun setUserProperty(key: String, value: String) {
        if (::firebaseAnalytics.isInitialized) {
            firebaseAnalytics.setUserProperty(key, value)
        }
    }

    fun logQuestAnswered(questType: String) {
        if (::firebaseAnalytics.isInitialized) {
            val bundle = Bundle().apply {
                putString("quest_type", questType)
            }
            firebaseAnalytics.logEvent("quest_answered", bundle)
        }
    }

    fun logEvent(eventName: String, params: Bundle) {
        if (::firebaseAnalytics.isInitialized) {
            firebaseAnalytics.logEvent(eventName, params)
        }
    }

    fun logSimpleEvent(eventName: String) {
        if (::firebaseAnalytics.isInitialized) {
            firebaseAnalytics.logEvent(eventName, null)
        }
    }
}


