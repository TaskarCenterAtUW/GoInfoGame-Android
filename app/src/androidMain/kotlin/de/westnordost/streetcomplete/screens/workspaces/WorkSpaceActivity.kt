package de.westnordost.streetcomplete.screens.workspaces

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import de.westnordost.streetcomplete.ApplicationConstants.APP_NAME
import de.westnordost.streetcomplete.data.preferences.Environment
import de.westnordost.streetcomplete.data.preferences.EnvironmentManager
import de.westnordost.streetcomplete.data.preferences.Preferences
import de.westnordost.streetcomplete.data.user.UserLoginController
import de.westnordost.streetcomplete.ui.theme.AppTheme
import de.westnordost.streetcomplete.util.firebase.FirebaseAnalyticsHelper
import de.westnordost.streetcomplete.util.location.FineLocationManager
import de.westnordost.streetcomplete.util.logs.Log
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import de.westnordost.streetcomplete.screens.main.edithistory.EditHistoryViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.getValue

class WorkSpaceActivity : AppCompatActivity() {

    private val preferences: Preferences by inject()
    private val environmentManager: EnvironmentManager by inject()
    private val userLoginController: UserLoginController by inject()
    private val _isLocationEnabled = mutableStateOf(false)
    private val isLocationEnabled: State<Boolean> get() = _isLocationEnabled
    private val workspaceViewModel by viewModel<WorkspaceViewModel>()

    companion object {
        const val SHOW_LOGGED_OUT_ALERT = "showLogoutAlert"
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
                    if (locationEnabled){
                        AppForceUpdateHandler(viewModel = workspaceViewModel)
                        StartWorkspaceFlow()
                    }
                    else
                        ShowLocationEnableUI()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        workspaceViewModel.getAppUpdateInfo()
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
            val navController = rememberNavController()

            MyAlertDialog(
                showDialog = showDialog,
                onDismiss = {
                    showDialog = false
                    // dismissing "Session Expired" must always land on the login screen, not
                    // rely on preferences.workspaceLogin already being false and startDestination
                    // having defaulted to "home" by coincidence of composition order.
                    navController.navigate("home") {
                        popUpTo(0) { inclusive = true }
                    }
                })
            AppNavigator(innerPadding, preferences, environmentManager, userLoginController, this, navController)
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
}

@Composable
fun AppNavigator(
    innerPadding: PaddingValues,
    preferences: Preferences,
    environmentManager: EnvironmentManager,
    userLoginController: UserLoginController,
    activity: AppCompatActivity,
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    var startDestination = "home"
    var doTokenRefresh = false
    var doLogout = false

    val data: Uri? = activity.intent?.data
    data?.let {
        val code = it.getQueryParameter("code")
        // must be ?.let, not .let - refreshToken?.isNotBlank() is a Boolean? that plain .let
        // would run unconditionally (even when code is null), forcing a logout on any
        // avivscr:// deep link at all, not just an actual OAuth code redirect.
        if (!code.isNullOrBlank()) {
            userLoginController.logOut()
            doLogout = true
        }
    }

    if (preferences.workspaceLogin) {

        preferences.workspaceUserId?.let {
            FirebaseAnalyticsHelper.setUserId(it)
        }
        val now = System.currentTimeMillis()
        if (preferences.refreshTokenExpiryTime != 0L &&
            preferences.refreshTokenExpiryTime < now
        ) {
            Log.w(
                "AuthExpiry",
                "refreshTokenExpiryTime=${preferences.refreshTokenExpiryTime} < now=$now " +
                    "(expired ${now - preferences.refreshTokenExpiryTime}ms ago, " +
                    "workspaceLastLogin=${preferences.workspaceLastLogin}) - forcing logout without attempting refresh"
            )
            userLoginController.logOut()
            doLogout = true
        } else if (preferences.accessTokenExpiryTime != 0L &&
            preferences.accessTokenExpiryTime < now
        ) {
            Log.d(
                "AuthExpiry",
                "accessTokenExpiryTime=${preferences.accessTokenExpiryTime} < now=$now - triggering proactive refresh"
            )
            doTokenRefresh = true
        }
        //Check if we reached 80 percent of auth token expiry time
        else if (preferences.accessTokenExpiryTime != 0L &&
            preferences.accessTokenExpiryTime - now < 0.2 * (preferences.accessTokenExpiryInterval)
        ) {
            Log.d(
                "AuthExpiry",
                "accessTokenExpiryTime=${preferences.accessTokenExpiryTime} is within 20% of expiry " +
                    "(now=$now) - triggering proactive refresh"
            )
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
            val context = LocalContext.current

            LaunchedEffect(Unit) {
                if (doTokenRefresh) viewModel.refreshToken()
            }

            // proactive refresh failed (distinct from the Ktor Auth plugin's own 401-triggered
            // refresh in ApplicationModule.kt, which already force-logs-out on its own failure) -
            // without this, a failed refresh here left the user stranded on this screen with a
            // stale token and no path back to login until some other request happened to 401
            val loginState by viewModel.loginState.collectAsState()
            // while a proactive refresh is in flight, the location-triggered fetch below must
            // wait for it - otherwise it races refreshToken() and calls project-group-roles/
            // getWorkspaces with the still-stale (possibly already-expired) token, hitting a 401
            // even though the refresh may still succeed a moment later
            var tokenRefreshInFlight by remember { mutableStateOf(doTokenRefresh) }
            LaunchedEffect(loginState) {
                when (loginState) {
                    is WorkspaceLoginState.Error -> {
                        Log.e(
                            "AuthExpiry",
                            "Proactive token refresh failed on workspace-list, forcing logout: " +
                                (loginState as WorkspaceLoginState.Error).error
                        )
                        userLoginController.logOut()
                        val intent = Intent(context, WorkSpaceActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            putExtra(WorkSpaceActivity.SHOW_LOGGED_OUT_ALERT, true)
                        }
                        context.startActivity(intent)
                    }
                    is WorkspaceLoginState.NetworkError -> {
                        // couldn't reach the server at all (no connectivity/DNS failure) - the
                        // refresh token itself was never actually rejected, so don't force a
                        // logout; proceed with the still-locally-valid token and let the next
                        // proactive-refresh trigger (or a real 401, handled reactively in
                        // ApplicationModule.kt) retry later
                        Log.w(
                            "AuthExpiry",
                            "Proactive token refresh on workspace-list couldn't reach the server, " +
                                "not forcing logout: " +
                                (loginState as WorkspaceLoginState.NetworkError).error
                        )
                        tokenRefreshInFlight = false
                    }
                    is WorkspaceLoginState.Success -> tokenRefreshInFlight = false
                    else -> {}
                }
            }

            LaunchedEffect(tokenRefreshInFlight) {
                if (tokenRefreshInFlight) return@LaunchedEffect
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
                preferences = preferences,
                modifier = modifier.padding(innerPadding)
            )
        }
    }
}
