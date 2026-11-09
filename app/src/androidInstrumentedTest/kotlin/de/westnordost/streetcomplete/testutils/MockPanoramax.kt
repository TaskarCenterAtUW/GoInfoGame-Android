package de.westnordost.streetcomplete.testutils

import de.westnordost.streetcomplete.data.preferences.EnvironmentManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
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
import org.koin.core.context.GlobalContext
import java.io.IOException
import java.util.Collections
import java.util.WeakHashMap

/**
 * Keeps photo uploads away from the real Panoramax: hooked into the app's own "panoramaxClient"
 * (same reason as [MockOsmServer.attachTo]). While [isActive] every request is answered here:
 * - [failure] null: like the real API when all goes well - upload set created, photo added as
 *   picture [PICTURE_ID] (public URL [PHOTO_URL]), upload set completed, semantics patched
 * - [Failure.Unreachable]: like an unreachable server (IOException)
 * - [Failure.Status]: with that HTTP error status
 */
object MockPanoramax {

    const val PICTURE_ID = "mock-picture-1"
    /** on the instance of the environment the app is on, see Environment.panoramaxBaseUrl */
    val PHOTO_URL: String
        get() = "${GlobalContext.get().get<EnvironmentManager>().currentEnvironment.panoramaxBaseUrl}" +
            "api/pictures/$PICTURE_ID/hd.jpg"

    sealed interface Failure {
        data object Unreachable : Failure
        data class Status(val code: HttpStatusCode) : Failure
    }

    @Volatile var isActive = false
    @Volatile var failure: Failure? = null
    /** "METHOD path" of each request, in order */
    val requests: MutableList<String> = Collections.synchronizedList(mutableListOf())
    /** the (multipart) bodies of the photo file uploads, in order */
    val fileBodies: MutableList<String> = Collections.synchronizedList(mutableListOf())

    private val attachedTo = Collections.newSetFromMap(WeakHashMap<HttpClient, Boolean>())

    // the app reads the body through this client's call, so it needs the same JSON support as
    // the app's own panoramaxClient
    private val server = HttpClient(MockEngine { request -> answer(request) }) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    fun attachTo(panoramaxClient: HttpClient) = synchronized(attachedTo) {
        if (!attachedTo.add(panoramaxClient)) return
        panoramaxClient.plugin(HttpSend).intercept { request ->
            if (!isActive) return@intercept execute(request)
            requests.add("${request.method.value} ${java.net.URI(request.url.buildString()).path}")
            if (failure == Failure.Unreachable) throw IOException("mock Panoramax: unreachable")
            server.request(HttpRequestBuilder().takeFrom(request)).call
        }
    }

    fun reset() {
        requests.clear()
        fileBodies.clear()
        failure = null
    }

    private suspend fun MockRequestHandleScope.answer(request: HttpRequestData): HttpResponseData {
        (failure as? Failure.Status)?.let { return respond("mock Panoramax failure", it.code) }
        val path = request.url.encodedPath
        return when {
            request.method == HttpMethod.Post && path.endsWith("/api/upload_sets") ->
                json("""{"id":"$UPLOAD_SET_ID"}""")
            request.method == HttpMethod.Post && path.endsWith("/api/upload_sets/$UPLOAD_SET_ID/files") -> {
                fileBodies.add(request.body.toByteArray().decodeToString())
                json("""{"picture_id":"$PICTURE_ID","rejected":null}""", HttpStatusCode.Accepted)
            }
            request.method == HttpMethod.Post && path.endsWith("/api/upload_sets/$UPLOAD_SET_ID/complete") ->
                json("""{"id":"$UPLOAD_SET_ID"}""")
            request.method == HttpMethod.Patch && path.endsWith("/api/pictures/$PICTURE_ID") ->
                json("""{"id":"$PICTURE_ID"}""")
            else -> respond("unexpected mock Panoramax request", HttpStatusCode.NotFound)
        }
    }

    private fun MockRequestHandleScope.json(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    private const val UPLOAD_SET_ID = "mock-upload-set-1"
}
