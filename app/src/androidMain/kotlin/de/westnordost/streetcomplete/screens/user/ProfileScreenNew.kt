package de.westnordost.streetcomplete.screens.user

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.preferences.Preferences
import de.westnordost.streetcomplete.screens.settings.SettingsViewModel
import de.westnordost.streetcomplete.screens.workspaces.WorkSpaceActivity
import de.westnordost.streetcomplete.ui.common.BackIcon
import de.westnordost.streetcomplete.util.creds_manager.SecureCredentialStorage
import kotlin.reflect.KSuspendFunction1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreenNewContent(
    viewModel: ProfileViewModel,
    settingsViewModel: SettingsViewModel,
    preferences: Preferences,
    onClickBack: () -> Unit,
    onBiometricEnabledChanged: KSuspendFunction1<Boolean, Boolean>,
) {
    var isBiometricEnabled by remember { mutableStateOf(preferences.isBiometricEnabled) }
    var lowBandwidthModeEnabled by remember { mutableStateOf(preferences.isLowBandwidthModeEnabled) }
    var isFollowModeEnabled by remember { mutableStateOf(preferences.isFollowModeEnabled) }
    val userName by viewModel.userName.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .background(colorResource(R.color.light_purple_background))
                .padding(bottom = 32.dp)
        ) {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.user_profile),
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorResource(R.color.light_purple_background)
                ),
                navigationIcon = { IconButton(onClick = onClickBack) { BackIcon() } },
            )

            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(75.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Text(
                text = userName.orEmpty(),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics{
                    val info = userName?.split("\n") ?: emptyList()
                    val email = info.getOrNull(0) ?: "Unknown"
                    val name = info.getOrNull(1) ?: "Unknown"

                    contentDescription = "Email: $email, User name: $name"
                }
            )
        }

        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .padding(bottom = 16.dp)
                .fillMaxSize()
                .padding(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            val localContext = LocalContext.current
            Text(
                "Preferences".uppercase(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
            var biometricLogin by remember { mutableStateOf<Boolean?>(null) }
            var followMode by remember { mutableStateOf<Boolean?>(null) }
            var lowBandwidth by remember { mutableStateOf<Boolean?>(null) }

            LaunchedEffect(biometricLogin) {
                biometricLogin?.let { newValue ->
                    val success = onBiometricEnabledChanged(newValue)
                    if (success) {
                        preferences.isBiometricEnabled = newValue
                        isBiometricEnabled = newValue
                        if (!newValue) {
                            SecureCredentialStorage.deleteCredential(
                                localContext,
                                preferences.environment
                            )
                        }
                    } else {
                        // Don't update preference; revert UI
                        isBiometricEnabled = !newValue
                    }
                    biometricLogin = null // reset
                }
            }

            LaunchedEffect(followMode) {
                followMode?.let { newValue ->
                    preferences.isFollowModeEnabled = newValue
                    followMode = null // reset
                    isFollowModeEnabled = newValue
                }
            }

            LaunchedEffect(lowBandwidth) {
                lowBandwidth?.let { newValue ->
                    preferences.isLowBandwidthModeEnabled = newValue
                    lowBandwidthModeEnabled = newValue
                    lowBandwidth = null
                }
            }

            PreferenceRow(
                stringResource(R.string.diable_biometric_title),
                stringResource(R.string.disable_biometric_message),
                isBiometricEnabled,
                onCheckedChange = { newValue ->
                    biometricLogin = newValue // trigger LaunchedEffect
                })

            PreferenceRow(
                stringResource(R.string.low_band_width),
                stringResource(R.string.low_band_width_message),
                lowBandwidthModeEnabled,
                onCheckedChange = { newValue ->
                    lowBandwidth = newValue // trigger LaunchedEffect
                })

            // PreferenceRow(
            //     stringResource(R.string.follow_mode),
            //     stringResource(R.string.enable_follow_mode),
            //     isFollowModeEnabled,
            //     onCheckedChange = { newValue ->
            //         followMode = newValue // trigger LaunchedEffect
            //     })

            DottedDivider(
                color = Color.Gray,
                modifier = Modifier.padding(16.dp)
            )

            val context = LocalContext.current

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        viewModel.logOutUser()
                        settingsViewModel.deleteCache()
                        finishAndLaunchNewActivity(context)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,      // Background
                        contentColor = MaterialTheme.colorScheme.onPrimary,      // Text/Icon color
                    ),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_outline_logout_24),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.user_logout).uppercase(),
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun PreferenceRow(
    text: String,
    subText: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.weight(3f)) {
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subText,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun DottedDivider(
    modifier: Modifier = Modifier,
    color: Color = Color.Gray,
    strokeWidth: Float = 2f,
    dotInterval: Float = 10f, // space between dots
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
    ) {
        drawLine(
            color = color,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            strokeWidth = strokeWidth,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dotInterval, dotInterval), 0f)
        )
    }
}

fun finishAndLaunchNewActivity(
    context: Context,
) {
    val activity = context as? Activity
    activity?.let {
        it.finishAffinity()
        val intent = Intent(it, WorkSpaceActivity::class.java)
        it.startActivity(intent)
    }
}

