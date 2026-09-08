package de.westnordost.streetcomplete.data.osmnotes

import de.westnordost.streetcomplete.data.AuthorizationException
import de.westnordost.streetcomplete.data.ConflictException
import de.westnordost.streetcomplete.data.QueryTooBigException
import de.westnordost.streetcomplete.data.osm.mapdata.BoundingBox
import de.westnordost.streetcomplete.data.osm.mapdata.LatLon
import de.westnordost.streetcomplete.data.user.UserAccessTokenSource
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

// no real server involved (public or private) - MockEngine controls the response, same pattern
// as MapDataApiClientTest/ChangesetApiClientTest. NotesApiParser already has its own dedicated
// XML round-trip tests (NotesApiParserTest), so this only needs to exercise NotesApiClient's own
// request/response-status handling.
class NotesApiClientTest {

    private val oneCommentNoteXml = """<osm><note lon="9.0" lat="83.0">
        <id>1</id>
        <date_created>2024-06-06 12:47:50 UTC</date_created>
        <status>open</status>
        <comments><comment>
            <date>2024-06-06 12:47:50 UTC</date>
            <uid>1</uid>
            <user>westnordost</user>
            <action>opened</action>
            <text>Created note!</text>
        </comment></comments>
    </note></osm>"""

    private val twoCommentNoteXml = """<osm><note lon="9.0" lat="83.0">
        <id>1</id>
        <date_created>2024-06-06 12:47:50 UTC</date_created>
        <status>open</status>
        <comments>
            <comment>
                <date>2024-06-06 12:47:50 UTC</date>
                <uid>1</uid>
                <user>westnordost</user>
                <action>opened</action>
                <text>Created note for comment!</text>
            </comment>
            <comment>
                <date>2024-06-06 12:47:51 UTC</date>
                <uid>1</uid>
                <user>westnordost</user>
                <action>commented</action>
                <text>First comment!</text>
            </comment>
        </comments>
    </note></osm>"""

    @Test fun `create note`(): Unit = runBlocking {
        val note = client(MockEngine { respondOk(oneCommentNoteXml) }).create(LatLon(83.0, 9.0), "Created note!")

        assertEquals(LatLon(83.0, 9.0), note.position)
        assertEquals(Note.Status.OPEN, note.status)
        assertEquals(1, note.comments.size)

        val comment = note.comments.first()
        assertEquals("Created note!", comment.text)
        assertEquals(NoteComment.Action.OPENED, comment.action)
        assertEquals("westnordost", comment.user?.displayName)
    }

    @Test fun `comment note`(): Unit = runBlocking {
        val note = client(MockEngine { respondOk(twoCommentNoteXml) }).comment(1, "First comment!")

        assertEquals(2, note.comments.size)
        assertEquals("Created note for comment!", note.comments[0].text)
        assertEquals(NoteComment.Action.OPENED, note.comments[0].action)
        assertEquals("westnordost", note.comments[0].user?.displayName)

        assertEquals("First comment!", note.comments[1].text)
        assertEquals(NoteComment.Action.COMMENTED, note.comments[1].action)
        assertEquals("westnordost", note.comments[1].user?.displayName)
    }

    @Test fun `comment note fails when not logged in`(): Unit = runBlocking {
        assertFailsWith<AuthorizationException> {
            client(MockEngine { respondError(HttpStatusCode.Unauthorized) }, token = null).comment(1, "test")
        }
    }

    @Test fun `comment note fails when not authorized`(): Unit = runBlocking {
        assertFailsWith<AuthorizationException> {
            client(MockEngine { respondError(HttpStatusCode.Forbidden) }).comment(1, "test")
        }
    }

    @Test fun `comment note fails when already closed`(): Unit = runBlocking {
        assertFailsWith<ConflictException> {
            client(MockEngine { respondError(HttpStatusCode.Conflict) }).comment(1, "test")
        }
    }

    @Test fun `get note`(): Unit = runBlocking {
        val note = client(MockEngine { respondOk(oneCommentNoteXml) }).get(1)
        assertEquals(1L, note?.id)
    }

    @Test fun `get no note`(): Unit = runBlocking {
        assertNull(client(MockEngine { respondError(HttpStatusCode.NotFound) }).get(0))
    }

    @Test fun `get notes`(): Unit = runBlocking {
        val notes = client(MockEngine { respondOk(twoCommentNoteXml) }).getAllOpen(BoundingBox(83.0, 9.3, 83.2, 9.5))
        assertTrue(notes.isNotEmpty())
    }

    @Test fun `get notes fails when bbox crosses 180th meridian`(): Unit = runBlocking {
        // client-side check, thrown before any request is made
        val client = client(MockEngine { respondError(HttpStatusCode.InternalServerError) })
        assertFailsWith<IllegalArgumentException> {
            client.getAllOpen(BoundingBox(0.0, 179.0, 0.1, -179.0))
        }
    }

    @Test fun `get notes fails when limit is too large`(): Unit = runBlocking {
        val client = client(MockEngine { respondError(HttpStatusCode.BadRequest) })
        assertFailsWith<QueryTooBigException> {
            client.getAllOpen(BoundingBox(0.0, 0.0, 0.1, 0.1), 100000000)
        }
        assertFailsWith<QueryTooBigException> {
            client.getAllOpen(BoundingBox(0.0, 0.0, 90.0, 90.0))
        }
    }

    private fun client(engine: MockEngine, token: String? = "token") =
        NotesApiClient(
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
            userAccessTokenSource = object : UserAccessTokenSource { override val accessToken = token.orEmpty() },
            notesApiParser = NotesApiParser()
        )
}
