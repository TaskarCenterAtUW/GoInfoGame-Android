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
) {
    private val json = Json { ignoreUnknownKeys = true }

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

            val responseBody = response.body<List<UserProjectGroupItem>>()
            return responseBody
        } catch (e: UnresolvedAddressException) {
            throw Exception("Please check your internet connection")
        } catch (e: Exception) {
            throw Exception(e.message)
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

            val responseBody = response.body<List<Workspace>>()
            return responseBody
        } catch (e: UnresolvedAddressException) {
            throw Exception("Please check your internet connection")
        } catch (e: Exception) {
            throw Exception(e.message)
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
            return response.body<UserInfoResponse>()
        } catch (e: Exception) {
            throw Exception(e.message)
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
            }

            return response.body<WorkspaceDetailsResponse>()
        } catch (e: SerializationException) {
            throw Exception("Workspace is not configured properly. Please contact the Admin for the workspace")
        } catch (e: Exception) {
            throw Exception(e.message?.take(100))
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
            } else {
                throw Exception("Login failed {${response.bodyAsText()}}")
            }

            // if OSM server does not return valid JSON, it is the server's fault, hence
        } catch (e: Exception) {
            throw Exception(e.message)
        }
    }

    private fun updateTokens(accessToken: String, refreshToken: String) {
        preferences.workspaceToken = accessToken
        preferences.workspaceRefreshToken = refreshToken

        // Ktor's Auth{bearer{}} plugin (ApplicationModule.kt) caches whatever loadTokens{} first
        // returned for this HttpClient's whole lifetime - writing new tokens to Preferences above
        // does NOT invalidate that cache, so every subsequent request (even ones that also set
        // bearerAuth() manually per-request) keeps silently reusing the stale cached token until
        // this is cleared. Confirmed via logcat: the request right after a fresh login carried the
        // OLD token's JWT (different `iss`), causing a 401 - clearing here forces the next request
        // needing auth to call loadTokens{} again and pick up what was just written above.
        httpClient.authProvider<BearerAuthProvider>()?.clearToken()
    }

    suspend fun refreshToken(refreshToken: String): LoginResponse {
        val url = environmentManager.currentEnvironment.tdeiBaseUrl + "/refresh-token"
        try {

            val response = performHttpCallWithFirebaseTracing(
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
            if (response.status == HttpStatusCode.OK) {
                val loginResponse = response.body<LoginResponse>()
                // same stale-token-cache issue as loginToWorkspace() - persist + clear here too,
                // not just after the ViewModel's own redundant preferences write
                updateTokens(loginResponse.access_token, loginResponse.refresh_token)
                return loginResponse
            } else {
                throw Exception("Refresh token failed {${response.bodyAsText()}}")
            }

            // if OSM server does not return valid JSON, it is the server's fault, hence
        } catch (e: Exception) {
            throw Exception(e.message)
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
            if (response.status == HttpStatusCode.OK) {
                preferences.configLastFetchTime = System.currentTimeMillis()
                preferences.configJson = response.bodyAsText()
            }

            return json.decodeFromString<AppUpdateCheckerResponse>(response.bodyAsText())
        } catch (e: Exception) {
            throw Exception(e.message)
        }
    }
}
