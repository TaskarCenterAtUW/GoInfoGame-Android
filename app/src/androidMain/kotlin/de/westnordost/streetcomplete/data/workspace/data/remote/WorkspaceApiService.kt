package de.westnordost.streetcomplete.data.workspace.data.remote

import android.location.Location
import de.westnordost.streetcomplete.data.preferences.EnvironmentManager
import de.westnordost.streetcomplete.data.preferences.Preferences
import de.westnordost.streetcomplete.data.user.WorkspaceConfigProvider
import de.westnordost.streetcomplete.data.workspace.UserProjectGroupItem
import de.westnordost.streetcomplete.data.workspace.Workspace
import de.westnordost.streetcomplete.data.workspace.domain.model.AppUpdateCheckerResponse
import de.westnordost.streetcomplete.data.workspace.domain.model.LoginResponse
import de.westnordost.streetcomplete.data.workspace.domain.model.UserInfoResponse
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.WorkspaceDetailsResponse
import de.westnordost.streetcomplete.util.firebase.performHttpCallWithFirebaseTracing
import de.westnordost.streetcomplete.util.network.retryOnTransientHttpFailure
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.nio.channels.UnresolvedAddressException
import kotlin.time.Duration.Companion.hours
import kotlin.time.DurationUnit

class WorkspaceApiService(
    private val httpClient: HttpClient,
    private val preferences: Preferences,
    private val environmentManager: EnvironmentManager,
    private val workspaceConfigProvider: WorkspaceConfigProvider,
    private val osmClient: HttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true }

    // maps a non-success HTTP status into a message meaningful to the end user, instead of
    // showing raw response bodies or letting a failed body-parse produce a confusing
    // SerializationException-derived message. Kept feature-local to this service rather than
    // reusing the shared wrapApiClientExceptions (data/ApiClientExceptions.kt), since that relies
    // on Ktor's expectSuccess throwing ClientRequestException/ServerResponseException, which this
    // client doesn't have enabled - status checking here has to stay manual either way.
    private fun httpErrorMessage(status: HttpStatusCode): String = when {
        status == HttpStatusCode.Unauthorized -> "Your session has expired. Please log in again."
        status == HttpStatusCode.Forbidden -> "You don't have permission to perform this action."
        status == HttpStatusCode.NotFound -> "The requested information could not be found."
        status == HttpStatusCode.RequestTimeout -> "The request timed out. Please check your connection and try again."
        status == HttpStatusCode.TooManyRequests -> "Too many requests. Please wait a moment and try again."
        status.value in 500..599 -> "The server is temporarily unavailable. Please try again later."
        status.value in 400..499 -> "The request could not be completed. Please try again."
        else -> "Something went wrong (error ${status.value}). Please try again."
    }

    // maps a caught network/parsing exception into a message meaningful to the end user. There's
    // no HttpTimeout plugin installed on this client, so timeouts/connection resets from the CIO
    // engine surface as raw java.io.IOException subtypes rather than a Ktor-specific exception.
    private fun Throwable.toWorkspaceErrorMessage(): String = when (this) {
        is UnresolvedAddressException -> "Please check your internet connection and try again."
        is java.net.SocketTimeoutException -> "The server took too long to respond. Please try again."
        is SerializationException -> "Received an unexpected response from the server. Please contact your workspace admin if this continues."
        is java.io.IOException -> "Please check your internet connection and try again."
        else -> message?.takeIf { it.isNotBlank() } ?: "Something went wrong. Please try again."
    }

    @Serializable
    class User(val username: String, val password: String)

    suspend fun getUserProjectGroups(): List<UserProjectGroupItem> {
        val url = "${environmentManager.currentEnvironment.tdeiBaseUrl}/project-group-roles/${workspaceConfigProvider.userId}"
        try {
            val response = performHttpCallWithFirebaseTracing(
                client = httpClient,
                url = url,
                method = HttpMethod.Get
            ) {

                get(url) {
                    workspaceConfigProvider.workspaceToken?.let { bearerAuth(it) }
                    parameter("page_size", 100)
                    parameter("page_no", 1)
                }
            }

            if (!response.status.isSuccess()) {
                throw Exception(httpErrorMessage(response.status))
            }
            val responseBody = response.body<List<UserProjectGroupItem>>()
            return responseBody
        } catch (e: Exception) {
            throw Exception(e.toWorkspaceErrorMessage())
        }
    }

    suspend fun getWorkspaces(location: Location): List<Workspace> {
        val url = "${environmentManager.currentEnvironment.workspaceBaseUrl}/mine"

        try {
            val response = performHttpCallWithFirebaseTracing(
                client = httpClient,
                url = url,
                method = HttpMethod.Get
            ) {

                get(url) {
                    workspaceConfigProvider.workspaceToken?.let { bearerAuth(it) }
                    parameter("lat", location.latitude)
                    parameter("lon", location.longitude)
                    parameter("radius", 20000)
                    parameter("gig_only", true)
                }
            }

            if (!response.status.isSuccess()) {
                throw Exception(httpErrorMessage(response.status))
            }
            val responseBody = response.body<List<Workspace>>()
            return responseBody
        } catch (e: Exception) {
            throw Exception(e.toWorkspaceErrorMessage())
        }
    }

    suspend fun getTDEIUserDetails(emailId: String): UserInfoResponse {
        val url = environmentManager.currentEnvironment.tdeiBaseUrl + "/user-profile"

        try {
            val response = performHttpCallWithFirebaseTracing(
                client = httpClient,
                url = url,
                method = HttpMethod.Get
            ) {
                get(url) {
                    workspaceConfigProvider.workspaceToken?.let { bearerAuth(it) }
                    parameter("user_name", emailId)
                }
            }
            // every field in UserInfoResponse is nullable, so a non-OK response (e.g. 404 when
            // the profile isn't found) that happens to be a JSON object still deserializes
            // "successfully" into an all-null UserInfoResponse instead of throwing - silently
            // writing null workspaceUserId/workspaceUserName with no visible error. Must check
            // status explicitly to catch that case.
            if (response.status != HttpStatusCode.OK) {
                val message = if (response.status == HttpStatusCode.NotFound) {
                    "User profile not found."
                } else {
                    httpErrorMessage(response.status)
                }
                throw Exception(message)
            }
            return response.body<UserInfoResponse>()
        } catch (e: Exception) {
            throw Exception(e.toWorkspaceErrorMessage())
        }
    }

    suspend fun getWorkspaceDetails(workspaceId: Int): WorkspaceDetailsResponse {
        val url = "${environmentManager.currentEnvironment.workspaceBaseUrl}/${workspaceId}"

        try {
            val response = performHttpCallWithFirebaseTracing(
                client = httpClient,
                url = url,
                method = HttpMethod.Get
            ) {

                get(url) {
                    workspaceConfigProvider.workspaceToken?.let { bearerAuth(it) }
                }
            }

            if (response.status == HttpStatusCode.NotFound) {
                throw Exception("Failed. Workspace not found with ID : $workspaceId")
            } else if (!response.status.isSuccess()) {
                throw Exception(httpErrorMessage(response.status))
            }

            return response.body<WorkspaceDetailsResponse>()
        } catch (e: SerializationException) {
            throw Exception("Workspace is not configured properly. Please contact the Admin for the workspace")
        } catch (e: Exception) {
            throw Exception(e.toWorkspaceErrorMessage())
        }
    }

    suspend fun loginToWorkspace(username: String, password: String): LoginResponse {
        val url = environmentManager.currentEnvironment.tdeiBaseUrl + "/authenticate"
        try {
            val response = performHttpCallWithFirebaseTracing(
                client = httpClient,
                url = url,
                method = HttpMethod.Get
            ) {
                post(url) {
                    val user = User(username.trim(), password.trim())
                    setBody(user)
                    contentType(ContentType.Application.Json)
                }
            }

            if (response.status == HttpStatusCode.OK) {
                val loginResponse = response.body<LoginResponse>()
                updateTokens(loginResponse.access_token, loginResponse.refresh_token)
                return loginResponse
            } else if (response.status == HttpStatusCode.Unauthorized || response.status == HttpStatusCode.Forbidden) {
                throw Exception("Invalid username or password.")
            } else {
                throw Exception(httpErrorMessage(response.status))
            }

            // if OSM server does not return valid JSON, it is the server's fault, hence
        } catch (e: Exception) {
            throw Exception(e.toWorkspaceErrorMessage())
        }
    }

    private fun updateTokens(accessToken: String, refreshToken: String) {
        preferences.workspaceToken = accessToken
        preferences.workspaceRefreshToken = refreshToken

        // Ktor's Auth{bearer{}} plugin (ApplicationModule.kt) caches whatever loadTokens{} first
        // returned for each HttpClient's whole lifetime - writing new tokens to Preferences above
        // does NOT invalidate that cache, so every subsequent request (even ones that also set
        // bearerAuth() manually per-request) keeps silently reusing the stale cached token until
        // this is cleared. Confirmed via logcat: the request right after a fresh login carried the
        // OLD token's JWT (different `iss`), causing a 401 - clearing here forces the next request
        // needing auth to call loadTokens{} again and pick up what was just written above. Clearing
        // both clients (not just httpClient) means this also cleans up after any prior logout/forced
        // logout/environment switch that left a stale cache behind, the moment a new login succeeds.
        clearCachedAuthTokens()
    }

    // called whenever the user switches environment (dev dropdown or a login deep link's ?env=)
    // before logging in - Ktor's Auth{bearer{}} plugin caches whatever loadTokens{} first
    // returned for each HttpClient's whole process lifetime (see updateTokens() above), so without
    // this a token obtained under the old environment keeps being sent to the new environment's
    // servers, which will always reject it with a 401. The caller is responsible for also
    // clearing preferences.workspaceToken/workspaceRefreshToken - this only clears the in-memory
    // Ktor-side cache, which is the one piece of state that lives in this class (it owns the
    // HttpClient instances).
    fun clearCachedAuthTokens() {
        httpClient.authProvider<BearerAuthProvider>()?.clearToken()
        osmClient.authProvider<BearerAuthProvider>()?.clearToken()
    }

    suspend fun refreshToken(refreshToken: String): LoginResponse {
        val url = environmentManager.currentEnvironment.tdeiBaseUrl + "/refresh-token"

        // a transient failure here (network blip, backend 5xx, rate limit) must not be
        // treated the same as an actually invalid/expired refresh token - retryOnTransientHttpFailure
        // smooths over short blips, but once it gives up the exception (e.g. UnresolvedAddressException
        // when the device has no connectivity at all) must propagate as-is rather than being flattened
        // into a generic Exception below - WorkspaceViewModel needs the real type to tell "can't reach
        // the server" apart from WorkspaceAuthRejectedException ("server rejected the refresh token"),
        // since only the latter should force a logout.
        val response = retryOnTransientHttpFailure {
            performHttpCallWithFirebaseTracing(
                client = httpClient,
                url = url,
                method = HttpMethod.Get
            ) {
                post(url) {
                    // deliberately no bearerAuth here - this call fires precisely when the access
                    // token is expired/near-expiry, so attaching it as Authorization risks the
                    // server rejecting the request before it even looks at the refresh token. The
                    // reactive refresh path (refreshJwtToken() in ApplicationModule.kt) hits the
                    // same endpoint the same way, unauthenticated.
                    //
                    // the API takes the refresh token as the "refresh_token" header, not the body
                    // (confirmed against the API's own curl example) - body must stay empty.
                    header("refresh_token", refreshToken)
                }
            }
        }
        if (response.status == HttpStatusCode.OK) {
            val loginResponse = response.body<LoginResponse>()
            // same stale-token-cache issue as loginToWorkspace() - persist + clear here too,
            // not just after the ViewModel's own redundant preferences write
            updateTokens(loginResponse.access_token, loginResponse.refresh_token)
            return loginResponse
        } else {
            // the server actively responded that the refresh token is no longer valid - this is
            // the only case that legitimately means "session expired, log out"
            throw WorkspaceAuthRejectedException(httpErrorMessage(response.status))
        }
    }

    suspend fun getForceUpdateInfo(): AppUpdateCheckerResponse {
        val url = environmentManager.currentEnvironment.appUpdateVersionCheckUrl

        try {
            val lastFetched = preferences.configLastFetchTime
            lastFetched?.let {
                if (System.currentTimeMillis() - it < 6.hours.toLong(DurationUnit.MILLISECONDS)) {
                    val cachedConfig = preferences.configJson
                    cachedConfig?.let { configString ->
                        return json.decodeFromString<AppUpdateCheckerResponse>(configString)
                    }
                }
            }
            val response = performHttpCallWithFirebaseTracing(
                client = httpClient,
                url = url,
                method = HttpMethod.Get
            ) {
                get(url)
            }
            if (!response.status.isSuccess()) {
                throw Exception(httpErrorMessage(response.status))
            }
            if (response.status == HttpStatusCode.OK) {
                preferences.configLastFetchTime = System.currentTimeMillis()
                preferences.configJson = response.bodyAsText()
            }

            return json.decodeFromString<AppUpdateCheckerResponse>(response.bodyAsText())
        } catch (e: Exception) {
            throw Exception(e.toWorkspaceErrorMessage())
        }
    }
}

// distinct from a network/IO failure - only thrown when the server actively responded that the
// refresh token itself is invalid/expired, which is the one case that should force a logout
class WorkspaceAuthRejectedException(message: String) : Exception(message)
