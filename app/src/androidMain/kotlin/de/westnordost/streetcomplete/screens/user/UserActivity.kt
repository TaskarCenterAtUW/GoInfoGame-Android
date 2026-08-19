package de.westnordost.streetcomplete.screens.user

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.core.app.ActivityCompat
import com.russhwolf.settings.SettingsListener
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

    private val listeners = mutableListOf<SettingsListener>()

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

        // AppTheme only follows isSystemInDarkTheme(), which reflects AppCompatDelegate's night
        // mode - that's only updated (via StreetCompleteApplication's own theme listener) after
        // this preference changes, and Compose has no way to know a Configuration change is coming
        // from that until it's actually delivered. Recreating (same fix SettingsActivity already
        // uses for its own theme picker) forces AppTheme to recompose against the now-current night
        // mode immediately, instead of only looking right after leaving and reopening this screen.
        listeners += preferences.onThemeChanged { ActivityCompat.recreate(this) }
    }

    override fun onDestroy() {
        super.onDestroy()
        listeners.forEach { it.deactivate() }
        listeners.clear()
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
