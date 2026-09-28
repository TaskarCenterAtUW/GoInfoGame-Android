package de.westnordost.streetcomplete.testutils

import androidx.test.ext.junit.runners.AndroidJUnit4
import de.westnordost.streetcomplete.data.preferences.EnvironmentManager
import de.westnordost.streetcomplete.data.preferences.Preferences
import de.westnordost.streetcomplete.refreshJwtToken
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.koin.core.qualifier.named
import java.io.IOException
import kotlin.test.assertFailsWith

/** The instrumented tests can't reach real servers through the app's own clients - see
 *  [RealNetworkGuard], installed by [SandboxedTestRunner]. */
@RunWith(AndroidJUnit4::class)
class RealNetworkGuardTest {

    private val koin = GlobalContext.get()
    private val preferences: Preferences = koin.get()

    @After fun tearDown() {
        MockOsmServer.isActive = false
    }

    // the path that most likely leaked to production TDEI on 2026-09-25: a 401 makes the
    // bearer-auth plugin call refreshJwtToken() - with the app's own refresh client it's blocked
    @Test fun tokenRefresh_neverReachesTheRealTdeiServer() = runBlocking<Unit> {
        val before = RealNetworkGuard.blocked.size
        val token = refreshJwtToken(preferences, EnvironmentManager(preferences), koin.get(named("refreshClient")))

        assertNull(token)
        val blocked = RealNetworkGuard.blocked.drop(before)
        assertTrue(blocked.toString(), blocked.isNotEmpty())
        assertTrue(blocked.toString(), blocked.all { it.startsWith("refreshClient: POST ") && it.endsWith("/refresh-token") })
    }

    @Test fun osmRequestNotAnsweredByTheMock_isBlocked() = runBlocking<Unit> {
        val client: HttpClient = koin.get(named("osmClient"))
        val url = EnvironmentManager(preferences).currentEnvironment.osmUrl + "map"
        assertFailsWith<IOException> { client.get(url) }
        assertTrue(RealNetworkGuard.blocked.last().startsWith("osmClient: GET "))
    }

    @Test fun osmRequestAnsweredByTheMock_goesThrough() = runBlocking<Unit> {
        MockOsmServer.reset()
        val client: HttpClient = koin.get(named("osmClient"))
        MockOsmServer.attachTo(client)
        MockOsmServer.isActive = true

        val response = client.get(EnvironmentManager(preferences).currentEnvironment.osmUrl + "map")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("<osm"))
        assertEquals(listOf("GET map"), MockOsmServer.snapshot().requests)
    }

    @Test fun workspaceApiClient_isGuardedToo() = runBlocking<Unit> {
        val client: HttpClient = koin.get()
        assertFailsWith<IOException> {
            client.get(EnvironmentManager(preferences).currentEnvironment.workspaceBaseUrl + "/mine")
        }
    }
}
