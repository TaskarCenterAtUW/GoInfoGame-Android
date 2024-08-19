package de.westnordost.streetcomplete.view.dialogs

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.screens.user.profile.ProfileViewModel
import de.westnordost.streetcomplete.screens.workspaces.WorkSpaceActivity

/** Shows a dialog that asks the user to login */
@SuppressLint("InflateParams")
class RequestLoginDialog(context: Context, profileViewModel: ProfileViewModel) : AlertDialog(context) {
    init {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_authorize_now, null, false)

        setView(view)
        setButton(BUTTON_POSITIVE, context.getString(R.string.user_logout)) { _, _ ->
            // val intent = Intent(context, UserActivity::class.java)
            // intent.putExtra(UserActivity.EXTRA_LAUNCH_AUTH, true)
            // context.startActivity(intent)
            profileViewModel.logOutUser()
            finishAndLaunchNewActivity(context)
        }
        setCancelable(false)
    }

    private fun finishAndLaunchNewActivity(
        context: Context,
    ) {
        val activity = context as? Activity
        activity?.let {
            it.finishAffinity()
            val intent = Intent(it, WorkSpaceActivity::class.java)
            it.startActivity(intent)
        }
    }
}
