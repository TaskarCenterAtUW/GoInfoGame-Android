package de.westnordost.streetcomplete.screens.workspaces

import android.Manifest
import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import de.westnordost.streetcomplete.data.preferences.Preferences
import de.westnordost.streetcomplete.data.workspace.domain.WorkspaceRepository
import de.westnordost.streetcomplete.testutils.UiTestScreenshot
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module

/**
 * Verifies the end-to-end login flow: entering credentials on the login screen successfully logs
 * the user in and lands them on the workspace list screen with their workspace(s) shown.
 *
 * The real network layer is swapped for [FakeWorkspaceRepository] via a Koin module override
 * (there is no test backend to log in against), and permissions/location are pre-granted/enabled
 * so the flow can proceed to `workspace-list` without any system dialogs.
 */
@RunWith(AndroidJUnit4::class)
class WorkspaceLoginFlowTest {

    // grants both permissions WorkSpaceActivity's PermissionHandler asks for, before the
    // activity is launched below, so no system permission dialog appears mid-test.
    // POST_NOTIFICATIONS only exists from API 33 - GrantPermissionRule throws on older platforms
    // if asked to grant an unknown permission, so it's only included there.
    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        *(if (android.os.Build.VERSION.SDK_INT >= 33) {
            arrayOf(Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        })
    )

    // not createAndroidComposeRule<WorkSpaceActivity>() - that would launch the activity as part
    // of applying the rule, before setUp() below gets a chance to install the fake repository and
    // reset login state. createEmptyComposeRule() lets us launch the activity ourselves, once
    // everything it depends on is ready, while still synchronizing/asserting against whatever
    // Compose content that activity ends up showing.
    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @get:Rule
    val testName = TestName()

    private val preferences: Preferences = GlobalContext.get().get()

    private lateinit var scenario: ActivityScenario<WorkSpaceActivity>

    @Before
    fun setUp() {
        enableDeviceLocation()

        // start every run logged out on the login screen, and with the save-credentials
        // biometric prompt disabled so a successful login navigates straight to the workspace
        // list instead of pausing on that dialog first
        preferences.workspaceLogin = false
        preferences.isBiometricEnabled = false

        loadKoinModules(module {
            single<WorkspaceRepository> { FakeWorkspaceRepository() }
        })

        scenario = ActivityScenario.launch(WorkSpaceActivity::class.java)
    }

    @After
    fun tearDown() {
        // before scenario.close() - see UiTestScreenshot's kdoc for why this can't be a rule
        UiTestScreenshot.capture("${javaClass.simpleName}.${testName.methodName}")
        scenario.close()
        preferences.workspaceLogin = false
    }

    @Test
    fun enteringValidCredentials_logsIn_andShowsWorkspaceList() {
        composeTestRule.onNodeWithText("Email").performTextInput(TEST_EMAIL)
        composeTestRule.onNodeWithText("Password").performTextInput(TEST_PASSWORD)
        // closing the keyboard before clicking Login avoids a real hang on API 33+ headless
        // emulators: a still-animating IME inset transition can stall the main thread's
        // Choreographer/message queue indefinitely, which starves viewModelScope.launch (main
        // dispatcher) from ever running - Compose's own test-polling loop keeps ticking
        // independently of that stall, so waitUntil below would otherwise spin until timeout
        // with no app-visible progress at all, even though performClick() itself returns fine
        Espresso.closeSoftKeyboard()
        composeTestRule.onNodeWithText("Login").performClick()

        // login -> setLoginState() -> getUserInfo() is a chain of suspend/async steps (see the
        // comment on WorkspaceLoginState.Success in WorkspaceLoginScreen.kt), so the toolbar
        // showing up is awaited rather than asserted immediately
        composeTestRule.waitUntil(timeoutMillis = 20_000) {
            composeTestRule
                .onAllNodesWithContentDescription(PROFILE_NAV_DESCRIPTION)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithContentDescription(PROFILE_NAV_DESCRIPTION).assertIsDisplayed()

        // the toolbar (and its Loading/Error states) render regardless of whether the workspace
        // list itself has finished loading - that only happens once a real device location fix
        // comes back and fetchWorkspaces() runs (see AppNavigator's "workspace-list" composable),
        // which is a separate, slower async step than the login above, so it needs its own wait
        composeTestRule.waitUntil(timeoutMillis = 20_000) {
            composeTestRule
                .onAllNodesWithText(FakeWorkspaceRepository.TEST_WORKSPACE_TITLE)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithText(FakeWorkspaceRepository.TEST_WORKSPACE_TITLE).assertIsDisplayed()
    }

    // WorkSpaceActivity blocks on ShowLocationEnableUI() until a location provider is enabled;
    // best-effort since most CI emulators already ship with location on and this shell command
    // isn't guaranteed to exist on every API level/device.
    private fun enableDeviceLocation() {
        try {
            val pfd: ParcelFileDescriptor = InstrumentationRegistry.getInstrumentation().uiAutomation
                .executeShellCommand("cmd location set-location-enabled true")
            ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes() }
        } catch (e: Exception) {
            // ignored - see comment above
        }
    }

    private companion object {
        const val TEST_EMAIL = "test.user@example.com"
        const val TEST_PASSWORD = "Test@1234"
        const val PROFILE_NAV_DESCRIPTION = "Navigate to profile screen"
    }
}
