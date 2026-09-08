package de.westnordost.streetcomplete.data.osm.mapdata

import de.westnordost.streetcomplete.ApplicationConstants
import de.westnordost.streetcomplete.data.AuthorizationException
import de.westnordost.streetcomplete.data.ConflictException
import de.westnordost.streetcomplete.data.QueryTooBigException
import de.westnordost.streetcomplete.data.user.UserAccessTokenSource
import de.westnordost.streetcomplete.data.user.WorkspaceConfigProvider
import de.westnordost.streetcomplete.testutils.node
import de.westnordost.streetcomplete.testutils.p
import de.westnordost.streetcomplete.testutils.way
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.respondOk
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * No real server involved (public or private) - MapDataApiParser/MapDataApiSerializer already
 * have their own dedicated XML round-trip tests (MapDataApiParserTest/MapDataApiSerializerTest),
 * so this only needs to exercise MapDataApiClient's own logic: request URLs, response-status ->
 * exception mapping, and null/empty handling on 404 - via MockEngine with minimal literal XML,
 * same pattern as OAuthApiClientTest/PhotoServiceApiClientTest.
 */
class MapDataApiClientTest {

    @Test fun getNode(): Unit = runBlocking {
        val client = client(respondingWith("/node/1", """<osm><node id="1" version="1" changeset="1" timestamp="2020-01-01T00:00:00Z" lat="1.0" lon="2.0"><tag k="name:en" v="Yangon"/></node></osm>"""))
        assertEquals("Yangon", client.getNode(1)?.tags?.get("name:en"))
        assertNull(client.getNode(0))
    }

    @Test fun getWay(): Unit = runBlocking {
        val client = client(respondingWith("/way/1", """<osm><way id="1" version="1" changeset="1" timestamp="2020-01-01T00:00:00Z"><tag k="name" v="Oderhafen"/></way></osm>"""))
        assertEquals("Oderhafen", client.getWay(1)?.tags?.get("name"))
        assertNull(client.getWay(0))
    }

    @Test fun getRelation(): Unit = runBlocking {
        val client = client(respondingWith("/relation/1", """<osm><relation id="1" version="1" changeset="1" timestamp="2020-01-01T00:00:00Z"><tag k="name" v="Hamburg"/></relation></osm>"""))
        assertEquals("Hamburg", client.getRelation(1)?.tags?.get("name"))
        assertNull(client.getRelation(0))
    }

    @Test fun getWaysForNode(): Unit = runBlocking {
        val client = client(respondingWith("/node/1/ways", "<osm>${waysOsm(1)}</osm>"))
        assertTrue(client.getWaysForNode(1).isNotEmpty())
        assertTrue(client.getWaysForNode(0).isEmpty())
    }

    @Test fun getRelationsForNode(): Unit = runBlocking {
        val client = client(respondingWith("/node/1/relations", "<osm>${relationsOsm(1)}</osm>"))
        assertTrue(client.getRelationsForNode(1).isNotEmpty())
        assertTrue(client.getRelationsForNode(0).isEmpty())
    }

    @Test fun getRelationsForWay(): Unit = runBlocking {
        val client = client(respondingWith("/way/1/relations", "<osm>${relationsOsm(1)}</osm>"))
        assertTrue(client.getRelationsForWay(1).isNotEmpty())
        assertTrue(client.getRelationsForWay(0).isEmpty())
    }

    @Test fun getRelationsForRelation(): Unit = runBlocking {
        val client = client(respondingWith("/relation/1/relations", "<osm>${relationsOsm(1)}</osm>"))
        assertTrue(client.getRelationsForRelation(1).isNotEmpty())
        assertTrue(client.getRelationsForRelation(0).isEmpty())
    }

    @Test fun getWayComplete(): Unit = runBlocking {
        val xml = """<osm>
            <node id="1" version="1" changeset="1" timestamp="2020-01-01T00:00:00Z" lat="1.0" lon="2.0" />
            <node id="2" version="1" changeset="1" timestamp="2020-01-01T00:00:00Z" lat="1.1" lon="2.1" />
            <way id="10" version="1" changeset="1" timestamp="2020-01-01T00:00:00Z">
                <nd ref="1" /><nd ref="2" />
            </way>
        </osm>"""
        val client = client(respondingWith("/way/10/full", xml))

        val data = client.getWayComplete(10)
        assertNotNull(data)
        assertTrue(data.nodes.isNotEmpty())
        assertTrue(data.ways.size == 1)

        assertNull(client.getWayComplete(0))
    }

    @Test fun getRelationComplete(): Unit = runBlocking {
        val xml = """<osm>
            <node id="1" version="1" changeset="1" timestamp="2020-01-01T00:00:00Z" lat="1.0" lon="2.0" />
            <way id="10" version="1" changeset="1" timestamp="2020-01-01T00:00:00Z"><nd ref="1" /></way>
            <relation id="100" version="1" changeset="1" timestamp="2020-01-01T00:00:00Z">
                <member type="way" ref="10" role="" />
                <tag k="name" v="Hamburg"/>
            </relation>
        </osm>"""
        val client = client(respondingWith("/relation/100/full", xml))

        val data = client.getRelationComplete(100)
        assertNotNull(data)
        assertTrue(data.nodes.isNotEmpty())
        assertTrue(data.ways.isNotEmpty())

        assertNull(client.getRelationComplete(0))
    }

    @Test fun getMap(): Unit = runBlocking {
        val client = client(respondingWith("/map", mapXml))
        val hamburg = client.getMap(HAMBURG_CITY_AREA)
        assertTrue(hamburg.nodes.isNotEmpty())
        assertTrue(hamburg.ways.isNotEmpty())
        assertTrue(hamburg.relations.isNotEmpty())
    }

