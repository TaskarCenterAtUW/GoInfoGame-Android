package de.westnordost.streetcomplete.screens.workspaces

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import de.westnordost.streetcomplete.data.workspace.data.remote.WorkspaceAuthRejectedException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
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
 * Verifies the end-to-end login/auth flows through the real WorkSpaceActivity: logging in with
 * credentials or via an avivscr:// deep link, every way that can fail (each must show an error and
 * leave the user genuinely logged out, never stuck), and what happens to a saved session on app
 * start (expired -> login, valid -> straight in, proactive refresh rejected -> forced logout with
 * the Session Expired alert, refresh merely failing -> session kept).
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

    private val fakeRepository = FakeWorkspaceRepository()
    private lateinit var scenario: ActivityScenario<WorkSpaceActivity>

    @Before
    fun setUp() {
        enableDeviceLocation()

        // start every run logged out on the login screen, and with the save-credentials
        // biometric prompt disabled so a successful login navigates straight to the workspace
        // list instead of pausing on that dialog first
        clearSession()
        preferences.isBiometricEnabled = false

        loadKoinModules(module {
            single<WorkspaceRepository> { fakeRepository }
        })
    }

    /** Launched per test, since some need a saved session or a deep-link intent first. */
    private fun launch(intent: Intent? = null) {
        scenario = if (intent != null) {
            ActivityScenario.launch(intent)
        } else {
            ActivityScenario.launch(WorkSpaceActivity::class.java)
        }
    }

    @After
    fun tearDown() {
        // before scenario.close() - see UiTestScreenshot's kdoc for why this can't be a rule
        UiTestScreenshot.capture("${javaClass.simpleName}.${testName.methodName}")
        if (::scenario.isInitialized) scenario.close()
        // a forced logout relaunches WorkSpaceActivity as a new task, outside the scenario
        finishAllActivities()
        clearSession()
    }

    //region login with credentials

    @Test
    fun enteringValidCredentials_logsIn_andShowsWorkspaceList() {
        launch()
        logInAndWaitForWorkspaceList()
        assertTrue(preferences.workspaceLogin)
        assertEquals(FakeWorkspaceRepository.TEST_USER_ID, preferences.workspaceUserId)
    }

    // regression test for the toolbar wordmark ("AVIV" + " ScoutRoute") wrapping onto a second
    // line once the search icon eats into the available width - caught from a per-test CI
    // screenshot, not visible in the semantics tree by default (see WorkspaceTitleLineCount's
    // kdoc in WorkspaceListScreen.kt for why)
    @Test
    fun toolbarTitle_rendersOnOneLine() {
        launch()
        logInAndWaitForWorkspaceList()
        composeTestRule
            .onNode(SemanticsMatcher("has workspace title") { it.config.contains(WorkspaceTitleLineCount) })
            .assert(SemanticsMatcher.expectValue(WorkspaceTitleLineCount, 1))
    }

    @Test
    fun wrongCredentials_showError_andStayOnLogin() {
        fakeRepository.login = { flow { throw Exception("Invalid username or password.") } }
        launch()
        submitCredentials()

        waitForText("Invalid username or password.")
        composeTestRule.onNodeWithText("Login").assertIsDisplayed()
        assertFalse(preferences.workspaceLogin)
    }

    @Test
    fun noConnectivityOnLogin_showsError() {
        fakeRepository.login = { flow { throw Exception("Please check your internet connection and try again.") } }
        launch()
        submitCredentials()

        waitForText("Please check your internet connection and try again.")
        assertFalse(preferences.workspaceLogin)
    }

    // regression: the login itself succeeded but the user-profile call failed (e.g. 404) - the
    // error was cleared again before it could show, the user was stuck on the login screen, and
    // a half-logged-in session (workspaceLogin = true, no user id) was left behind
    @Test
    fun userProfileFailingAfterLogin_showsError_andLeavesUserLoggedOut() {
        fakeRepository.userInfo = { flow { throw Exception("User profile not found.") } }
        launch()
        submitCredentials()

        waitForText("User profile not found.")
        composeTestRule.onNodeWithText("Login").assertIsDisplayed()
        assertFalse(preferences.workspaceLogin)
        assertNull(preferences.workspaceToken)
        assertNull(preferences.workspaceUserId)
    }

    @Test
    fun retryAfterUserProfileFailure_logsIn() {
        fakeRepository.userInfo = { flow { throw Exception("User profile not found.") } }
        launch()
        submitCredentials()
        waitForText("User profile not found.")

        fakeRepository.userInfo = FakeWorkspaceRepository().userInfo
        composeTestRule.onNodeWithText("Login").performClick()
        waitForWorkspaceListToolbar()
        assertTrue(preferences.workspaceLogin)
        assertEquals(FakeWorkspaceRepository.TEST_USER_ID, preferences.workspaceUserId)
    }

    //endregion

    //region login via deep link (avivscr://...?code=<refresh token>)

    @Test
    fun deepLink_withValidCode_logsIn() {
        val refreshed = mutableListOf<String>()
        fakeRepository.refresh = { token ->
            refreshed.add(token)
            flowOf(FakeWorkspaceRepository.TEST_LOGIN_RESPONSE.copy(access_token = jwtWithEmail(TEST_EMAIL)))
        }
        launch(deepLink("deep-link-code"))

        waitForWorkspaceListToolbar()
        assertEquals(listOf("deep-link-code"), refreshed)
        // the deep link carries no email - it's read from the refreshed access token
        assertEquals(TEST_EMAIL, preferences.workspaceUserEmail)
        assertTrue(preferences.workspaceLogin)
        assertEquals(FakeWorkspaceRepository.TEST_USER_ID, preferences.workspaceUserId)
    }

    @Test
    fun deepLink_withRejectedCode_showsError_onLogin() {
        fakeRepository.refresh = { flow { throw WorkspaceAuthRejectedException("Your session has expired. Please log in again.") } }
        launch(deepLink("expired-code"))

        waitForText("Your session has expired. Please log in again.")
        composeTestRule.onNodeWithText("Login").assertIsDisplayed()
        assertFalse(preferences.workspaceLogin)
    }

    @Test
    fun deepLink_whenUserProfileFails_showsError_andLeavesUserLoggedOut() {
        fakeRepository.refresh = { flowOf(FakeWorkspaceRepository.TEST_LOGIN_RESPONSE.copy(access_token = jwtWithEmail(TEST_EMAIL))) }
        fakeRepository.userInfo = { flow { throw Exception("User profile not found.") } }
        launch(deepLink("deep-link-code"))

        waitForText("User profile not found.")
        composeTestRule.onNodeWithText("Login").assertIsDisplayed()
        assertFalse(preferences.workspaceLogin)
        assertNull(preferences.workspaceToken)
    }

    //endregion

    //region saved session on app start

    @Test
    fun validSessionOnStart_skipsLogin() {
        saveSession(accessExpiresInMs = HOUR, refreshExpiresInMs = DAY)
        launch()
        waitForWorkspaceListToolbar()
        composeTestRule.onAllNodesWithText("Email").assertCountEquals(0)
    }

    @Test
    fun expiredRefreshTokenOnStart_showsLogin_andClearsSession() {
        saveSession(accessExpiresInMs = -DAY, refreshExpiresInMs = -HOUR)
        launch()
        waitForText("Email")
        assertFalse(preferences.workspaceLogin)
        assertNull(preferences.workspaceRefreshToken)
    }

    @Test
    fun expiredAccessToken_refreshedOnStart_keepsSession() {
        saveSession(accessExpiresInMs = -HOUR, refreshExpiresInMs = DAY)
        launch()
        waitForWorkspaceListToolbar()
        waitUntil { preferences.workspaceToken == FakeWorkspaceRepository.TEST_LOGIN_RESPONSE.access_token }
        assertTrue(preferences.workspaceLogin)
        composeTestRule.onAllNodesWithText("Session Expired").assertCountEquals(0)
    }

    @Test
    fun expiredAccessToken_refreshRejectedOnStart_forcesLogoutWithAlert() {
        fakeRepository.refresh = { flow { throw WorkspaceAuthRejectedException("Your session has expired. Please log in again.") } }
        saveSession(accessExpiresInMs = -HOUR, refreshExpiresInMs = DAY)
        launch()

        waitForText("Session Expired")
        assertFalse(preferences.workspaceLogin)
        composeTestRule.onNodeWithText("Close").performClick()
        waitForText("Email")
    }

    // the refresh failing without the server rejecting the token (offline, server down) must not
    // cost the user their session
    @Test
    fun expiredAccessToken_refreshFailingOnStart_keepsSession() {
        fakeRepository.refresh = { flow { throw Exception("The server is temporarily unavailable. Please try again later.") } }
        saveSession(accessExpiresInMs = -HOUR, refreshExpiresInMs = DAY)
        launch()

        waitForWorkspaceListToolbar()
        Thread.sleep(1_000) // give a (wrong) forced logout the chance to happen
        composeTestRule.onAllNodesWithText("Session Expired").assertCountEquals(0)
        assertTrue(preferences.workspaceLogin)
        assertEquals("saved-refresh", preferences.workspaceRefreshToken)
    }

    //endregion

    //region helpers

    private fun submitCredentials() {
        composeTestRule.onNodeWithText("Email").performTextInput(TEST_EMAIL)
        composeTestRule.onNodeWithText("Password").performTextInput(TEST_PASSWORD)
        // see logInAndWaitForWorkspaceList for why the keyboard is closed first
        Espresso.closeSoftKeyboard()
        composeTestRule.onNodeWithText("Login").performClick()
    }

    private fun waitForText(text: String) {
        composeTestRule.waitUntil(timeoutMillis = 20_000) {
            composeTestRule.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForWorkspaceListToolbar() {
        composeTestRule.waitUntil(timeoutMillis = 20_000) {
            composeTestRule.onAllNodesWithContentDescription(PROFILE_NAV_DESCRIPTION).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitUntil(condition: () -> Boolean) {
        composeTestRule.waitUntil(timeoutMillis = 10_000) { condition() }
    }

    /** A logged-in session as a previous app run would have left it, expiring relative to now. */
    private fun saveSession(accessExpiresInMs: Long, refreshExpiresInMs: Long) {
        val now = System.currentTimeMillis()
        preferences.workspaceLogin = true
        preferences.workspaceToken = "saved-access"
        preferences.workspaceRefreshToken = "saved-refresh"
        preferences.workspaceUserEmail = TEST_EMAIL
        preferences.workspaceUserId = FakeWorkspaceRepository.TEST_USER_ID
        preferences.workspaceLastLogin = now - HOUR
        preferences.accessTokenExpiryInterval = HOUR
        preferences.refreshTokenExpiryInterval = DAY
        preferences.accessTokenExpiryTime = now + accessExpiresInMs
        preferences.refreshTokenExpiryTime = now + refreshExpiresInMs
    }

    private fun clearSession() {
        preferences.workspaceLogin = false
        preferences.workspaceToken = null
        preferences.workspaceRefreshToken = null
        preferences.workspaceUserId = null
        preferences.workspaceUserEmail = null
        preferences.accessTokenExpiryTime = 0L
        preferences.refreshTokenExpiryTime = 0L
    }

    private fun deepLink(code: String) = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("avivscr://login?code=$code"),
        InstrumentationRegistry.getInstrumentation().targetContext,
        WorkSpaceActivity::class.java
    )

    /** An (unsigned) JWT whose payload carries [email] - all the deep-link login reads from it. */
    private fun jwtWithEmail(email: String): String {
        fun encode(json: String) =
            Base64.encodeToString(json.toByteArray(), Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        return encode("""{"alg":"none","typ":"JWT"}""") + "." + encode("""{"email":"$email"}""") + ".c2ln"
    }

    private fun finishAllActivities() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val monitor = ActivityLifecycleMonitorRegistry.getInstance()
            listOf(Stage.RESUMED, Stage.STARTED, Stage.PAUSED, Stage.STOPPED, Stage.CREATED)
                .flatMap { monitor.getActivitiesInStage(it) }
                .forEach { it.finish() }
        }
    }

        private fun logInAndWaitForWorkspaceList() {
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

    //endregion

    private companion object {
        const val TEST_EMAIL = "test.user@example.com"
        const val TEST_PASSWORD = "Test@1234"
        const val PROFILE_NAV_DESCRIPTION = "Navigate to profile screen"
        const val HOUR = 60 * 60 * 1000L
        const val DAY = 24 * HOUR
    }
}
