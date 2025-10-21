package de.westnordost.streetcomplete.screens.user

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import de.westnordost.streetcomplete.resources.Res
import de.westnordost.streetcomplete.resources.ic_profile_24
import de.westnordost.streetcomplete.resources.user_profile
import de.westnordost.streetcomplete.resources.user_profile_title
import de.westnordost.streetcomplete.screens.user.profile.ProfileScreen
import de.westnordost.streetcomplete.ui.common.BackIcon
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Shows the tabs with the user profile, user statistics, achievements and links */
@Composable
fun UserScreen(
    onClickBack: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        val pagerState = rememberPagerState(pageCount = { UserTab.entries.size })
        UserScreenTopAppBar(
            onClickBack = onClickBack,
            pagerState = pagerState
        )
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalAlignment = Alignment.Top,
        ) { p ->
            when (UserTab.entries[p]) {
                UserTab.Profile -> {
                    ProfileScreen(viewModel = koinViewModel())
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserScreenTopAppBar(
    onClickBack: () -> Unit,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column {
            TopAppBar(
                title = { Text(stringResource(Res.string.user_profile)) },
                windowInsets = TopAppBarDefaults.windowInsets,
                navigationIcon = { IconButton(onClick = onClickBack) { BackIcon() } },
            )

            val scope = rememberCoroutineScope()
            val page = pagerState.targetPage

            BoxWithConstraints {
                TabRow(
                    selectedTabIndex = page,
                    modifier = Modifier.windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
                    )
                ) {
                    for (tab in UserTab.entries) {
                        val icon = painterResource(tab.icon)
                        val text = stringResource(tab.text)
                        val index = tab.ordinal
                        val showText = min(maxWidth, maxHeight) >= 600.dp
                        Tab(
                            selected = page == index,
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                            icon = { Icon(icon, text) },
                            text = if (showText) {
                                { Text(text) }
                            } else {
                                null
                            }
                        )
                    }
                }
            }
        }
    }
}

private enum class UserTab(
    val icon: DrawableResource,
    val text: StringResource,
) {
    Profile(
        icon = Res.drawable.ic_profile_24,
        text = Res.string.user_profile_title,
    ),
}
