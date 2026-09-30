package de.westnordost.streetcomplete.testutils

import android.util.Xml
import de.westnordost.streetcomplete.ApplicationConstants
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.plugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.takeFrom
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import org.xmlpull.v1.XmlPullParser

/**
 * An in-memory OSM API 0.6 (the parts the app uses): map download, element fetches, and
 * changeset create / upload / close. It keeps element versions like the real API, so an upload
 * based on a stale version is rejected with 409 Conflict - which is what makes the app go through
 * its real conflict detection (re-fetch, compare, RESOLVE/OVERRIDE).
 *
 * Hooked into the app's own "osmClient" with [attachTo]: while [isActive], every request on it
 * is answered here instead of going out. It has to be that instance, not a replacement registered
 * in Koin - the app's upload/download singletons keep the client they were created with, and an
 * earlier test in the same process may already have created them (which once sent real requests
 * to the production OSM proxy). One instance per test process, reset per test with [reset].
 */
object MockOsmServer {

    @Volatile var isActive = false
    private val attachedTo = java.util.Collections.newSetFromMap(java.util.WeakHashMap<HttpClient, Boolean>())

    /** Routes [osmClient]'s requests here while [isActive]; passes them through otherwise. */
    fun attachTo(osmClient: HttpClient) = synchronized(attachedTo) {
        if (!attachedTo.add(osmClient)) return
        osmClient.plugin(HttpSend).intercept { request ->
            if (isActive) client.request(HttpRequestBuilder().takeFrom(request)).call else execute(request)
        }
    }

    data class MockNode(val id: Long, val lat: Double, val lon: Double, val version: Int = 1, val tags: Map<String, String> = emptyMap())
    /** [timestamp] is the way's OSM "last edited" time (ISO 8601) - what long-form recheck goes by. */
    data class MockWay(
        val id: Long,
        val nodeIds: List<Long>,
        val version: Int = 1,
        val tags: Map<String, String> = emptyMap(),
        val timestamp: String = TIMESTAMP,
    )

    data class Changeset(val id: Long, val tags: Map<String, String>, var isOpen: Boolean = true)
    /** One accepted `changeset/{id}/upload`: the modified ways as the app sent them. */
    data class Upload(val changesetId: Long, val modifiedWays: List<MockWay>)

    private val lock = Any()
    private val nodes = mutableMapOf<Long, MockNode>()
    private val ways = mutableMapOf<Long, MockWay>()
    private var nextChangesetId = 1L

    val changesets = mutableListOf<Changeset>()
    val uploads = mutableListOf<Upload>()
    /** uploads rejected with 409 because they were based on an outdated version */
    val rejectedUploads = mutableListOf<Long>()
    val requests = mutableListOf<String>()
    /** requests the mock has no answer for - anything here means the test setup is incomplete */
    val unexpectedRequests = mutableListOf<String>()

    val client: HttpClient = HttpClient(MockEngine { request -> synchronized(lock) { handle(request) } }) {
        defaultRequest { header(HttpHeaders.UserAgent, ApplicationConstants.USER_AGENT) }
        expectSuccess = false
    }

    fun reset() = synchronized(lock) {
        nodes.clear(); ways.clear()
        changesets.clear(); uploads.clear(); rejectedUploads.clear()
        requests.clear(); unexpectedRequests.clear()
        failure = null
        nextChangesetId = 1L
    }

    fun put(vararg elements: Any) = synchronized(lock) {
        for (e in elements) when (e) {
            is MockNode -> nodes[e.id] = e
            is MockWay -> ways[e.id] = e
        }
    }

    fun way(id: Long): MockWay? = synchronized(lock) { ways[id] }

    sealed interface Failure {
        /** the request never reaches a server (IOException, as with no connectivity) */
        data object Offline : Failure
        data class Status(val code: HttpStatusCode) : Failure {
            init {
                // the app's bearer-auth plugin answers a 401 by calling the REAL refresh-token
                // endpoint with its own HttpClient, which this mock can't intercept
                require(code != HttpStatusCode.Unauthorized) {
                    "401 would make the app call the real TDEI refresh endpoint - use 403 to test auth errors"
                }
            }
        }
    }

    /** Decides per request ("GET map", "POST changeset/1/upload", ...) whether it fails; null = answer normally. */
    @Volatile var failure: ((request: String) -> Failure?)? = null

