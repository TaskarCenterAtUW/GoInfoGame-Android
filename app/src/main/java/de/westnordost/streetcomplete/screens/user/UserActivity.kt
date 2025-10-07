package de.westnordost.streetcomplete.screens.user

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.material3.Surface
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.preferences.Preferences
import de.westnordost.streetcomplete.databinding.ActivityUserBinding
import de.westnordost.streetcomplete.screens.BaseActivity
import de.westnordost.streetcomplete.screens.settings.SettingsViewModel
import de.westnordost.streetcomplete.screens.user.profile.ProfileScreenNewContent
import de.westnordost.streetcomplete.screens.user.profile.ProfileViewModel
import de.westnordost.streetcomplete.ui.theme.AppTheme
import de.westnordost.streetcomplete.util.creds_manager.BiometricHelper
import de.westnordost.streetcomplete.util.logs.Log
import de.westnordost.streetcomplete.util.viewBinding
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

/** Shows all the user information, login etc.
 *  This activity coordinates quite a number of fragments, which all call back to this one. In order
 *  of appearance:
 *  The LoginFragment, the UserFragment (which contains the viewpager with more
 *  fragments) and the "fake" dialog QuestTypeInfoFragment.
 */
class UserActivity : BaseActivity() {

    private val viewModel by viewModel<ProfileViewModel>()
    private val settingsViewModel by viewModel<SettingsViewModel>()
    private val preferences: Preferences by inject()

    /* --------------------------------------- Lifecycle --------------------------------------- */
    private val binding by viewBinding(ActivityUserBinding::inflate)

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user)

        binding.navHost.setContent {
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
