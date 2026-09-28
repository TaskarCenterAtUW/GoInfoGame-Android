package de.westnordost.streetcomplete.screens.main.errors

import android.content.Context
import android.content.Intent
import de.westnordost.streetcomplete.screens.workspaces.WorkSpaceActivity

/** The server refused an upload/download as unauthorized (403): the Uploader/Downloader already
 *  logged out - take the user to the login screen with the "Session Expired" alert, the same as
 *  when a 401's token refresh fails (ApplicationModule.kt), instead of leaving them on the map,
 *  logged out, able to keep answering. */
internal fun Context.showSessionExpired() {
    val intent = Intent(this, WorkSpaceActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        putExtra(WorkSpaceActivity.SHOW_LOGGED_OUT_ALERT, true)
    }
    startActivity(intent)
}
