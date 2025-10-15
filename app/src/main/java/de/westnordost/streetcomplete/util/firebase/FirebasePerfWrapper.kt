package de.westnordost.streetcomplete.util.firebase

import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.HttpMetric
import io.ktor.client.HttpClient
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpMethod
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

suspend fun performHttpCallWithFirebaseTracing(
    client: HttpClient,
    url: String,
    method: HttpMethod = HttpMethod.Get,
    requestBlock: suspend HttpClient.() -> HttpResponse
): HttpResponse {
    val metric: HttpMetric = FirebasePerformance.getInstance()
        .newHttpMetric(url, method.value)
    metric.start()

    return try {
        val response = client.requestBlock()
        metric.setHttpResponseCode(response.status.value)
        metric.setResponseContentType(response.headers["Content-Type"])
        response
    } catch (e: Exception) {
        metric.setHttpResponseCode(500) // generic failure code
        throw e
    } finally {
        metric.stop()
    }
}

