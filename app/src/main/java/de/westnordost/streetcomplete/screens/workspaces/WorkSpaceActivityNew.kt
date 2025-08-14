package de.westnordost.streetcomplete.screens.workspaces

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import de.westnordost.streetcomplete.ui.theme.AppTheme

class WorkSpaceActivityNew : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Your app's theme
            AppTheme {
                PermissionHandler(
                    permissions = listOf(
                        PermissionModel(
                            permission = "android.permission.POST_NOTIFICATIONS",
                            maxSDKVersion = Int.MAX_VALUE,
                            minSDKVersion = 33,
                            rational = "Access to notifications is required to share feedback"
                        ),
                        PermissionModel(
                            permission = "android.permission.ACCESS_FINE_LOCATION",
                            maxSDKVersion = Int.MAX_VALUE,
                            minSDKVersion = 23,
                            rational = "Access to location is required to use the app"
                        ),
                    ),
                    askPermission = true,
//                    permissionGranted = {
//                        Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
//                    }
                )
            }
        }
    }
}
