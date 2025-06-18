package de.westnordost.streetcomplete

import android.content.Intent
import android.content.res.AssetManager
import android.content.res.Resources
import de.westnordost.streetcomplete.data.preferences.Preferences
import de.westnordost.streetcomplete.data.workspace.data.remote.EnvironmentManager
import de.westnordost.streetcomplete.data.workspace.domain.model.LoginResponse
import de.westnordost.streetcomplete.screens.workspaces.WorkSpaceActivity
import de.westnordost.streetcomplete.screens.workspaces.WorkSpaceActivity.Companion.SHOW_LOGGED_OUT_ALERT
import de.westnordost.streetcomplete.util.CrashReportExceptionHandler
import de.westnordost.streetcomplete.util.SoundFx
import de.westnordost.streetcomplete.util.logs.DatabaseLogger
import de.westnordost.streetcomplete.util.satellite_layers.ImageryRepository
import de.westnordost.streetcomplete.util.logs.Log
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.userAgent
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    factory<AssetManager> { androidContext().assets }
    factory<Resources> { androidContext().resources }

    single {
        CrashReportExceptionHandler(
            androidContext(),
            get(),
            "rajeshk@gaussiansolutions.com",
            "crashreport.txt"
        )
    }
    single { DatabaseLogger(get()) }
    single { SoundFx(androidContext()) }
    single {
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
            install(Auth) {
                bearer {
                    loadTokens {
                        val token = get<Preferences>().workspaceToken
                        val refreshToken = get<Preferences>().workspaceRefreshToken
                        if (token != null && refreshToken != null) {
                            BearerTokens(token, refreshToken)
                        } else {
                            null
                        }
                    }

                    refreshTokens {
                        val preferences = get<Preferences>()
                        val httpClient = get<HttpClient>() // Inject HttpClient for making requests
                        val environmentManager = get<EnvironmentManager>()

                        if (!preferences.workspaceLogin)
                            return@refreshTokens null
                        val newAccessToken =
                            refreshJwtToken(httpClient, preferences, environmentManager)

                        if (newAccessToken == null) {
                            preferences.workspaceLogin = false
                            //Launch workspaceActivity
                            val intent = Intent(androidContext(), WorkSpaceActivity::class.java)
                            intent.flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            intent.putExtra(SHOW_LOGGED_OUT_ALERT, true)
                            androidContext().startActivity(intent)
                        }

                        newAccessToken?.let {
                            preferences.workspaceToken = it  // Save new access token
                            BearerTokens(it, preferences.workspaceRefreshToken!!)
                        }
                    }
                }
            }
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        Log.d("KtorClient", message) // Avoid System.err
                    }
                }
                level = LogLevel.ALL
            }
            defaultRequest {
                userAgent(ApplicationConstants.USER_AGENT)
            }
        }
    }

    single { ImageryRepository(get()) }

}

suspend fun refreshJwtToken(
    client: HttpClient,
    preferences: Preferences,
    environmentManager: EnvironmentManager
): String? {
    return try {

        val tempClient = HttpClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.ALL
            }
        }

        val response =
            tempClient.post(environmentManager.currentEnvironment.loginUrl + "/refresh-token") {
                setBody(preferences.workspaceRefreshToken)
                headers {
                    contentType(ContentType.Application.Json)
                }
            }

        if (response.status == HttpStatusCode.OK) {
            val jsonResponse = Json.decodeFromString<LoginResponse>(response.bodyAsText())

            preferences.workspaceToken = jsonResponse.access_token
            preferences.workspaceRefreshToken = jsonResponse.refresh_token
            preferences.refreshTokenExpiryInterval = jsonResponse.refresh_expires_in * 1000
            preferences.accessTokenExpiryInterval = jsonResponse.expires_in * 1000
            preferences.workspaceLastLogin = System.currentTimeMillis()

            jsonResponse.access_token
        } else null
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
