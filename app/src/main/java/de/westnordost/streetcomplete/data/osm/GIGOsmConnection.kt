package de.westnordost.streetcomplete.data.osm

import android.content.Context
import android.content.Intent
import de.westnordost.osmapi.ApiRequestWriter
import de.westnordost.osmapi.ApiResponseReader
import de.westnordost.osmapi.OsmApiErrorFactory
import de.westnordost.osmapi.OsmConnection
import de.westnordost.osmapi.common.errors.OsmApiReadResponseException
import de.westnordost.osmapi.common.errors.OsmConnectionException
import de.westnordost.osmapi.common.errors.RedirectedException
import de.westnordost.streetcomplete.data.preferences.Preferences
import de.westnordost.streetcomplete.data.workspace.data.remote.EnvironmentManager
import de.westnordost.streetcomplete.data.workspace.domain.model.LoginResponse
import de.westnordost.streetcomplete.screens.workspaces.WorkSpaceActivity
import de.westnordost.streetcomplete.screens.workspaces.WorkSpaceActivity.Companion.SHOW_LOGGED_OUT_ALERT
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class GIGOsmConnection(
    private val context: Context,
    url: String,
    agent: String,
    private val preference: Preferences,
    private val environmentManager: EnvironmentManager
) : OsmConnection(
    url, agent, preference.workspaceToken, 45 * 1000
) {

    override fun <T> makeRequest(
        call: String, method: String?, authenticate: Boolean,
        writer: ApiRequestWriter?, reader: ApiResponseReader<T>?,
    ): T? {
        var connection: HttpURLConnection? = null
        try {
            connection = sendRequest(call, method, authenticate, writer)
            handleResponseCode(connection)

            return if (reader != null) handleResponse(connection, reader)
            else null
        } catch (e: IOException) {
            throw OsmConnectionException(e)
        } finally {
            connection?.disconnect()
        }
    }

    @Throws(IOException::class)
    private fun <T> handleResponse(connection: HttpURLConnection, reader: ApiResponseReader<T>): T {
        var `in`: InputStream? = null
        try {
            `in` = BufferedInputStream(connection.inputStream)
            return reader.parse(`in`)
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            throw OsmApiReadResponseException(e)
        } finally {
            `in`?.close()
        }
    }

    @Throws(IOException::class)
    private fun handleResponseCode(connection: HttpURLConnection) {
        val httpResponseCode = connection.responseCode
        // actually any response code between 200 and 299 is a "success" but may need additional
        // handling. Since the Osm Api only returns 200 on success curently, this check is fine
        if (httpResponseCode != HttpURLConnection.HTTP_OK &&
            httpResponseCode != HttpURLConnection.HTTP_UNAUTHORIZED
        ) {
            val responseMessage = connection.responseMessage
            val errorDescription = getErrorDescription(connection.errorStream)

            throw OsmApiErrorFactory.createError(
                httpResponseCode,
                responseMessage,
                errorDescription
            )
        }
    }

    @Throws(IOException::class)
    private fun getErrorDescription(inputStream: InputStream?): String? {
        if (inputStream == null) return null
        val result = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        var length: Int
        while ((inputStream.read(buffer).also { length = it }) != -1) {
            result.write(buffer, 0, length)
        }
        return result.toString(CHARSET)
    }

    @Throws(IOException::class)
    private fun sendRequest(
        call: String,
        method: String?,
        authenticate: Boolean,
        writer: ApiRequestWriter?,
    ): HttpURLConnection {
        var connection = openConnection(call)
        if (method != null) {
            connection.requestMethod = method
        }

        if (method in arrayOf("POST", "PUT", "PATCH")) {
            if (preference.workspaceToken != null) {
                connection.setRequestProperty(
                    "Authorization",
                    "Bearer ${preference.workspaceToken}"
                )
            }
        }


        preference.workspaceId?.let {
            connection.setRequestProperty("X-Workspace", it.toString())
        }
        if (writer != null && writer.contentType != null) {
            connection.setRequestProperty("Content-Type", writer.contentType)
            connection.setRequestProperty("charset", CHARSET.lowercase(Locale.UK))
        }

        if (writer != null) {
            sendRequestPayload(connection, writer)
        }

        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
            synchronized(this) {
                val newToken = refreshToken() // Refresh token synchronously
                if (!newToken.isNullOrBlank()) {
                    preference.workspaceToken = newToken
                    connection.disconnect() // Close previous connection
                    connection = openConnection(call) // Open new connection with updated token
                    connection.setRequestProperty("Authorization", "Bearer $newToken")
                    return sendRequest(call, method, authenticate, writer) // Retry request
                } else {
                    preference.workspaceToken = null // Clear token if refresh fails
                    preference.workspaceLogin = false
                    //Launch workspaceActivity
                    val intent = Intent(context, WorkSpaceActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    intent.putExtra(SHOW_LOGGED_OUT_ALERT, true)
                    context.startActivity(intent)
                }
            }
        }

        return connection
    }

    private fun refreshToken(): String? {
        val refreshToken = preference.workspaceRefreshToken ?: return null
        try {
            val url = URL(environmentManager.currentEnvironment.loginUrl + "/refresh-token")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            // Send refresh token payload

            val jsonPayload = "\"$refreshToken\"" // Ensure token is passed as a JSON string
            connection.outputStream.use { it.write(jsonPayload.toByteArray(Charsets.UTF_8)) }


            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseStream = connection.inputStream.bufferedReader().use { it.readText() }
                val loginResponse = Json.decodeFromString<LoginResponse>(responseStream)
                preference.workspaceToken = loginResponse.access_token
                preference.workspaceRefreshToken = loginResponse.refresh_token
                preference.refreshTokenExpiryInterval = loginResponse.refresh_expires_in * 1000
                preference.accessTokenExpiryInterval = loginResponse.expires_in * 1000
                preference.workspaceLastLogin = System.currentTimeMillis()
                return loginResponse.access_token
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null // Refresh failed
    }

    @Throws(IOException::class)
    private fun sendRequestPayload(connection: HttpURLConnection, writer: ApiRequestWriter) {
        connection.doOutput = true

        var out: OutputStream? = null
        try {
            out = connection.outputStream
            writer.write(out)
        } catch (e: IOException) {
            connection.disconnect()
            throw e
        } finally {
            out?.close()
        }
    }

    @Synchronized
    @Throws(IOException::class)
    private fun openConnection(call: String): HttpURLConnection {
        val url = URL(URL(apiUrl), call)
        val connection = url.openConnection() as HttpURLConnection

        // hotel wifi with signon
        if (url.host != connection.url.host) {
            throw RedirectedException()
        }

        if (userAgent != null) {
            connection.setRequestProperty("User-Agent", userAgent)
        }
        connection.connectTimeout = timeout
        connection.readTimeout = timeout

        // default is method=GET, doInput=true, doOutput=false
        return connection
    }
}