    @Test fun `getMap does not return relations of ignored type`(): Unit = runBlocking {
        val xml = """<osm><relation id="1" version="1" changeset="1" timestamp="2020-01-01T00:00:00Z">
            <tag k="type" v="route"/>
        </relation></osm>"""
        val client = client(respondingWith("/map", xml))
        val hamburg = client.getMap(HAMBURG_CITY_AREA, ApplicationConstants::ignoreRelation)
        assertTrue(hamburg.relations.none { it.tags["type"] == "route" })
    }

    @Test fun `getMap fails when bbox crosses 180th meridian`(): Unit = runBlocking {
        // client-side check, thrown before any request is made - MockEngine that would fail the
        // test if it were ever called
        val client = client(MockEngine { respondError(HttpStatusCode.InternalServerError) })
        assertFailsWith<IllegalArgumentException> {
            client.getMap(BoundingBox(0.0, 179.9999999, 0.0000001, -179.9999999))
        }
    }

    @Test fun `getMap fails when bbox is too big`(): Unit = runBlocking {
        val client = client(MockEngine { respondError(HttpStatusCode.BadRequest) })
        assertFailsWith<QueryTooBigException> {
            client.getMap(BoundingBox(-90.0, -180.0, 90.0, 180.0))
        }
    }

    @Test fun `getMap returns bounding box that was specified in request`(): Unit = runBlocking {
        val client = client(respondingWith("/map", mapXml))
        val hamburg = client.getMap(HAMBURG_CITY_AREA)
        assertEquals(HAMBURG_CITY_AREA, hamburg.boundingBox)
    }

    @Test fun `uploadChanges as anonymous fails`(): Unit = runBlocking {
        assertFailsWith<AuthorizationException> {
            client(MockEngine { respondError(HttpStatusCode.Unauthorized) }, token = null)
                .uploadChanges(1L, MapDataChanges())
        }
    }

    @Test fun `uploadChanges without authorization fails`(): Unit = runBlocking {
        assertFailsWith<AuthorizationException> {
            client(MockEngine { respondError(HttpStatusCode.Forbidden) })
                .uploadChanges(1L, MapDataChanges())
        }
    }

    @Test fun `uploadChanges in already closed changeset fails`(): Unit = runBlocking {
        assertFailsWith<ConflictException> {
            client(MockEngine { respondError(HttpStatusCode.Conflict) })
                .uploadChanges(1L, MapDataChanges())
        }
    }

    @Test fun `uploadChanges of non-existing element fails`(): Unit = runBlocking {
        assertFailsWith<ConflictException> {
            client(MockEngine { respondError(HttpStatusCode.NotFound) })
                .uploadChanges(
                    changesetId = 1L,
                    changes = MapDataChanges(modifications = listOf(node(Long.MAX_VALUE)))
                )
        }
    }

    @Test fun uploadChanges(): Unit = runBlocking {
        // maps the 3 negative (not-yet-assigned) temporary ids used locally to the real ids/
        // versions the (fake) server assigned on upload - this id-remapping is the one genuinely
        // interesting piece of logic in this method, everything else is passthrough
        val diffResult = """<diffResult>
            <node old_id="-1" new_id="101" new_version="1" />
            <node old_id="-2" new_id="102" new_version="1" />
            <node old_id="-3" new_id="103" new_version="1" />
            <way old_id="-4" new_id="104" new_version="1" />
        </diffResult>"""
        val client = client(respondingWith("changeset/1/upload", diffResult))

        val updates = client.uploadChanges(
            changesetId = 1L,
            changes = MapDataChanges(
                creations = listOf(
                    node(-1, pos = p(15.0, -39.0), tags = mapOf("first" to "1")),
                    node(-2, pos = p(15.0, -39.1), tags = mapOf("second" to "2")),
                    node(-3, pos = p(15.0, -39.1), tags = mapOf("third" to "3")),
                    way(-4, nodes = listOf(-1, -2, -3)),
                )
            )
        )
        assertEquals(
            setOf(
                ElementKey(ElementType.NODE, -1),
                ElementKey(ElementType.NODE, -2),
                ElementKey(ElementType.NODE, -3),
                ElementKey(ElementType.WAY, -4),
            ),
            updates.idUpdates.map { ElementKey(it.elementType, it.oldElementId) }.toSet()
        )
        assertEquals(4, updates.updated.size)
        assertTrue(updates.updated.any { it.tags["first"] == "1" && it.id == 101L })
        assertTrue(updates.updated.filterIsInstance<Way>().single().id == 104L)
    }

    private val mapXml = """<?xml version="1.0" encoding="UTF-8"?>
        <osm>
        <bounds minlat="53.5790000" minlon="9.9390000" maxlat="53.5800000" maxlon="9.9400000"/>
        ${nodesOsm(1)}
        ${waysOsm(1)}
        ${relationsOsm(1)}
        </osm>
    """

    /** responds with [xml] for any request whose path ends with [foundPathSuffix], 404 otherwise -
     *  mirrors how the client itself distinguishes "found" from "doesn't exist" */
    private fun respondingWith(foundPathSuffix: String, xml: String): MockEngine = MockEngine { request ->
        if (request.url.encodedPath.endsWith(foundPathSuffix)) respondOk(xml)
        else respondError(HttpStatusCode.NotFound)
    }

    private fun client(engine: MockEngine, token: String? = "token") =
        MapDataApiClient(
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
            parser = MapDataApiParser(),
            serializer = MapDataApiSerializer()
        )

    private val HAMBURG_CITY_AREA = BoundingBox(53.579, 9.939, 53.580, 9.940)
}
