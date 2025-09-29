package de.westnordost.streetcomplete.util.test_utils

import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.HttpMetric
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngine.Companion.invoke
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

suspend fun performHttpCallWithFirebaseTracingMock(
    client: HttpClient,
    url: String,
    method: HttpMethod = HttpMethod.Get,
    requestBlock: suspend HttpClient.() -> HttpResponse
): HttpResponse {

    val mockEngine = MockEngine { request ->
        respond(
            content = """{"access_token":"eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICItblZQcnZCMnQ5aDZsNzdYbXlEY3ZJN0NXaDMyNVROdnpaNGdhaE5RNFJVIn0.eyJleHAiOjE3NTkyMjE4MTMsImlhdCI6MTc1OTEzNTQxMywianRpIjoiMmU4YjYxMzktMGIzOS00MTdlLTlmYTMtNzdjYTAxNWEwOWY3IiwiaXNzIjoiaHR0cHM6Ly9hY2NvdW50LWRldi50ZGVpLnVzL3JlYWxtcy90ZGVpIiwic3ViIjoiOWNiOWQzZjYtNTNlNy00OGU4LWI2NjgtMjZiZmQxZWE1MWZlIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoidGRlaS1nYXRld2F5Iiwic2lkIjoiNzJiMTYzMmEtMDc1Yi00NzcyLTg3N2MtODY0ZjExZDYwN2IyIiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6WyIiLCIqIl0sInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJkZWZhdWx0LXJvbGVzLXRkZWkiXX0sInNjb3BlIjoiZW1haWwgb3BlbmlkIHByb2ZpbGUiLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwibmFtZSI6IlJhamVzaCBLIiwicHJlZmVycmVkX3VzZXJuYW1lIjoicmFqZXNoa0BnYXVzc2lhbnNvbHV0aW9ucy5jb20iLCJnaXZlbl9uYW1lIjoiUmFqZXNoIiwiZmFtaWx5X25hbWUiOiJLIiwiZW1haWwiOiJyYWplc2hrQGdhdXNzaWFuc29sdXRpb25zLmNvbSJ9.q65ORrWVJkf6bMzXL4sOnGH8jVxO9X0D0egsDYQzYfmgmJF3nRmZnO1f0yfs1vlhEXGx_9b_NwIbg-5obrn9mebEJegv-o6eZmwA9Rna67l2n2gagBpdMFsAUEpXc-X1pgS1Nf4NEoncWcK88jdnBgd6YnM8ERoeqBub7vHUe4UtPOFSiVceDvqWhOXUFqVBYzvNGZxBLz45bjLy30vn344MHGBaUUFVoVUwsrwqOkrWCdWloTc4D3GH6xy21H3U4ChY7R_mWd0XqlFn4Fya7m3XVrqNpLfOEWMele5-IfhMw0tjbwg2EW0Jt1tAe2u9mG2JA5251k6jw0mt4a27KA","expires_in":86400,"refresh_expires_in":259200,"refresh_token":"eyJhbGciOiJIUzUxMiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICI0YTQ3NTVmMy1hNTNhLTRjZGMtOWQ4OS0wYzU2OWZlOWUyNTUifQ.eyJleHAiOjE3NTkzOTQ2MTMsImlhdCI6MTc1OTEzNTQxMywianRpIjoiOGJmMWQwOWMtMzhiNy00N2ZlLTkxNzItOTExNDMzNmJiMjVjIiwiaXNzIjoiaHR0cHM6Ly9hY2NvdW50LWRldi50ZGVpLnVzL3JlYWxtcy90ZGVpIiwiYXVkIjoiaHR0cHM6Ly9hY2NvdW50LWRldi50ZGVpLnVzL3JlYWxtcy90ZGVpIiwic3ViIjoiOWNiOWQzZjYtNTNlNy00OGU4LWI2NjgtMjZiZmQxZWE1MWZlIiwidHlwIjoiUmVmcmVzaCIsImF6cCI6InRkZWktZ2F0ZXdheSIsInNpZCI6IjcyYjE2MzJhLTA3NWItNDc3Mi04NzdjLTg2NGYxMWQ2MDdiMiIsInNjb3BlIjoiZW1haWwgb3BlbmlkIHByb2ZpbGUgd2ViLW9yaWdpbnMgcm9sZXMgYWNyIGJhc2ljIiwicmV1c2VfaWQiOiI5MWFmZjgyNy01OWExLTQwYjctYWVhMC04OGYwYTE5NDI1N2EifQ.SU1nDQa-flJK4hI7rBlFbzGNIcGXpeH92H2iofHLugPhjmpACJQxzwLSrqpbUhpy706Jtaa6d6RoUkVGz0aHCw"}""",
            status = HttpStatusCode.OK,
            headers = headersOf("Content-Type", ContentType.Application.Json.toString())
        )
    }
    val clientNew = HttpClient(mockEngine){
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
    val metric: HttpMetric = FirebasePerformance.getInstance()
        .newHttpMetric(url, method.value)
    metric.start()

    return try {
        val response = clientNew.requestBlock()
        metric.setHttpResponseCode(200) // always 200 for mock
        metric.setResponseContentType(response.headers["Content-Type"])
        response
    } catch (e: Exception) {
        metric.setHttpResponseCode(500) // generic failure code
        throw e
    } finally {
        metric.stop()
    }
}
