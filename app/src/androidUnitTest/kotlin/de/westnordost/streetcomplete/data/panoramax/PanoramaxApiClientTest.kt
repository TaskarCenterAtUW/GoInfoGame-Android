package de.westnordost.streetcomplete.data.panoramax

import com.russhwolf.settings.MapSettings
import de.westnordost.streetcomplete.data.osm.mapdata.LatLon
import de.westnordost.streetcomplete.data.preferences.Environment
import de.westnordost.streetcomplete.data.preferences.EnvironmentManager
import de.westnordost.streetcomplete.data.preferences.Preferences
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.nio.channels.UnresolvedAddressException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The upload flow against the Panoramax API (upload set: create, add files, complete), what is
 *  sent along with each picture, and that every way an upload can fail surfaces as
 *  [PanoramaxException] - that's what the edit and note uploaders treat as "photo failed, keep the
 *  edit for the next sync" (and count towards the stuck-photo notice); anything else aborts the
 *  whole upload run. */
class PanoramaxApiClientTest {

    private val base = Environment.PANORAMAX_DEV_URL
    private val position = LatLon(47.6553, -122.3035)

    /** "METHOD path" of each request, in order */
    private val requests = mutableListOf<String>()
    /** request bodies of the file uploads, in order */
    private val fileBodies = mutableListOf<String>()
    private var createBody: String? = null
    private var patchBody: String? = null

    private val preferences = Preferences(MapSettings())
    private lateinit var photo1: File
    private lateinit var photo2: File

    @BeforeTest fun setUp() {
        preferences.environment = Environment.DEV.name
        photo1 = File.createTempFile("photo1", ".jpg").apply { writeBytes(ByteArray(8) { 1 }) }
        photo2 = File.createTempFile("photo2", ".jpg").apply { writeBytes(ByteArray(8) { 2 }) }
    }

    @AfterTest fun tearDown() {
        photo1.delete()
        photo2.delete()
    }

