package de.westnordost.streetcomplete.screens.user.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.material3.Surface
import androidx.fragment.app.Fragment
import de.westnordost.streetcomplete.data.preferences.Preferences
import de.westnordost.streetcomplete.screens.settings.SettingsViewModel
import de.westnordost.streetcomplete.ui.util.composableContent
import de.westnordost.streetcomplete.util.creds_manager.BiometricHelper
import de.westnordost.streetcomplete.util.logs.Log
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class ProfileFragment : Fragment() {
    private val viewModel by viewModel<ProfileViewModel>()
    private val settingsViewModel by viewModel<SettingsViewModel>()
    private val preferences: Preferences by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        composableContent {
            Surface {
                ProfileScreen(
                    viewModel,
                    settingsViewModel,
                    preferences,
                    onBiometricEnabledChanged = ::onBiometricEnabledChanged
                )
            }
        }

    fun onBiometricEnabledChanged(enabled: Boolean) {
        Log.d("ProfileFragment", "onBiometricEnabledChanged: $enabled")
        preferences.isBiometricEnabled = enabled

        val biometricHelper = BiometricHelper(
            context = requireContext(),
            activity = requireActivity(),
            onSuccess = {
                Toast.makeText(requireContext(), "Authenticated!", Toast.LENGTH_SHORT).show()
            },
            onFailure = {
                Toast.makeText(requireContext(), "Failed to authenticate", Toast.LENGTH_SHORT)
                    .show()
            }
        )
        if (enabled)
            biometricHelper.authenticate()
    }
}
