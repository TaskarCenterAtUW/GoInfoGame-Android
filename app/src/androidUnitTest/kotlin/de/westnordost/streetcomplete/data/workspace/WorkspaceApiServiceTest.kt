package de.westnordost.streetcomplete.data.workspace

import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.HttpMetric
import com.russhwolf.settings.MapSettings
import de.westnordost.streetcomplete.data.preferences.EnvironmentManager
import de.westnordost.streetcomplete.data.preferences.Preferences
import de.westnordost.streetcomplete.data.user.WorkspaceConfigProvider
import de.westnordost.streetcomplete.data.workspace.data.remote.WorkspaceApiService
import de.westnordost.streetcomplete.data.workspace.data.remote.WorkspaceAuthRejectedException
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.LongFormResponse
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.mockito.MockedStatic
import org.mockito.Mockito
import java.nio.channels.UnresolvedAddressException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WorkspaceApiServiceTest {

    private lateinit var firebasePerformance: MockedStatic<FirebasePerformance>
    private val preferences = Preferences(MapSettings())
    private val requests = mutableListOf<HttpRequestData>()

    private val loginJson =
        """{"access_token":"new-access","refresh_token":"new-refresh","expires_in":300,"refresh_expires_in":1800}"""

    @BeforeTest fun setUp() {
        // performHttpCallWithFirebaseTracing reports every call to Firebase Performance, which
        // isn't initialized in a JVM test
        val perf = Mockito.mock(FirebasePerformance::class.java)
        Mockito.`when`(perf.newHttpMetric(Mockito.anyString(), Mockito.anyString()))
            .thenReturn(Mockito.mock(HttpMetric::class.java))
        firebasePerformance = Mockito.mockStatic(FirebasePerformance::class.java)
        firebasePerformance.`when`<FirebasePerformance> { FirebasePerformance.getInstance() }.thenReturn(perf)
    }

    @AfterTest fun tearDown() {
        firebasePerformance.close()
    }

    /** A service whose HTTP calls are answered by [responses] in order (the last one repeats). */
    private fun service(
        vararg responses: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
        workspaceToken: String? = "stored-access",
    ): WorkspaceApiService {
        val engine = MockEngine { request ->
            requests.add(request)
            responses[minOf(requests.size, responses.size) - 1](this, request)
        }
        // same plugins the real client is set up with in ApplicationModule
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(Auth) { bearer { loadTokens { null } } }
        }
        val config = object : WorkspaceConfigProvider {
            override val osmBaseUrl = "https://osm.example"
            override val workspaceToken = workspaceToken
            override val workspaceId: Int? = null
            override val userId: String? = null
        }
        return WorkspaceApiService(client, preferences, EnvironmentManager(preferences), config, client)
    }

    private fun json(status: HttpStatusCode, body: String): MockRequestHandleScope.(HttpRequestData) -> HttpResponseData =
        { respond(body, status, headersOf(HttpHeaders.ContentType, "application/json")) }

    private fun status(status: HttpStatusCode): MockRequestHandleScope.(HttpRequestData) -> HttpResponseData =
        json(status, """{"message":"${status.description}"}""")

    private fun networkDown(): MockRequestHandleScope.(HttpRequestData) -> HttpResponseData =
        { throw UnresolvedAddressException() }

    //region login

    @Test fun `login returns the tokens and stores them`() = runTest {
        val response = service(json(HttpStatusCode.OK, loginJson)).loginToWorkspace(" user@example.com ", "secret ")

        assertEquals("new-access", response.access_token)
        assertEquals("new-access", preferences.workspaceToken)
        assertEquals("new-refresh", preferences.workspaceRefreshToken)

        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertTrue(request.url.encodedPath.endsWith("/authenticate"))
        // credentials are trimmed before being sent
        assertEquals("""{"username":"user@example.com","password":"secret"}""", (request.body as TextContent).text)
    }

    @Test fun `login with wrong credentials says so`() = runTest {
        for (code in listOf(HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden)) {
            val e = assertFailsWith<Exception> { service(status(code)).loginToWorkspace("u", "p") }
            assertEquals("Invalid username or password.", e.message, "for $code")
        }
        assertNull(preferences.workspaceToken)
    }

    @Test fun `login server error gives a friendly message`() = runTest {
        val e = assertFailsWith<Exception> { service(status(HttpStatusCode.InternalServerError)).loginToWorkspace("u", "p") }
        assertEquals("The server is temporarily unavailable. Please try again later.", e.message)
    }

    @Test fun `login without connectivity gives a friendly message`() = runTest {
        val e = assertFailsWith<Exception> { service(networkDown()).loginToWorkspace("u", "p") }
        assertEquals("Please check your internet connection and try again.", e.message)
    }

    //endregion

    //region user profile

    @Test fun `user profile is fetched with the stored token for the given user`() = runTest {
        val info = service(json(HttpStatusCode.OK, """{"id":"user-1","username":"jdoe","firstName":"J","lastName":"Doe"}"""))
            .getTDEIUserDetails("user@example.com")

        assertEquals("user-1", info.id)
        val request = requests.single()
        assertTrue(request.url.encodedPath.endsWith("/user-profile"))
        assertEquals("user@example.com", request.url.parameters["user_name"])
        assertEquals("Bearer stored-access", request.headers[HttpHeaders.Authorization])
    }

    @Test fun `user profile 404 with a JSON body fails instead of returning an empty profile`() = runTest {
        // regression: every UserInfoResponse field is nullable, so this body used to "parse"
        // into an all-null profile and log the user in without a user id, silently
        val e = assertFailsWith<Exception> {
            service(json(HttpStatusCode.NotFound, """{"message":"not found"}""")).getTDEIUserDetails("u")
        }
        assertEquals("User profile not found.", e.message)
    }

    @Test fun `user profile other errors give friendly messages`() = runTest {
        val expired = assertFailsWith<Exception> { service(status(HttpStatusCode.Unauthorized)).getTDEIUserDetails("u") }
        assertEquals("Your session has expired. Please log in again.", expired.message)

        requests.clear()
        val down = assertFailsWith<Exception> { service(status(HttpStatusCode.BadGateway)).getTDEIUserDetails("u") }
        assertEquals("The server is temporarily unavailable. Please try again later.", down.message)
    }

    //endregion

    //region refresh

    @Test fun `refresh sends the refresh token as a header, unauthenticated, and stores the new tokens`() = runTest {
        val response = service(json(HttpStatusCode.OK, loginJson)).refreshToken("old-refresh")

        assertEquals("new-refresh", response.refresh_token)
        assertEquals("new-access", preferences.workspaceToken)
        assertEquals("new-refresh", preferences.workspaceRefreshToken)
        val request = requests.single()
        assertTrue(request.url.encodedPath.endsWith("/refresh-token"))
        assertEquals("old-refresh", request.headers["refresh_token"])
        assertNull(request.headers[HttpHeaders.Authorization])
    }

    @Test fun `refresh rejected by the server means the session is over`() = runTest {
        for (code in listOf(HttpStatusCode.Unauthorized, HttpStatusCode.BadRequest, HttpStatusCode.Forbidden)) {
            requests.clear()
            assertFailsWith<WorkspaceAuthRejectedException>("for $code") { service(status(code)).refreshToken("r") }
            assertEquals(1, requests.size, "a rejection is not retried")
        }
    }

    @Test fun `a transient refresh failure is retried`() = runTest {
        val response = service(status(HttpStatusCode.ServiceUnavailable), json(HttpStatusCode.OK, loginJson))
            .refreshToken("r")
        assertEquals("new-access", response.access_token)
        assertEquals(2, requests.size)
    }

    @Test fun `a server that keeps failing is not treated as a rejected refresh token`() = runTest {
        // regression: this used to throw WorkspaceAuthRejectedException, and the workspace list
        // then forced a logout because of a server outage
        for (code in listOf(HttpStatusCode.InternalServerError, HttpStatusCode.ServiceUnavailable, HttpStatusCode.TooManyRequests)) {
            requests.clear()
            val e = assertFailsWith<Exception>("for $code") { service(status(code)).refreshToken("r") }
            assertFalse(e is WorkspaceAuthRejectedException, "for $code")
            assertEquals(3, requests.size, "retried before giving up, for $code")
        }
    }

    @Test fun `refresh without connectivity keeps the original exception type`() = runTest {
        // WorkspaceViewModel relies on this to tell "offline" (keep the session) from "rejected"
        assertFailsWith<UnresolvedAddressException> { service(networkDown()).refreshToken("r") }
        assertEquals(3, requests.size)
    }

    //endregion

    //region unexpected response bodies

    // regression: Ktor wraps the parse error in a JsonConvertException, which wasn't recognized,
    // so the raw "Illegal input: Fields [...] are required ..." was shown to the user
    @Test fun `a response body that doesn't parse gives a friendly message on every endpoint`() = runTest {
        val expected = "Received an unexpected response from the server. Please contact your workspace admin if this continues."
        val calls: List<Pair<String, suspend WorkspaceApiService.() -> Unit>> = listOf(
            "login" to { loginToWorkspace("u", "p") },
            "user profile" to { getTDEIUserDetails("u") },
            "workspace list" to { getWorkspaces(Mockito.mock(android.location.Location::class.java)) },
            "project groups" to { getUserProjectGroups() },
        )
        for ((name, call) in calls) {
            for (body in listOf("""{"unexpected":true}""", "not json at all")) {
                requests.clear()
                val e = assertFailsWith<Exception>("$name, body $body") {
                    service(json(HttpStatusCode.OK, body)).call()
                }
                assertEquals(expected, e.message, "$name, body $body")
            }
        }
    }

    //endregion

    //region app update info (cached for 6 hours)

    private val updateInfoJson = """{"android":{
        "dev":{"latest_version":"1.2.0","min_required_version":"1.0.0"},
        "prod":{"latest_version":"1.2.0","min_required_version":"1.0.0"},
        "stage":{"latest_version":"1.2.0","min_required_version":"1.0.0"}}}"""

    @Test fun `update info is fetched once and then served from the cache`() = runTest {
        val service = service(json(HttpStatusCode.OK, updateInfoJson))
        assertEquals("1.2.0", service.getForceUpdateInfo().android.prod.latestVersion)
        assertEquals("1.2.0", service.getForceUpdateInfo().android.prod.latestVersion)
        assertEquals(1, requests.size)
    }

    // regression: the response was cached before being parsed, so a malformed one was served
    // from the cache and failed every update check for the next 6 hours
    @Test fun `a malformed update info response is not cached`() = runTest {
        val service = service(json(HttpStatusCode.OK, """{"unexpected":true}"""), json(HttpStatusCode.OK, updateInfoJson))

        val e = assertFailsWith<Exception> { service.getForceUpdateInfo() }
        assertEquals("Received an unexpected response from the server. Please contact your workspace admin if this continues.", e.message)
        assertNull(preferences.configJson)
        assertNull(preferences.configLastFetchTime)

        assertEquals("1.2.0", service.getForceUpdateInfo().android.prod.latestVersion)
        assertEquals(2, requests.size)
    }

    @Test fun `a cached update info that doesn't parse is fetched again`() = runTest {
        // left behind by a version that cached before parsing
        preferences.configLastFetchTime = System.currentTimeMillis()
        preferences.configJson = """{"unexpected":true}"""

        assertEquals("1.2.0", service(json(HttpStatusCode.OK, updateInfoJson)).getForceUpdateInfo().android.prod.latestVersion)
        assertEquals(1, requests.size)
        assertEquals(updateInfoJson, preferences.configJson)
    }

    //endregion

    //region workspace details (tapping a workspace in the list)

    @Test fun `workspace details are fetched for the tapped workspace with the stored token`() = runTest {
        service(json(HttpStatusCode.OK, workspaceDetailsJson(id = 7))).getWorkspaceDetails(7)

        val request = requests.single()
        assertEquals(HttpMethod.Get, request.method)
        assertEquals(
            EnvironmentManager(preferences).currentEnvironment.workspaceBaseUrl + "/7",
            request.url.toString()
        )
        assertEquals("Bearer stored-access", request.headers[HttpHeaders.Authorization])
    }

    @Test fun `workspace details carry a valid long form`() = runTest {
        val details = service(json(HttpStatusCode.OK, workspaceDetailsJson(id = 7))).getWorkspaceDetails(7)

        assertEquals(7, details.id)
        assertEquals("Test Workspace", details.title)
        val longForm = Json { ignoreUnknownKeys = true }
            .decodeFromJsonElement(LongFormResponse.serializer(), assertNotNull(details.longFormQuestDef))
        assertEquals("3.2.0", longForm.version)
        assertEquals(30, longForm.recencyPeriodInDays)
        assertEquals(listOf("Bench"), longForm.featurePresets.map { it.name })
        assertEquals(listOf("streetlight"), longForm.customIcons.map { it.name })
        assertValidLongForm(longForm.elements)
        assertEquals(listOf(101, 102, 103, 104), longForm.elements.single().quests.map { it?.questId })
    }

    @Test fun `overrideConflicts is read from the workspace details`() = runTest {
        for ((raw, expected) in listOf("true" to true, "false" to false, "null" to null)) {
            requests.clear()
            val details = service(json(HttpStatusCode.OK, workspaceDetailsJson(overrideConflicts = raw)))
                .getWorkspaceDetails(7)
            assertEquals(expected, details.overrideConflicts, "for \"overrideConflicts\": $raw")
        }
    }

    @Test fun `overrideConflicts missing from the workspace details is null, not an error`() = runTest {
        val details = service(json(HttpStatusCode.OK, workspaceDetailsJson(overrideConflicts = null)))
            .getWorkspaceDetails(7)
        assertNull(details.overrideConflicts)
    }

    @Test fun `unknown workspace says which one`() = runTest {
        val e = assertFailsWith<Exception> {
            service(status(HttpStatusCode.NotFound)).getWorkspaceDetails(42)
        }
        assertEquals("Failed. Workspace not found with ID : 42", e.message)
    }

    @Test fun `workspace details errors give friendly messages`() = runTest {
        val expired = assertFailsWith<Exception> { service(status(HttpStatusCode.Unauthorized)).getWorkspaceDetails(7) }
        assertEquals("Your session has expired. Please log in again.", expired.message)

        requests.clear()
        val forbidden = assertFailsWith<Exception> { service(status(HttpStatusCode.Forbidden)).getWorkspaceDetails(7) }
        assertEquals("You don't have permission to perform this action.", forbidden.message)

        requests.clear()
        val down = assertFailsWith<Exception> { service(status(HttpStatusCode.InternalServerError)).getWorkspaceDetails(7) }
        assertEquals("The server is temporarily unavailable. Please try again later.", down.message)

        requests.clear()
        val offline = assertFailsWith<Exception> { service(networkDown()).getWorkspaceDetails(7) }
        assertEquals("Please check your internet connection and try again.", offline.message)
    }

    @Test fun `workspace details missing a required field is reported as a misconfigured workspace`() = runTest {
        val e = assertFailsWith<Exception> {
            service(json(HttpStatusCode.OK, """{"id":7,"title":"Test Workspace"}""")).getWorkspaceDetails(7)
        }
        assertEquals("Workspace is not configured properly. Please contact the Admin for the workspace", e.message)
    }

    //endregion
}
