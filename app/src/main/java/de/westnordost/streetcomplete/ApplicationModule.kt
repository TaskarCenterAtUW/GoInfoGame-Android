package de.westnordost.streetcomplete

import android.content.res.AssetManager
import android.content.res.Resources
import de.westnordost.streetcomplete.util.CrashReportExceptionHandler
import de.westnordost.streetcomplete.util.SoundFx
import de.westnordost.streetcomplete.util.logs.DatabaseLogger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.userAgent
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    factory<AssetManager> { androidContext().assets }
    factory<Resources> { androidContext().resources }

    single { CrashReportExceptionHandler(androidContext(), get(), "rajeshk@gaussiansolutions.com", "crashreport.txt") }
    single { DatabaseLogger(get()) }
    single { SoundFx(androidContext()) }
    single { HttpClient {
        install(ContentNegotiation){
            json(Json {
                ignoreUnknownKeys = true
            })
        }
        install(Logging){
            logger = Logger.DEFAULT
            level = LogLevel.ALL
        }
        defaultRequest {
            userAgent(ApplicationConstants.USER_AGENT)
        }
    } }
}
