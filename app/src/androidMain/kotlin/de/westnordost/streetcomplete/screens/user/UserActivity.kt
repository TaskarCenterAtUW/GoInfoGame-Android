package de.westnordost.streetcomplete.screens.user

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import de.westnordost.streetcomplete.data.preferences.Preferences
import de.westnordost.streetcomplete.screens.BaseActivity
import de.westnordost.streetcomplete.screens.settings.SettingsViewModel
import de.westnordost.streetcomplete.ui.theme.AppTheme
import de.westnordost.streetcomplete.util.creds_manager.BiometricHelper
import de.westnordost.streetcomplete.util.logs.Log
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class UserActivity : BaseActivity() {

    private val viewModel by viewModel<ProfileViewModel>()
    private val settingsViewModel by viewModel<SettingsViewModel>()
    private val preferences: Preferences by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Surface {
                    ProfileScreenNewContent(
                        viewModel,
                        settingsViewModel,
                        preferences,
                        onClickBack = { finish() },
                        onBiometricEnabledChanged = ::onBiometricEnabledChanged
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun onBiometricEnabledChanged(enabled: Boolean): Boolean =
        kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            Log.d("ProfileFragment", "onBiometricEnabledChanged: $enabled")

            val biometricHelper = BiometricHelper(
                context = this,
                activity = this,
                onSuccess = {
                    Toast.makeText(this, "Authenticated!", Toast.LENGTH_SHORT).show()
                    if (cont.isActive) cont.resume(true, null)
                },
                onFailure = {
                    Toast.makeText(this, "Failed to authenticate", Toast.LENGTH_SHORT)
                        .show()
                    if (cont.isActive) cont.resume(false, null)
                }
            )
            biometricHelper.authenticate()
        }
}
