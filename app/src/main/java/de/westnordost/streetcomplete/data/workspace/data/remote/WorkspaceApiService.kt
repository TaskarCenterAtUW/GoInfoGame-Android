package de.westnordost.streetcomplete.data.workspace.data.remote

import de.westnordost.streetcomplete.data.preferences.Preferences
import de.westnordost.streetcomplete.data.workspace.domain.model.LoginResponse
import de.westnordost.streetcomplete.data.workspace.domain.model.UserInfoResponse
import de.westnordost.streetcomplete.data.workspace.domain.model.Workspace
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.AddLongFormResponseItem
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class WorkspaceApiService(private val httpClient: HttpClient,
    private val preferences: Preferences) {
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    class User(val username: String, val password: String)

    suspend fun getWorkspaces(): List<Workspace> {
        try {
            val response =
                httpClient.get("${EnvironmentManager(preferences).currentEnvironment.baseUrl}/mine")
            val responseBody = response.body<List<Workspace>>()
            return responseBody

            // if OSM server does not return valid JSON, it is the server's fault, hence
        } catch (e: Exception) {
            throw Exception(e.message)
        }
    }

    suspend fun getTDEIUserDetails(emailId : String): UserInfoResponse {
        try {
            val response =
                httpClient.get(EnvironmentManager(preferences).currentEnvironment.tdeiUrl){
                    parameter("user_name", emailId)
                    headers {
                        append("Authorization", "Bearer ${preferences.workspaceToken}")
                    }
                }
            return response.body<UserInfoResponse>()

            // if OSM server does not return valid JSON, it is the server's fault, hence
        } catch (e: Exception) {
            throw Exception(e.message)
        }
    }

    suspend fun getLongFormForWorkspace(workspaceId: Int): List<AddLongFormResponseItem> {
        try {
            val response =
                httpClient.get("${EnvironmentManager(preferences).currentEnvironment.baseUrl}/${workspaceId}/quests/long")
            val responseBody = response.body<List<AddLongFormResponseItem>>()
            return responseBody

            // if OSM server does not return valid JSON, it is the server's fault, hence
        } catch (e: Exception) {
            throw Exception(e.message)
        }
    }

    suspend fun loginToWorkspace(username: String, password: String): LoginResponse {
        try {
            val response =
                httpClient.post(EnvironmentManager(preferences).currentEnvironment.loginUrl) {
                    val user = User(username.trim(), password.trim())
                    setBody(user)
                    contentType(ContentType.Application.Json)
                }

            if (response.status == HttpStatusCode.OK){
                val loginResponse = response.body<LoginResponse>()
                return loginResponse
            }else{
                throw Exception("Login failed {${response.bodyAsText()}}")
            }


            // if OSM server does not return valid JSON, it is the server's fault, hence
        } catch (e: Exception) {
            throw Exception(e.message)
        }
    }
}
