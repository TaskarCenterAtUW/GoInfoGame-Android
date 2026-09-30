package de.westnordost.streetcomplete.testutils

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.plugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.request
import io.ktor.client.request.takeFrom
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.io.IOException
import java.util.Collections
import java.util.WeakHashMap

/**
 * Keeps photo uploads away from the real KartaView: hooked into the app's own "kartaViewClient"
 * (same reason as [MockOsmServer.attachTo]). While [isActive] every request is answered here:
 * - [failure] null: like the real API when all goes well - sequence created, photo uploaded,
 *   sequence closed, photo looked up with [PHOTO_URL] as its public URL
 * - [Failure.Unreachable]: like an unreachable server (IOException)
 * - [Failure.Status]: with that HTTP error status
 */
object MockKartaView {

    const val PHOTO_URL = "https://storage.kartaview.org/files/photo/lth/mock-photo-1.jpg"

    sealed interface Failure {
        data object Unreachable : Failure
        data class Status(val code: HttpStatusCode) : Failure
    }

    @Volatile var isActive = false
    @Volatile var failure: Failure? = null
    val requests: MutableList<String> = Collections.synchronizedList(mutableListOf())

    private val attachedTo = Collections.newSetFromMap(WeakHashMap<HttpClient, Boolean>())

    // the app reads the body through this client's call, so it needs the same JSON support as
    // the app's own kartaViewClient
    private val server = HttpClient(MockEngine { request -> answer(request) }) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    fun attachTo(kartaViewClient: HttpClient) = synchronized(attachedTo) {
        if (!attachedTo.add(kartaViewClient)) return
        kartaViewClient.plugin(HttpSend).intercept { request ->
            if (!isActive) return@intercept execute(request)
            requests.add("${request.method.value} ${java.net.URI(request.url.buildString()).path}")
            if (failure == Failure.Unreachable) throw IOException("mock KartaView: unreachable")
            server.request(HttpRequestBuilder().takeFrom(request)).call
        }
    }

    fun reset() {
        requests.clear()
        failure = null
    }

    private fun MockRequestHandleScope.answer(request: HttpRequestData): HttpResponseData {
        (failure as? Failure.Status)?.let { return respond("mock KartaView failure", it.code) }
        val path = request.url.encodedPath
        return when {
            request.method == HttpMethod.Post && path.endsWith("/1.0/sequence/") ->
                json("""{"osv":{"sequence":{"id":"mock-sequence-1"}},"status":$STATUS}""")
            request.method == HttpMethod.Post && path.endsWith("/1.0/photo/") ->
                json("""{"osv":{"photo":{"id":"mock-photo-1","path":"mock","photoName":"mock.jpg"}},"status":$STATUS}""")
            request.method == HttpMethod.Post && path.endsWith("/1.0/sequence/finished-uploading/") ->
                json("""{"osv":{},"status":$STATUS}""")
            request.method == HttpMethod.Get && path.endsWith("/2.0/photo/mock-photo-1") ->
                json("""{"result":{"data":{"imageLthUrl":"$PHOTO_URL"}}}""")
            else -> respond("unexpected mock KartaView request", HttpStatusCode.NotFound)
        }
    }

    private fun MockRequestHandleScope.json(body: String) =
        respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))

    private const val STATUS = """{"apiCode":600,"apiMessage":"ok","httpCode":200,"httpMessage":"Success"}"""
}
