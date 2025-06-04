package de.westnordost.streetcomplete.data.workspace.data.remote

import android.location.Location
import de.westnordost.streetcomplete.data.preferences.Preferences
import de.westnordost.streetcomplete.data.workspace.domain.model.LoginResponse
import de.westnordost.streetcomplete.data.workspace.domain.model.UserInfoResponse
import de.westnordost.streetcomplete.data.workspace.domain.model.Workspace
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.Elements
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.LongFormResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.plugins.plugin
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import java.nio.channels.UnresolvedAddressException

class WorkspaceApiService(
    private val httpClient: HttpClient,
    private val preferences: Preferences,
    private val environmentManager: EnvironmentManager
) {
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    class User(val username: String, val password: String)

    suspend fun getWorkspaces(location: Location): List<Workspace> {
        try {
            val response =
                httpClient.get("${environmentManager.currentEnvironment.baseUrl}/mine") {
                    //Add query params
                    parameter("lat", location.latitude)
                    parameter("lon", location.longitude)
                    parameter("radius", 20000)
                    parameter("gig_only", true)
                }
            val responseBody = response.body<List<Workspace>>()
            return responseBody

            // if OSM server does not return valid JSON, it is the server's fault, hence
        } catch (e: UnresolvedAddressException) {
            throw Exception("Please check your internet connection")
        } catch (e: Exception) {
            throw Exception(e.message)
        }
    }

    suspend fun getTDEIUserDetails(emailId: String): UserInfoResponse {
        try {
            val response =
                httpClient.get(environmentManager.currentEnvironment.tdeiUrl) {
                    parameter("user_name", emailId)
//                    headers {
//                        append("Authorization", "Bearer ${preferences.workspaceToken}")
//                    }
                }
            return response.body<UserInfoResponse>()

            // if OSM server does not return valid JSON, it is the server's fault, hence
        } catch (e: Exception) {
            throw Exception(e.message)
        }
    }

    suspend fun getLongFormForWorkspace(workspaceId: Int): List<Elements> {
        try {
            val response =
                httpClient.get("${environmentManager.currentEnvironment.baseUrl}/${workspaceId}/quests/long")

            if (response.status == HttpStatusCode.NoContent) {
                throw Exception("Failed. Please configure long form for the workspace $workspaceId")
            }
            val text = response.bodyAsText()
            val jsonElement = Json.decodeFromString<JsonElement>(text)

            val json = Json {
                ignoreUnknownKeys = true
            }
            return when {

                jsonElement is JsonObject && "version" in jsonElement -> {
                    val wrapper = json.decodeFromJsonElement<LongFormResponse>(jsonElement)
                    wrapper.elements
                }

                jsonElement is JsonArray -> {
                    json.decodeFromJsonElement(jsonElement)
                }

                else -> {
                    throw SerializationException("Unexpected JSON structure for long form")
                }
            }
            // if OSM server does not return valid JSON, it is the server's fault, hence
        } catch (e: SerializationException) {
            throw Exception("Workspace is not configured properly. Please contact the Admin for the workspace")
        } catch (e: Exception) {
            throw Exception(e.message?.take(100))
        }
    }

    suspend fun loginToWorkspace(username: String, password: String): LoginResponse {
        try {
            val response =
                httpClient.post(environmentManager.currentEnvironment.loginUrl + "/authenticate") {
                    val user = User(username.trim(), password.trim())
                    setBody(user)
                    contentType(ContentType.Application.Json)
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

        // Force Ktor to use the new tokens immediately
        val authPlugin = httpClient.plugin(Auth)
        authPlugin.providers.filterIsInstance<BearerAuthProvider>().firstOrNull()?.clearToken()
    }

    suspend fun refreshToken(refreshToken: String): LoginResponse {
        try {
            val response =
                httpClient.post(environmentManager.currentEnvironment.loginUrl + "/refresh-token") {
                    setBody(refreshToken)
                    contentType(ContentType.Application.Json)
                }

            if (response.status == HttpStatusCode.OK) {
                val loginResponse = response.body<LoginResponse>()
                return loginResponse
            } else {
                throw Exception("Refresh token failed {${response.bodyAsText()}}")
            }

            // if OSM server does not return valid JSON, it is the server's fault, hence
        } catch (e: Exception) {
            throw Exception(e.message)
        }
    }
}
