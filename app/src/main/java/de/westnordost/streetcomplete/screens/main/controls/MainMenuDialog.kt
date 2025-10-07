package de.westnordost.streetcomplete.screens.main.controls

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.core.view.doOnPreDraw
import androidx.core.view.isGone
import de.westnordost.streetcomplete.databinding.DialogMainMenuBinding
import de.westnordost.streetcomplete.screens.MainActivity
import de.westnordost.streetcomplete.screens.about.AboutActivity
import de.westnordost.streetcomplete.screens.main.teammode.TeamModeDialog
import de.westnordost.streetcomplete.screens.settings.SettingsActivity
import de.westnordost.streetcomplete.screens.user.UserActivity
import de.westnordost.streetcomplete.screens.workspaces.WorkSpaceActivity

/** Shows a dialog containing the main menu items */
class MainMenuDialog(
    context: Context,
    indexInTeam: Int?,
    onClickDownload: () -> Unit,
    onEnableTeamMode: (Int, Int) -> Unit,
    onDisableTeamMode: () -> Unit
) : AlertDialog(context) {
    init {
        val binding = DialogMainMenuBinding.inflate(LayoutInflater.from(context))
        binding.closeButton.setOnClickListener { dismiss() }
        binding.workspaceButton.setOnClickListener {
            val activity = context as? Activity
            activity?.let {
                val intent = Intent(it, WorkSpaceActivity::class.java)
                it.startActivity(intent)
                it.finish()
            }
        }
        binding.profileButton.setOnClickListener {
            val intent = Intent(context, UserActivity::class.java)
            context.startActivity(intent)
        }
        binding.enableTeamModeButton.setOnClickListener {
            TeamModeDialog(context, onEnableTeamMode).show()
            dismiss()
        }
        binding.disableTeamModeButton.setOnClickListener {
            onDisableTeamMode()
            dismiss()
        }
        binding.settingsButton.setOnClickListener {
            val intent = Intent(context, SettingsActivity::class.java)
            context.startActivity(intent)
//            context.startActivity(SettingsActivity.createLaunchQuestSettingsIntent(this.context))
//            dismiss()
        }
        binding.aboutButton.setOnClickListener {
            val intent = Intent(context, AboutActivity::class.java)
            context.startActivity(intent)
            dismiss()
        }
        binding.downloadButton.setOnClickListener {
            onClickDownload()
            dismiss()
        }

//        if (indexInTeam != null) {
//            binding.teamModeColorCircle.setIndexInTeam(indexInTeam)
//        }
        // binding.enableTeamModeButton.isGone = indexInTeam != null
        // binding.disableTeamModeButton.isGone = indexInTeam == null

//        binding.root.doOnPreDraw {
//            binding.bigMenuItemsContainer.columnCount = binding.root.width / binding.profileButton.width
//        }

        setView(binding.root)
    }
}
