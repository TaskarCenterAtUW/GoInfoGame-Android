package de.westnordost.streetcomplete.util.logs

import android.util.Log
import io.ktor.client.plugins.logging.Logger as KtorLogger

/** Logger for the Ktor Logging plugin of the app's HTTP clients: writes to logcat only, not
 *  through the app's [de.westnordost.streetcomplete.util.logs.Log], which also stores every
 *  message in the log database (DatabaseLogger). A logged response can be a whole map data
 *  download of several MB - stored in the database, those made it grow by MBs per download (kept
 *  for 14 days, see Cleaner) and every one of them was written into the same database the map
 *  and quests are read from. */
class LogcatHttpLogger(private val tag: String) : KtorLogger {
    override fun log(message: String) {
        Log.d(tag, message)
    }
}
