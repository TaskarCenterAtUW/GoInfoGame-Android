package de.westnordost.streetcomplete

import com.russhwolf.settings.MapSettings
import de.westnordost.streetcomplete.data.preferences.EnvironmentManager
import de.westnordost.streetcomplete.data.preferences.Preferences
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The reactive token refresh the bearer-auth plugin runs on a 401 (ApplicationModule.kt) - only
 *  testable since its HttpClient is injected instead of created inside. */
class RefreshJwtTokenTest {

    private val preferences = Preferences(MapSettings())
    private val environmentManager = EnvironmentManager(preferences)
    private val requests = mutableListOf<HttpRequestData>()

    private val loginJson =
        """{"access_token":"new-access","refresh_token":"new-refresh","expires_in":300,"refresh_expires_in":1800}"""

    @BeforeTest fun setUp() {
        preferences.workspaceToken = "old-access"
        preferences.workspaceRefreshToken = "old-refresh"
    }

    private fun client(vararg answers: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) =
        HttpClient(MockEngine { request ->
            requests.add(request)
            answers[minOf(requests.size, answers.size) - 1](this, request)
        })

    private fun json(status: HttpStatusCode, body: String): MockRequestHandleScope.(HttpRequestData) -> HttpResponseData =
        { respond(body, status, headersOf(HttpHeaders.ContentType, "application/json")) }

    @Test fun `refresh stores the rotated tokens and returns the new access token`() = runTest {
        val before = System.currentTimeMillis()
        val token = refreshJwtToken(preferences, environmentManager, client(json(HttpStatusCode.OK, loginJson)))

        assertEquals("new-access", token)
        assertEquals("new-access", preferences.workspaceToken)
        assertEquals("new-refresh", preferences.workspaceRefreshToken)
        assertEquals(300_000L, preferences.accessTokenExpiryInterval)
        assertEquals(1_800_000L, preferences.refreshTokenExpiryInterval)
        assertTrue(preferences.workspaceLastLogin >= before)
        assertEquals(preferences.workspaceLastLogin + 300_000L, preferences.accessTokenExpiryTime)
        assertEquals(preferences.workspaceLastLogin + 1_800_000L, preferences.refreshTokenExpiryTime)
    }

    @Test fun `refresh sends the refresh token as a header, unauthenticated, to the TDEI endpoint`() = runTest {
        refreshJwtToken(preferences, environmentManager, client(json(HttpStatusCode.OK, loginJson)))

        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals(environmentManager.currentEnvironment.tdeiBaseUrl + "/refresh-token", request.url.toString())
        assertEquals("old-refresh", request.headers["refresh_token"])
        assertNull(request.headers[HttpHeaders.Authorization])
    }

    @Test fun `rejected refresh returns null and keeps the stored tokens`() = runTest {
        for (code in listOf(HttpStatusCode.Unauthorized, HttpStatusCode.BadRequest, HttpStatusCode.Forbidden)) {
            requests.clear()
            assertNull(refreshJwtToken(preferences, environmentManager, client(json(code, """{"message":"no"}"""))), "for $code")
            assertEquals(1, requests.size, "a rejection is not retried, for $code")
        }
        assertEquals("old-refresh", preferences.workspaceRefreshToken)
    }

    @Test fun `a transient failure is retried`() = runTest {
        val token = refreshJwtToken(
            preferences, environmentManager,
            client(json(HttpStatusCode.ServiceUnavailable, "{}"), json(HttpStatusCode.OK, loginJson))
        )
        assertEquals("new-access", token)
        assertEquals(2, requests.size)
    }

    @Test fun `unreachable server returns null after retrying`() = runTest {
        assertNull(refreshJwtToken(preferences, environmentManager, client({ throw IOException("offline") })))
        assertEquals(3, requests.size)
    }
}
