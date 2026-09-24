package de.westnordost.streetcomplete.data.workspace

import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.HttpMetric
import com.russhwolf.settings.MapSettings
import de.westnordost.streetcomplete.data.preferences.EnvironmentManager
import de.westnordost.streetcomplete.data.preferences.Preferences
import de.westnordost.streetcomplete.data.user.WorkspaceConfigProvider
import de.westnordost.streetcomplete.data.workspace.data.remote.WorkspaceApiService
import de.westnordost.streetcomplete.data.workspace.data.remote.WorkspaceAuthRejectedException
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
}
