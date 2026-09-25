package de.westnordost.streetcomplete.data.workspace

import android.location.Location
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.HttpMetric
import com.russhwolf.settings.MapSettings
import de.westnordost.streetcomplete.data.preferences.EnvironmentManager
import de.westnordost.streetcomplete.data.preferences.Preferences
import de.westnordost.streetcomplete.data.user.WorkspaceConfigProvider
import de.westnordost.streetcomplete.data.workspace.data.remote.WorkspaceApiService
import de.westnordost.streetcomplete.data.workspace.data.repository.WorkspaceRepositoryImpl
import de.westnordost.streetcomplete.testutils.argumentCaptor
import de.westnordost.streetcomplete.testutils.capture
import de.westnordost.streetcomplete.testutils.mock
import de.westnordost.streetcomplete.testutils.on
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * The conflict mode (`overrideConflicts`) only comes with the workspace details fetched when the
 * user taps a workspace; [de.westnordost.streetcomplete.data.osm.edits.upload.ElementEditUploader]
 * later reads it from the workspace table. These check it gets from the HTTP response into that
 * table, and that the workspace-list refresh (whose endpoint doesn't return it) doesn't wipe it.
 */
class WorkspaceRepositoryImplTest {

    private lateinit var firebasePerformance: MockedStatic<FirebasePerformance>
    private val preferences = Preferences(MapSettings())
    private lateinit var dao: WorkspaceDao

    @BeforeTest fun setUp() {
        val perf = Mockito.mock(FirebasePerformance::class.java)
        Mockito.`when`(perf.newHttpMetric(Mockito.anyString(), Mockito.anyString()))
            .thenReturn(Mockito.mock(HttpMetric::class.java))
        firebasePerformance = Mockito.mockStatic(FirebasePerformance::class.java)
        firebasePerformance.`when`<FirebasePerformance> { FirebasePerformance.getInstance() }.thenReturn(perf)
        dao = mock()
    }

    @AfterTest fun tearDown() {
        firebasePerformance.close()
    }

    /** A repository backed by the real API service, whose server answers every call with [body]. */
    private fun repository(body: String, status: HttpStatusCode = HttpStatusCode.OK): WorkspaceRepositoryImpl {
        val client = HttpClient(MockEngine {
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(Auth) { bearer { loadTokens { null } } }
        }
        val config = object : WorkspaceConfigProvider {
            override val osmBaseUrl = "https://osm.example"
            override val workspaceToken = "stored-access"
            override val workspaceId: Int? = null
            override val userId: String? = "user-1"
        }
        val api = WorkspaceApiService(client, preferences, EnvironmentManager(preferences), config, client)
        return WorkspaceRepositoryImpl(api, dao)
    }

    @Test fun `overrideConflicts true is stored for the tapped workspace`() = runTest {
        val details = repository(workspaceDetailsJson(id = 7, overrideConflicts = "true"))
            .getWorkspaceDetails(7).single()

        assertEquals(true, details.overrideConflicts)
        verify(dao).updateOverrideConflicts(7, true)
    }

    @Test fun `overrideConflicts false is stored for the tapped workspace`() = runTest {
        repository(workspaceDetailsJson(id = 7, overrideConflicts = "false")).getWorkspaceDetails(7).single()
        verify(dao).updateOverrideConflicts(7, false)
    }

    @Test fun `overrideConflicts null or missing is stored as false - the RESOLVE default`() = runTest {
        repository(workspaceDetailsJson(id = 7, overrideConflicts = "null")).getWorkspaceDetails(7).single()
        repository(workspaceDetailsJson(id = 7, overrideConflicts = null)).getWorkspaceDetails(7).single()
        verify(dao, Mockito.times(2)).updateOverrideConflicts(7, false)
    }

    @Test fun `the mode is stored under the id that was tapped`() = runTest {
        repository(workspaceDetailsJson(id = 7, overrideConflicts = "true")).getWorkspaceDetails(12).single()
        verify(dao).updateOverrideConflicts(12, true)
    }

    @Test fun `failing to fetch the workspace details leaves the stored mode alone`() = runTest {
        assertFailsWith<Exception> {
            repository("""{"message":"boom"}""", HttpStatusCode.InternalServerError).getWorkspaceDetails(7).single()
        }
        verify(dao, never()).updateOverrideConflicts(anyInt(), anyBoolean())
    }

    @Test fun `refreshing the workspace list keeps each workspace's stored mode`() = runTest {
        // the list endpoint has no overrideConflicts; workspace 7 was earlier opened in OVERRIDE mode
        on(dao.getAll()).thenReturn(listOf(
            Workspace(id = 7, title = "Seven", type = "osw", overrideConflicts = true),
            Workspace(id = 8, title = "Eight", type = "osw", overrideConflicts = false),
        ))
        val listJson = """[
            {"id":7,"title":"Seven","type":"osw","externalAppAccess":1},
            {"id":8,"title":"Eight","type":"osw","externalAppAccess":1},
            {"id":9,"title":"Nine (new)","type":"osw","externalAppAccess":1}
        ]"""
        val location = mock<Location>()

        repository(listJson).getWorkspaces(location).single()

        val stored = argumentCaptor<List<Workspace>>()
        verify(dao).put(capture(stored))
        assertEquals(
            mapOf(7 to true, 8 to false, 9 to false),
            stored.value.associate { it.id to it.overrideConflicts }
        )
    }
}
