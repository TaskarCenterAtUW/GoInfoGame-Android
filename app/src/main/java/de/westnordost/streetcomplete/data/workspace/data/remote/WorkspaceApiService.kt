package de.westnordost.streetcomplete.data.workspace.data.remote

import android.util.Log
import de.westnordost.streetcomplete.data.workspace.domain.model.LoginResponse
import de.westnordost.streetcomplete.data.workspace.domain.model.Workspace
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
    @Serializable class User(val username: String, val password: String)

    suspend fun getWorkspaces(): List<Workspace> {
        try {
            val response =
                httpClient.get("https://waylyticsosm.blob.core.windows.net/flows/workspaces-test/uwdataset.json")
            Log.d("UNIQUE", response.body())
            val workspaceResponse = json.decodeFromString<List<Workspace>>(response.body())
            return workspaceResponse

            // if OSM server does not return valid JSON, it is the server's fault, hence
        } catch (e: Exception) {
            throw Exception(e.message)
        }
    }

    suspend fun loginToWorkspace(username : String, password : String) : LoginResponse {
        try {
            val response =
                httpClient.post("https://tdei-usermanagement-be-dev.azurewebsites.net/api/v1/authenticate"){
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
