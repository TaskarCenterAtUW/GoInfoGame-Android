package de.westnordost.streetcomplete.screens.workspaces

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import de.westnordost.streetcomplete.data.preferences.Preferences
import de.westnordost.streetcomplete.screens.MainActivity
import de.westnordost.streetcomplete.ui.theme.AppTheme
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel

class WorkSpaceActivity : ComponentActivity() {

    private val preferences : Preferences by inject()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
                    TopAppBar(
                        title = { Text(text = "Go Info Game") },
                        modifier = Modifier.wrapContentSize(Alignment.Center),
                    )
                }, contentWindowInsets = WindowInsets.statusBars) { innerPadding ->
                    // LoginScreen(
                    //     viewModel = koinViewModel(),
                    //     modifier = Modifier.padding(innerPadding)
                    // )
                    AppNavigator(innerPadding, preferences)
                }
            }
        }
    }
}

@Composable
fun AppNavigator(innerPadding : PaddingValues, preferences: Preferences, modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    var startDestination = "home"
    if (preferences.workspaceLogin) {
        startDestination = "workspace-list"
    }
    NavHost(navController = navController, startDestination = startDestination){
        composable("home") {
            LoginScreen(
                viewModel = koinViewModel(),
                preferences,
                navController,
                modifier = modifier.padding(innerPadding)
            )
        }
        composable("workspace-list"){
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
            WorkspaceList(onClick = {  }, modifier = Modifier.padding(innerPadding))
        }
    }
}