    /** Someone else deleted the way. */
    fun deleteWay(id: Long) = synchronized(lock) { ways.remove(id) }

    /** Someone else changed which nodes the way consists of (e.g. extended it), version bumped. */
    fun changeWayNodes(id: Long, nodeIds: List<Long>) = synchronized(lock) {
        val way = ways.getValue(id)
        ways[id] = way.copy(version = way.version + 1, nodeIds = nodeIds)
    }

    /** Someone else edits the way on the server: new tags, version bumped. */
    fun editWayConcurrently(id: Long, tagChanges: Map<String, String?>) = synchronized(lock) {
        val way = ways.getValue(id)
        val tags = way.tags.toMutableMap()
        for ((k, v) in tagChanges) if (v == null) tags.remove(k) else tags[k] = v
        ways[id] = way.copy(version = way.version + 1, tags = tags)
    }

    fun snapshot(): Snapshot = synchronized(lock) {
        Snapshot(changesets.map { it.copy() }, uploads.toList(), rejectedUploads.toList(), requests.toList(), unexpectedRequests.toList())
    }

    data class Snapshot(
        val changesets: List<Changeset>,
        val uploads: List<Upload>,
        val rejectedUploads: List<Long>,
        val requests: List<String>,
        val unexpectedRequests: List<String>,
    )

    //region request handling

    private fun MockRequestHandleScope.handle(request: HttpRequestData): HttpResponseData {
        // everything after ".../api/0.6/"
        val path = request.url.encodedPath.substringAfter("/api/0.6/")
        val line = "${request.method.value} $path"
        requests.add(line)
        when (val f = failure?.invoke(line)) {
            Failure.Offline -> throw java.io.IOException("mock OSM: network down ()")
            is Failure.Status -> return respond("mock OSM failure", f.code)
            null -> {}
        }

        val segments = path.split('/')
        return when {
            request.method == HttpMethod.Get && path == "map" -> xml(osm(nodes.values, ways.values))

            request.method == HttpMethod.Get && segments.size == 2 && segments[0] == "node" ->
                nodes[segments[1].toLong()]?.let { xml(osm(listOf(it), emptyList())) } ?: notFound()
            request.method == HttpMethod.Get && segments.size == 2 && segments[0] == "way" ->
                ways[segments[1].toLong()]?.let { xml(osm(emptyList(), listOf(it))) } ?: notFound()
            request.method == HttpMethod.Get && segments.size == 3 && segments[0] == "way" && segments[2] == "full" ->
                ways[segments[1].toLong()]?.let { way -> xml(osm(way.nodeIds.mapNotNull { nodes[it] }, listOf(way))) } ?: notFound()
            request.method == HttpMethod.Get && segments.size == 3 && segments[2] in setOf("ways", "relations") ->
                xml(osm(emptyList(), emptyList()))

            request.method == HttpMethod.Put && path == "changeset/create" -> {
                val id = nextChangesetId++
                changesets.add(Changeset(id, parseChangesetTags(request.bodyText())))
                respond(id.toString(), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/plain"))
            }
            request.method == HttpMethod.Put && segments.size == 3 && segments[0] == "changeset" && segments[2] == "close" -> {
                val changeset = changesets.firstOrNull { it.id == segments[1].toLong() }
                when {
                    changeset == null -> notFound()
                    !changeset.isOpen -> respond("closed", HttpStatusCode.Conflict)
                    else -> { changeset.isOpen = false; respond("", HttpStatusCode.OK) }
                }
            }
            request.method == HttpMethod.Post && segments.size == 3 && segments[0] == "changeset" && segments[2] == "upload" ->
                upload(segments[1].toLong(), request.bodyText())

            else -> {
                unexpectedRequests.add(line)
                notFound()
            }
        }
    }

    private fun MockRequestHandleScope.upload(changesetId: Long, osmChange: String): HttpResponseData {
        val changeset = changesets.firstOrNull { it.id == changesetId }
        if (changeset == null || !changeset.isOpen) {
            return respond("The changeset $changesetId was closed", HttpStatusCode.Conflict)
        }
        val modified = parseModifiedWays(osmChange)
        for (way in modified) {
            val current = ways[way.id] ?: return respond("Way ${way.id} not found", HttpStatusCode.NotFound)
            if (current.version != way.version) {
                rejectedUploads.add(changesetId)
                return respond(
                    "Version mismatch: Provided ${way.version}, server had: ${current.version} of Way ${way.id}",
                    HttpStatusCode.Conflict
                )
            }
        }
        val diff = StringBuilder("<diffResult version=\"0.6\">")
        for (way in modified) {
            val newVersion = way.version + 1
            ways[way.id] = way.copy(version = newVersion)
            diff.append("<way old_id=\"${way.id}\" new_id=\"${way.id}\" new_version=\"$newVersion\"/>")
        }
        diff.append("</diffResult>")
        uploads.add(Upload(changesetId, modified))
        return xml(diff.toString())
    }

    private fun MockRequestHandleScope.xml(body: String) =
        respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/xml"))

    private fun MockRequestHandleScope.notFound() = respond("", HttpStatusCode.NotFound)

    private fun HttpRequestData.bodyText(): String = when (val body = body) {
        is OutgoingContent.ByteArrayContent -> body.bytes().decodeToString()
        else -> ""
    }

    //endregion

    //region XML

    private fun osm(nodes: Collection<MockNode>, ways: Collection<MockWay>): String {
        val sb = StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><osm version=\"0.6\">")
        if (nodes.isNotEmpty()) {
            sb.append("<bounds minlat=\"${nodes.minOf { it.lat }}\" minlon=\"${nodes.minOf { it.lon }}\" ")
            sb.append("maxlat=\"${nodes.maxOf { it.lat }}\" maxlon=\"${nodes.maxOf { it.lon }}\"/>")
        }
        for (n in nodes) {
            sb.append("<node id=\"${n.id}\" version=\"${n.version}\" timestamp=\"$TIMESTAMP\" lat=\"${n.lat}\" lon=\"${n.lon}\">")
            sb.appendTags(n.tags)
            sb.append("</node>")
        }
        for (w in ways) {
            sb.append("<way id=\"${w.id}\" version=\"${w.version}\" timestamp=\"${w.timestamp}\">")
            w.nodeIds.forEach { sb.append("<nd ref=\"$it\"/>") }
            sb.appendTags(w.tags)
            sb.append("</way>")
        }
        return sb.append("</osm>").toString()
    }

