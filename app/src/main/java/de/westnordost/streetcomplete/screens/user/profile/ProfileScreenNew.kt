package de.westnordost.streetcomplete.screens.user.profile

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.preferences.Preferences
import de.westnordost.streetcomplete.screens.settings.SettingsViewModel
import de.westnordost.streetcomplete.ui.common.BackIcon
import de.westnordost.streetcomplete.util.creds_manager.SecureCredentialStorage
import kotlin.reflect.KSuspendFunction1

@RequiresApi(Build.VERSION_CODES.M)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreenNewContent(
    viewModel: ProfileViewModel,
    settingsViewModel: SettingsViewModel,
    preferences: Preferences,
    onClickBack: () -> Unit,
    onBiometricEnabledChanged: KSuspendFunction1<Boolean, Boolean>
) {
    var isChecked by remember { mutableStateOf(preferences.isBiometricEnabled) }
    val userName by viewModel.userName.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE5E5E5))
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

            Image(
                painter = painterResource(R.drawable.ic_profile_48dp),
                contentDescription = null,
                colorFilter = ColorFilter.tint(Color.White),
                modifier = Modifier
                    .size(75.dp)
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Text(
                text = userName.orEmpty(),
                color = Color.DarkGray,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
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
                .background(Color.White)
        ) {
            Text(
                "Preferences".uppercase(),
                color = Color.Gray,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                var pendingValue by remember { mutableStateOf<Boolean?>(null) }
                val context = LocalContext.current
                Column(modifier = Modifier.weight(3f)) {
                    Text(
                        text = stringResource(R.string.diable_biometric_title),
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.disable_biometric_message),
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = isChecked,
                    onCheckedChange = { newValue ->
                        pendingValue = newValue // trigger LaunchedEffect
                    },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = MaterialTheme.colorScheme.secondary
                    ),
                    modifier = Modifier.weight(1f)
                )

                LaunchedEffect(pendingValue) {
                    pendingValue?.let { newValue ->
                        val success = onBiometricEnabledChanged(newValue)
                        if (success) {
                            preferences.isBiometricEnabled = newValue
                            isChecked = newValue
                            if (!newValue) {
                                SecureCredentialStorage.deleteCredential(
                                    context,
                                    preferences.environment
                                )
                            }
                        } else {
                            // Don't update preference; revert UI
                            isChecked = !newValue
                        }
                        pendingValue = null // reset
                    }
                }
            }

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
                        containerColor = MaterialTheme.colorScheme.secondary,      // Background
                        contentColor = MaterialTheme.colorScheme.onSecondary,      // Text/Icon color
                    ),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_outline_logout_24),
                        contentDescription = "Email",
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
fun DottedDivider(
    color: Color = Color.Gray,
    strokeWidth: Float = 2f,
    dotInterval: Float = 10f, // space between dots
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
    ) {
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(0f, 0f),
            end = androidx.compose.ui.geometry.Offset(size.width, 0f),
            strokeWidth = strokeWidth,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dotInterval, dotInterval), 0f)
        )
    }
}

