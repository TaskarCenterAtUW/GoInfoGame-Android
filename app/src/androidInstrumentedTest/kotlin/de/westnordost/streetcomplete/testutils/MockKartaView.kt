package de.westnordost.streetcomplete.testutils

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.request
import io.ktor.client.request.takeFrom
import io.ktor.http.HttpStatusCode
import java.io.IOException
import java.util.Collections
import java.util.WeakHashMap

/**
 * Keeps photo uploads away from the real KartaView: hooked into the app's own "kartaViewClient"
 * (same reason as [MockOsmServer.attachTo]). While [isActive] every request fails - either like
 * an unreachable server ([Failure.Unreachable]) or with an HTTP error status ([Failure.Status]),
 * which is what a stuck photo upload looks like to the app.
 */
object MockKartaView {

    sealed interface Failure {
        data object Unreachable : Failure
        data class Status(val code: HttpStatusCode) : Failure
    }

    @Volatile var isActive = false
    @Volatile var failure: Failure = Failure.Status(HttpStatusCode.ServiceUnavailable)
    val requests: MutableList<String> = Collections.synchronizedList(mutableListOf())

    private val attachedTo = Collections.newSetFromMap(WeakHashMap<HttpClient, Boolean>())

    private val failingClient = HttpClient(MockEngine {
        respond("mock KartaView failure", (failure as Failure.Status).code)
    })

    fun attachTo(kartaViewClient: HttpClient) = synchronized(attachedTo) {
        if (!attachedTo.add(kartaViewClient)) return
        kartaViewClient.plugin(HttpSend).intercept { request ->
            if (!isActive) return@intercept execute(request)
            requests.add("${request.method.value} ${request.url.buildString()}")
            when (failure) {
                Failure.Unreachable -> throw IOException("mock KartaView: unreachable")
                is Failure.Status -> failingClient.request(HttpRequestBuilder().takeFrom(request)).call
            }
        }
    }

    fun reset() {
        requests.clear()
        failure = Failure.Status(HttpStatusCode.ServiceUnavailable)
    }
}
