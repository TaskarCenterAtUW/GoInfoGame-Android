package com.tcatuw.goinfo.screens.main.controls

import android.net.ConnectivityManager
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.download.DownloadController
import com.tcatuw.goinfo.data.osm.mapdata.BoundingBox
import com.tcatuw.goinfo.data.visiblequests.TeamModeQuestFilter
import com.tcatuw.goinfo.databinding.FragmentMainMenuButtonBinding
import com.tcatuw.goinfo.util.ktx.popIn
import com.tcatuw.goinfo.util.ktx.popOut
import com.tcatuw.goinfo.util.ktx.toast
import com.tcatuw.goinfo.util.ktx.viewLifecycleScope
import com.tcatuw.goinfo.util.viewBinding
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/** Fragment that shows the main menu button and manages its logic */
class MainMenuButtonFragment : Fragment(R.layout.fragment_main_menu_button) {

    private val teamModeQuestFilter: TeamModeQuestFilter by inject()
    private val downloadController: DownloadController by inject()

    interface Listener {
        fun getDownloadArea(): BoundingBox?
    }

    private val listener: Listener? get() = parentFragment as? Listener ?: activity as? Listener

    private val binding by viewBinding(FragmentMainMenuButtonBinding::bind)

    private val teamModeListener = object : TeamModeQuestFilter.TeamModeChangeListener {
        override fun onTeamModeChanged(enabled: Boolean) { viewLifecycleScope.launch { setTeamMode(enabled) } }
    }

    /* --------------------------------------- Lifecycle ---------------------------------------- */

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.mainMenuButton.setOnClickListener { onClickMainMenu() }
    }

    override fun onStart() {
        super.onStart()

        // in onStart and not onViewCreated because the notification that team mode is active should
        // pop in always when the app comes back from the background again
        if (teamModeQuestFilter.isEnabled) {
            setTeamMode(true)
        }

        teamModeQuestFilter.addListener(teamModeListener)
    }

    override fun onStop() {
        super.onStop()
        teamModeQuestFilter.removeListener(teamModeListener)
    }

    /* ------------------------------------------------------------------------------------------ */

    internal fun onClickMainMenu() {
        MainMenuDialog(
            requireContext(),
            if (teamModeQuestFilter.isEnabled) teamModeQuestFilter.indexInTeam else null,
            this::onClickDownload,
            teamModeQuestFilter::enableTeamMode,
            teamModeQuestFilter::disableTeamMode
        ).show()
    }

    private fun setTeamMode(enabled: Boolean) {
        if (enabled) {
            context?.toast(R.string.team_mode_active)
            binding.teamModeColorCircle.popIn()
            binding.teamModeColorCircle.setIndexInTeam(teamModeQuestFilter.indexInTeam)
        } else {
            context?.toast(R.string.team_mode_deactivated)
            binding.teamModeColorCircle.popOut()
        }
    }

    /* ------------------------------------ Download Button  ------------------------------------ */

    private fun onClickDownload() {
        if (isConnected()) downloadDisplayedArea()
        else context?.toast(R.string.offline)
    }

    private fun isConnected(): Boolean {
        val connectivityManager = context?.getSystemService<ConnectivityManager>()
        val activeNetworkInfo = connectivityManager?.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

    private fun downloadDisplayedArea() {
        val downloadArea = listener?.getDownloadArea() ?: return

        if (downloadController.isPriorityDownloadInProgress) {
            context?.let {
                AlertDialog.Builder(it)
                    .setMessage(R.string.confirmation_cancel_prev_download_title)
                    .setPositiveButton(R.string.confirmation_cancel_prev_download_confirmed) { _, _ ->
                        downloadController.download(downloadArea, true)
                    }
                    .setNegativeButton(R.string.confirmation_cancel_prev_download_cancel, null)
                    .show()
            }
        } else {
            downloadController.download(downloadArea, true)
        }
    }
}
