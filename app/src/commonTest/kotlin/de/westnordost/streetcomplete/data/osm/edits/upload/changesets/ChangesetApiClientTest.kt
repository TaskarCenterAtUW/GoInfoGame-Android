package de.westnordost.streetcomplete.data.osm.edits.upload.changesets

import de.westnordost.streetcomplete.data.AuthorizationException
import de.westnordost.streetcomplete.data.ConflictException
import de.westnordost.streetcomplete.data.user.WorkspaceConfigProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.respondOk
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

// no real server involved (public or private) - MockEngine controls the response status, same
// pattern as MapDataApiClientTest
class ChangesetApiClientTest {

    @Test fun `open throws exception on insufficient privileges`(): Unit = runBlocking {
        assertFailsWith<AuthorizationException> {
            client(MockEngine { respondError(HttpStatusCode.Unauthorized) }, token = null).open(mapOf())
        }
        assertFailsWith<AuthorizationException> {
            client(MockEngine { respondError(HttpStatusCode.Forbidden) }).open(mapOf())
        }
    }

    @Test fun `open and close works without error`(): Unit = runBlocking {
        val openClient = client(MockEngine { respondOk("1") })
        assertEquals(1L, openClient.open(mapOf("testKey" to "testValue")))

        val closeClient = client(MockEngine { respondOk() })
        closeClient.close(1L)
    }

    @Test fun `close on an already-closed changeset throws ConflictException`(): Unit = runBlocking {
        assertFailsWith<ConflictException> {
            client(MockEngine { respondError(HttpStatusCode.Conflict) }).close(1L)
        }
    }

    @Test fun `close throws exception on insufficient privileges`(): Unit = runBlocking {
        assertFailsWith<AuthorizationException> {
            client(MockEngine { respondError(HttpStatusCode.Unauthorized) }, token = null).close(1)
        }
        assertFailsWith<AuthorizationException> {
            client(MockEngine { respondError(HttpStatusCode.Forbidden) }).close(1)
        }
    }

    private fun client(engine: MockEngine, token: String? = "token") =
        ChangesetApiClient(
            httpClient = HttpClient(engine),
            workspaceConfigProvider = object : WorkspaceConfigProvider {
                override val osmBaseUrl: String
                    get() = "https://example.com/api/0.6/"
                override val workspaceId: Int
                    get() = 1
                override val workspaceToken: String?
                    get() = token
                override val userId: String?
                    get() = null
            },
            serializer = ChangesetApiSerializer()
        )
}