    private fun client(answer: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData = { ok(it) }) =
        PanoramaxApiClient(
            SystemFileSystem,
            HttpClient(MockEngine { request ->
                requests.add("${request.method.value} ${request.url.encodedPath}")
                answer(request)
            }) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            },
            EnvironmentManager(preferences),
            accessToken = "token",
        )

    /** like the real API when all goes well, picture ids pic-1, pic-2, ... */
    private suspend fun MockRequestHandleScope.ok(request: HttpRequestData): HttpResponseData {
        val path = request.url.encodedPath
        return when {
            request.method == HttpMethod.Post && path == "/api/upload_sets" -> {
                createBody = request.body.toByteArray().decodeToString()
                json("""{"id":"set-1"}""")
            }
            request.method == HttpMethod.Post && path == "/api/upload_sets/set-1/files" -> {
                fileBodies.add(request.body.toByteArray().decodeToString())
                json("""{"picture_id":"pic-${fileBodies.size}","rejected":null}""", HttpStatusCode.Accepted)
            }
            request.method == HttpMethod.Post && path == "/api/upload_sets/set-1/complete" ->
                json("""{"id":"set-1"}""")
            request.method == HttpMethod.Patch && path.startsWith("/api/pictures/") -> {
                patchBody = request.body.toByteArray().decodeToString()
                json("""{}""")
            }
            else -> respond("unexpected", HttpStatusCode.NotFound)
        }
    }

    private fun MockRequestHandleScope.json(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    //region uploading

    @Test fun `uploads all photos into one upload set and returns their picture URLs`() = runTest {
        val urls = client().upload(
            listOf(PanoramaxPhoto(photo1.path, 90f), PanoramaxPhoto(photo2.path, 180f)),
            position
        )

        assertEquals(
            listOf("${base}api/pictures/pic-1/hd.jpg", "${base}api/pictures/pic-2/hd.jpg"),
            urls
        )
        assertEquals(
            listOf(
                "POST /api/upload_sets",
                "POST /api/upload_sets/set-1/files",
                "POST /api/upload_sets/set-1/files",
                "POST /api/upload_sets/set-1/complete",
            ),
            requests
        )
    }

    // all photos of an edit are at the same position - with the server's deduplication, it'd drop
    // all but the first when completing the upload set, after having given each a picture id
    @Test fun `upload set is created without deduplication and splitting`() = runTest {
        client().upload(listOf(PanoramaxPhoto(photo1.path), PanoramaxPhoto(photo2.path)), position)

        val body = createBody!!
        assertTrue(body.contains("""no_deduplication":true"""), body)
        assertTrue(body.contains("""no_split":true"""), body)
        assertTrue(body.contains("""estimated_nb_files":2"""), body)
    }

    @Test fun `position and bearing are sent along`() = runTest {
        client().upload(listOf(PanoramaxPhoto(photo1.path, 90.4f)), position)

        val body = fileBodies.single()
        assertTrue(body.hasField("override_latitude", "47.6553"))
        assertTrue(body.hasField("override_longitude", "-122.3035"))
        assertTrue(body.hasField("override_Exif.GPSInfo.GPSImgDirection", "90/1"))
        assertTrue(body.hasField("override_Exif.GPSInfo.GPSImgDirectionRef", "T"))
        assertTrue(Regex("name=\"?override_capture_time\"?\r\n").containsMatchIn(body))
    }

    @Test fun `negative bearing is sent as 0-359`() = runTest {
        client().upload(listOf(PanoramaxPhoto(photo1.path, -90f)), position)

        assertTrue(fileBodies.single().hasField("override_Exif.GPSInfo.GPSImgDirection", "270/1"))
    }

    // 0 would claim the photo faces north
    @Test fun `no bearing is sent if unknown`() = runTest {
        client().upload(listOf(PanoramaxPhoto(photo1.path, null)), position)

        assertFalse(fileBodies.single().contains("GPSImgDirection"))
    }

    @Test fun `workspace and the photo's own semantics are sent along`() = runTest {
        preferences.workspaceId = 7

        client().upload(
            listOf(PanoramaxPhoto(photo1.path, 0f, mapOf(
                "element_id" to "way/123", "question" to "Is it steep?", "quest_id" to "12"
            ))),
            position
        )

        val body = fileBodies.single()
        assertTrue(body.contains(
            """[{"key":"workspace_id","value":"7"},{"key":"element_id","value":"way/123"},""" +
                """{"key":"question","value":"Is it steep?"},{"key":"quest_id","value":"12"}]"""
        ), body)
    }

    // the pictures are public
    @Test fun `the user's email is never sent`() = runTest {
        preferences.workspaceId = 7
        preferences.workspaceUserEmail = "surveyor@example.org"

        client().upload(listOf(PanoramaxPhoto(photo1.path)), position)

        val body = fileBodies.single()
        assertTrue(body.contains("""[{"key":"workspace_id","value":"7"}]"""), body)
        assertFalse(body.contains("surveyor@example.org"))
        assertFalse(body.contains("user_email"))
    }

    @Test fun `photos whose file doesn't exist are skipped`() = runTest {
        val urls = client().upload(
            listOf(PanoramaxPhoto("/does/not/exist.jpg"), PanoramaxPhoto(photo1.path)),
            position
        )

        assertEquals(listOf("${base}api/pictures/pic-1/hd.jpg"), urls)
        assertEquals(1, fileBodies.size)
    }

    @Test fun `nothing to upload makes no request`() = runTest {
        assertEquals(emptyList(), client { error("no request expected") }.upload(emptyList(), position))
        assertEquals(
            emptyList(),
            client { error("no request expected") }.upload(listOf(PanoramaxPhoto("/does/not/exist.jpg")), position)
        )
        assertEquals(emptyList(), requests)
    }

    //endregion

    //region failing

    @Test fun `unreachable Panoramax is a PanoramaxException marked as unreachable`() = runTest {
        val e = assertFailsWith<PanoramaxException> {
            client { throw IOException("connection refused") }.upload(listOf(PanoramaxPhoto(photo1.path)), position)
        }
        assertTrue(e.isUnreachable)
    }

    @Test fun `DNS failure is a PanoramaxException`() = runTest {
        // not an IOException
        assertFailsWith<PanoramaxException> {
            client { throw UnresolvedAddressException() }.upload(listOf(PanoramaxPhoto(photo1.path)), position)
        }
    }

    // the edit uploader stops trying the other queued photos for the run on an unreachable
    // Panoramax, but not on an error status for one upload
    @Test fun `error status is a PanoramaxException not marked as unreachable`() = runTest {
        val e = assertFailsWith<PanoramaxException> {
            client { respond("down", HttpStatusCode.ServiceUnavailable) }.upload(listOf(PanoramaxPhoto(photo1.path)), position)
        }
        assertFalse(e.isUnreachable)
    }

    @Test fun `rejected picture is a PanoramaxException`() = runTest {
        val e = assertFailsWith<PanoramaxException> {
            client { request ->
                if (request.url.encodedPath.endsWith("/files")) {
                    json(
                        """{"picture_id":null,"rejected":{"reason":"invalid_metadata","severity":"error","message":"no position"}}""",
                        HttpStatusCode.Accepted
                    )
                } else ok(request)
            }.upload(listOf(PanoramaxPhoto(photo1.path)), position)
        }
        assertFalse(e.isUnreachable)
        assertTrue(e.message!!.contains("invalid_metadata"))
    }

    @Test fun `failing to complete the upload set is a PanoramaxException`() = runTest {
        assertFailsWith<PanoramaxException> {
            client { request ->
                if (request.url.encodedPath.endsWith("/complete")) {
                    respond("nope", HttpStatusCode.InternalServerError)
                } else ok(request)
            }.upload(listOf(PanoramaxPhoto(photo1.path)), position)
        }
    }

    //endregion

    //region adding semantics later

    @Test fun `adds semantics to an uploaded picture`() = runTest {
        assertTrue(client().addSemantics("${base}api/pictures/pic-9/hd.jpg", mapOf("element_id" to "node/5")))

        assertEquals(listOf("PATCH /api/pictures/pic-9"), requests)
        assertEquals("""{"semantics":[{"key":"element_id","value":"node/5"}]}""", patchBody)
    }

    // e.g. a photo uploaded to KartaView before the switch
    @Test fun `does nothing for a picture of somewhere else`() = runTest {
        assertFalse(client { error("no request expected") }.addSemantics(
            "https://storage.kartaview.org/files/photo/lth/1.jpg", mapOf("element_id" to "node/5")
        ))
        assertEquals(emptyList(), requests)
    }

    @Test fun `failing to add semantics is a PanoramaxException`() = runTest {
        assertFailsWith<PanoramaxException> {
            client { respond("nope", HttpStatusCode.Forbidden) }
                .addSemantics("${base}api/pictures/pic-9/hd.jpg", mapOf("element_id" to "node/5"))
        }
    }

    //endregion

    /** whether this multipart body has a form field [name] with [value] */
    private fun String.hasField(name: String, value: String): Boolean =
        Regex("name=\"?${Regex.escape(name)}\"?\r\n(?:[^\r\n]+\r\n)*\r\n${Regex.escape(value)}\r\n")
            .containsMatchIn(this)
}
