package de.westnordost.streetcomplete.data.workspace.data.remote

import de.westnordost.streetcomplete.data.workspace.domain.model.LoginResponse
import de.westnordost.streetcomplete.data.workspace.domain.model.Workspace
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.AddLongFormResponseItem
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class WorkspaceApiService(private val httpClient: HttpClient) {
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    class User(val username: String, val password: String)

    suspend fun getWorkspaces(): List<Workspace> {
        try {
            val response =
                httpClient.get("https://api.workspaces-dev.sidewalks.washington.edu/api/v1/workspaces/mine")
            val responseBody = response.body<List<Workspace>>()
            return responseBody

            // if OSM server does not return valid JSON, it is the server's fault, hence
        } catch (e: Exception) {
            throw Exception(e.message)
        }
    }

    suspend fun getLongFormForWorkspace(workspaceId: Int): List<AddLongFormResponseItem> {
        try {
            val response =
                httpClient.get("https://api.workspaces-dev.sidewalks.washington.edu/api/v1/workspaces/${workspaceId}/quests/long")
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
                httpClient.post("https://tdei-usermanagement-be-dev.azurewebsites.net/api/v1/authenticate") {
                    val user = User(username, password)
                    setBody(user)
                    contentType(ContentType.Application.Json)
                }

            val loginResponse = json.decodeFromString<LoginResponse>(response.body())
            return loginResponse

            // if OSM server does not return valid JSON, it is the server's fault, hence
        } catch (e: Exception) {
            throw Exception(e.message)
        }
    }
}
