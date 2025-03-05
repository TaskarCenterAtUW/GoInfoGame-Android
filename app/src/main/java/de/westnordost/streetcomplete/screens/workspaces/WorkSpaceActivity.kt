package de.westnordost.streetcomplete.screens.workspaces

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import de.westnordost.streetcomplete.data.preferences.Preferences
import de.westnordost.streetcomplete.data.workspace.data.remote.EnvironmentManager
import de.westnordost.streetcomplete.screens.user.UserActivity
import de.westnordost.streetcomplete.ui.theme.AppTheme
import de.westnordost.streetcomplete.util.location.FineLocationManager
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel

class WorkSpaceActivity : ComponentActivity() {

    private val preferences: Preferences by inject()
    private val environmentManager : EnvironmentManager by inject()
    private val _isLocationEnabled = mutableStateOf(false)
    private val isLocationEnabled: State<Boolean> get() = _isLocationEnabled

    private val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission is granted, proceed with location access
            checkLocationPermission()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        isLocationEnabled()

        // Check if location is enabled
        setContent()
    }

    override fun onResume() {
        super.onResume()
        isLocationEnabled()
    }

    private fun setContent() {
        if (isLocationEnabled.value) {
            checkLocationPermission()
        } else {
            setContent {
                AppTheme {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            "GoInfoGame needs location to be enabled to fetch workspaces nearby",
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
                        Button(onClick = { setContent() }) {
                            Text("Refresh")
                        }
                    }
                }
            }
        }
    }

    private fun isLocationEnabled() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        _isLocationEnabled.value =
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission is already granted
                setContent {
                    AppTheme {
                        val workspaceLoginState by preferences.workspaceLoginState.collectAsState()
                        Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
                            TopAppBar(
                                title = { Text(text = "Go Info Game") },
                                actions = {
                                    if (workspaceLoginState) {
                                        Icon(
                                            imageVector = Icons.Default.Person, // Replace with your desired icon
                                            contentDescription = "Star Icon",
                                            modifier = Modifier
                                                .padding(end = 16.dp)
                                                .clickable {
                                                    val intent = Intent(
                                                        this@WorkSpaceActivity,
                                                        UserActivity::class.java
                                                    )
                                                    startActivity(intent)
                                                }
                                        )
                                    }
                                },
                                modifier = Modifier.wrapContentSize(Alignment.Center),
                            )
                        }, contentWindowInsets = WindowInsets.statusBars) { innerPadding ->
                            // LoginScreen(
                            //     viewModel = koinViewModel(),
                            //     modifier = Modifier.padding(innerPadding)
                            // )
                            AppNavigator(innerPadding, preferences, environmentManager)
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
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    var startDestination = "home"
    var doTokenRefresh = false
    var doLogout = false

    if (preferences.workspaceLogin) {

        if (preferences.refreshTokenExpiryTime != 0L &&
            preferences.refreshTokenExpiryTime < System.currentTimeMillis()
        ) {
            preferences.workspaceLogin = false
            doLogout = true
        } else if (preferences.authTokenExpiryTime != 0L &&
            preferences.authTokenExpiryTime < System.currentTimeMillis()
        ) {
            doTokenRefresh = true
        }
        //Check if we reached 80 percent of auth token expiry time
        else if (preferences.authTokenExpiryTime != 0L &&
            preferences.authTokenExpiryTime - System.currentTimeMillis() < 0.2 * (preferences.workspaceTokenExpires)
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
                modifier = modifier.padding(innerPadding)
            )
        }
        composable("workspace-list") {
            val viewModel = koinViewModel<WorkspaceViewModel>()
            if (doTokenRefresh)
                viewModel.refreshToken()
            val fineLocationManager = FineLocationManager(LocalContext.current) { location ->
                // Do something with the location
                viewModel.fetchWorkspaces(location)
            }
            if (ActivityCompat.checkSelfPermission(
                    LocalContext.current,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return@composable
            }
            fineLocationManager.getCurrentLocation()
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
            WorkspaceList(onClick = { }, modifier = Modifier.padding(innerPadding))
        }
    }
}
