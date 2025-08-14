package de.westnordost.streetcomplete.screens.workspaces

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.appdistribution.InterruptionLevel
import com.google.firebase.appdistribution.ktx.appDistribution
import com.google.firebase.ktx.Firebase
import de.westnordost.streetcomplete.ApplicationConstants.APP_NAME
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.preferences.Preferences
import de.westnordost.streetcomplete.data.workspace.data.remote.EnvironmentManager
import de.westnordost.streetcomplete.ui.theme.AppTheme
import de.westnordost.streetcomplete.util.firebase.FirebaseAnalyticsHelper
import de.westnordost.streetcomplete.util.location.FineLocationManager
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel

class WorkSpaceActivity : AppCompatActivity() {

    private val preferences: Preferences by inject()
    private val environmentManager: EnvironmentManager by inject()
    private val _isLocationEnabled = mutableStateOf(false)
    private val isLocationEnabled: State<Boolean> get() = _isLocationEnabled

    companion object {
        const val SHOW_LOGGED_OUT_ALERT = "showLogoutAlert"
    }

    private val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission is granted, proceed with location access
            val showAlert = intent.getBooleanExtra(SHOW_LOGGED_OUT_ALERT, false)
            checkLocationPermission(showAlert)
        }
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Firebase.appDistribution.showFeedbackNotification(
                // Text providing notice to your testers about collection and
                // processing of their feedback data
                R.string.tutorial_intro,
                // The level of interruption for the notification
                InterruptionLevel.HIGH
            )
        }
        checkNotificationPermission()
    }

    private fun checkNotificationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                // For Android versions below Tiramisu, notifications are granted by default
                Firebase.appDistribution.showFeedbackNotification(
                    // Text providing notice to your testers about collection and
                    // processing of their feedback data
                    R.string.tutorial_intro,
                    // The level of interruption for the notification
                    InterruptionLevel.HIGH
                )
            }
        } else {
            Firebase.appDistribution.showFeedbackNotification(
                // Text providing notice to your testers about collection and
                // processing of their feedback data
                R.string.tutorial_intro,
                // The level of interruption for the notification
                InterruptionLevel.HIGH
            )
            val showAlert = intent.getBooleanExtra(SHOW_LOGGED_OUT_ALERT, false)
            setContent(showAlert)
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        isLocationEnabled()
//        if (BuildConfig.DEBUG) {
//            checkNotificationPermission()
//        } else {
//            val showAlert = intent.getBooleanExtra(SHOW_LOGGED_OUT_ALERT, false)
//            setContent(showAlert)
//        }
        setContent {
            AppTheme {
                var showMainScreen by remember { mutableStateOf(false) }
                var locationEnabled by remember { mutableStateOf(isLocationEnabled()) }

                val lifecycleOwner = LocalLifecycleOwner.current
                // Register the lifecycle observer
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            // Re-check permission when the app resumes
                            locationEnabled = isLocationEnabled()
                        }
                    }

                    lifecycleOwner.lifecycle.addObserver(observer)

                    // Cleanup the observer when the composable leaves the screen
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

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
                    permissionGranted = {
                        showMainScreen = true
                    }
                )
                if (showMainScreen) {
                    if (locationEnabled)
                        StartWorkspaceFlow()
                    else
                        ShowLocationEnableUI()
                }
            }
        }
    }

    @Composable
    fun ShowLocationEnableUI() {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                "$APP_NAME needs location to be enabled to fetch workspaces nearby",
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp),
                style = TextStyle(fontSize = 20.sp)
            )
            if (!isLocationEnabled.value) {
                Button(onClick = {
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(intent)
                }) {
                    Text("Enable location")
                }
            }
        }
    }

    @Composable
    fun StartWorkspaceFlow() {
        val showAlert = intent.getBooleanExtra(SHOW_LOGGED_OUT_ALERT, false)
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.navigationBars),
            contentWindowInsets = WindowInsets.statusBars
        ) { innerPadding ->
            var showDialog by remember { mutableStateOf(showAlert) }

            MyAlertDialog(
                showDialog = showDialog,
                onDismiss = { showDialog = false })
            AppNavigator(innerPadding, preferences, environmentManager, this)
        }
    }

    private fun setContent(showAlert: Boolean) {
        if (isLocationEnabled.value) {
            checkLocationPermission(showAlert)
        } else {
            setContent {
                AppTheme {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            "$APP_NAME needs location to be enabled to fetch workspaces nearby",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp),
                            style = TextStyle(fontSize = 20.sp)
                        )
                        if (!isLocationEnabled.value) {
                            Button(onClick = {
                                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                startActivity(intent)
                            }) {
                                Text("Enable location")
                            }
                        }
                        Button(onClick = { setContent(showAlert) }) {
                            Text("Refresh")
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun MyAlertDialog(showDialog: Boolean, onDismiss: () -> Unit) {
        if (showDialog) {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(text = "Session Expired") },
                text = { Text("Please login again.") },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    private fun checkLocationPermission(showAlert: Boolean) {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission is already granted
                setContent {
                    AppTheme {
                        val workspaceLoginState by preferences.workspaceLoginState.collectAsState()
                        Scaffold(
                            modifier = Modifier
                                .fillMaxSize()
                                .windowInsetsPadding(WindowInsets.navigationBars),
                            contentWindowInsets = WindowInsets.statusBars
                        ) { innerPadding ->
                            // LoginScreen(
                            //     viewModel = koinViewModel(),
                            //     modifier = Modifier.padding(innerPadding)
                            // )
                            var showDialog by remember { mutableStateOf(showAlert) }

                            MyAlertDialog(
                                showDialog = showDialog,
                                onDismiss = { showDialog = false })
                            AppNavigator(innerPadding, preferences, environmentManager, this)
                        }
                    }
                }
            }

            else -> {
                // Request the permission
                requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }
}

@Composable
fun AppNavigator(
    innerPadding: PaddingValues,
    preferences: Preferences,
    environmentManager: EnvironmentManager,
    activity: AppCompatActivity,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    var startDestination = "home"
    var doTokenRefresh = false
    var doLogout = false

    if (preferences.workspaceLogin) {

        preferences.workspaceUserId?.let {
            FirebaseAnalyticsHelper.setUserId(it)
        }
        if (preferences.refreshTokenExpiryTime != 0L &&
            preferences.refreshTokenExpiryTime < System.currentTimeMillis()
        ) {
            preferences.workspaceLogin = false
            doLogout = true
        } else if (preferences.accessTokenExpiryTime != 0L &&
            preferences.accessTokenExpiryTime < System.currentTimeMillis()
        ) {
            doTokenRefresh = true
        }
        //Check if we reached 80 percent of auth token expiry time
        else if (preferences.accessTokenExpiryTime != 0L &&
            preferences.accessTokenExpiryTime - System.currentTimeMillis() < 0.2 * (preferences.accessTokenExpiryInterval)
        ) {
            doTokenRefresh = true
        }


        startDestination = "workspace-list"
        if (doLogout)
            startDestination = "home"
    }
    NavHost(navController = navController, startDestination = startDestination) {
        composable("home") {
            LoginScreen(
                viewModel = koinViewModel(),
                environmentManager,
                preferences,
                navController,
                activity,
                modifier = modifier.padding(innerPadding)
            )
        }
        composable("workspace-list") {
            val viewModel = koinViewModel<WorkspaceViewModel>()
            if (doTokenRefresh)
                viewModel.refreshToken()
            val context = LocalContext.current

            LaunchedEffect(Unit) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    val fineLocationManager = FineLocationManager(context) { location ->
                        if (location.accuracy <= 300) {
                            viewModel.fetchWorkspaces(location)
                        }
                    }
                    fineLocationManager.getCurrentLocation()
                }
            }
            WorkSpaceListScreen(
                viewModel = koinViewModel(),
                modifier = modifier.padding(innerPadding)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AppTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            WorkspaceList(
                onClick = { },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