    private fun StringBuilder.appendTags(tags: Map<String, String>) {
        for ((k, v) in tags) append("<tag k=\"${k.xmlEscaped()}\" v=\"${v.xmlEscaped()}\"/>")
    }

    private fun String.xmlEscaped() =
        replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;")

    private fun parseChangesetTags(xml: String): Map<String, String> {
        val tags = mutableMapOf<String, String>()
        xml.parse { parser ->
            if (parser.name == "tag") tags[parser.getAttributeValue(null, "k")] = parser.getAttributeValue(null, "v")
        }
        return tags
    }

    private fun parseModifiedWays(osmChange: String): List<MockWay> {
        val result = mutableListOf<MockWay>()
        var inModify = false
        var id = 0L
        var version = 0
        var nodeIds = mutableListOf<Long>()
        var tags = mutableMapOf<String, String>()
        val parser = Xml.newPullParser()
        parser.setInput(osmChange.reader())
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "modify" -> inModify = true
                    "way" -> if (inModify) {
                        id = parser.getAttributeValue(null, "id").toLong()
                        version = parser.getAttributeValue(null, "version").toInt()
                        nodeIds = mutableListOf()
                        tags = mutableMapOf()
                    }
                    "nd" -> nodeIds.add(parser.getAttributeValue(null, "ref").toLong())
                    "tag" -> tags[parser.getAttributeValue(null, "k")] = parser.getAttributeValue(null, "v")
                }
                XmlPullParser.END_TAG -> when (parser.name) {
                    "modify" -> inModify = false
                    "way" -> if (inModify) result.add(MockWay(id, nodeIds, version, tags))
                }
            }
        }
        return result
    }

    private fun String.parse(onStartTag: (XmlPullParser) -> Unit) {
        val parser = Xml.newPullParser()
        parser.setInput(reader())
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG) onStartTag(parser)
        }
    }

    //endregion

    const val TIMESTAMP = "2025-01-01T00:00:00Z"
}
