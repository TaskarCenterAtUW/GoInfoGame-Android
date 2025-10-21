package de.westnordost.streetcomplete.screens.user

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.koin.androidx.compose.koinViewModel

/** There are two screens on the user screen: The login and the user screen. Which one is displayed
 *  depends on whether the user is logged in or not. */
@Composable
fun UserNavHost(
    launchAuth: Boolean,
    onClickBack: () -> Unit,
) {
    val navController = rememberNavController()
    val viewModel = koinViewModel<UserViewModel>()

    NavHost(
        navController = navController,
        startDestination = UserDestination.User
    ) {
        composable(UserDestination.User) {
            UserScreen(
                onClickBack = onClickBack
            )
        }
    }
}

private object UserDestination {
    const val Login = "login"
    const val User = "user"
}
