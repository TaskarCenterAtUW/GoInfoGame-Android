package de.westnordost.streetcomplete.util.network

import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

private fun HttpResponse.isTransientFailure(): Boolean =
    status.value in 500..599 || status == HttpStatusCode.TooManyRequests

/**
 * Runs [block] (a single HTTP call), retrying with exponential backoff (500ms, then 1s) on
 * failures that are likely transient - network exceptions/timeouts, 5xx, 429 - but not on other
 * 4xx responses, since those mean the server has definitively rejected the request (e.g. an
 * actually expired/revoked refresh token) and retrying would just delay the same rejection.
 */
suspend fun retryOnTransientHttpFailure(
    maxAttempts: Int = 3,
    initialDelayMillis: Long = 500,
    block: suspend () -> HttpResponse,
): HttpResponse {
    var attempt = 1
    while (true) {
        val response = try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (attempt >= maxAttempts) throw e
            delay(initialDelayMillis * (1L shl (attempt - 1)))
            attempt++
            continue
        }
        if (!response.isTransientFailure() || attempt >= maxAttempts) return response
        delay(initialDelayMillis * (1L shl (attempt - 1)))
        attempt++
    }
}
