package de.westnordost.streetcomplete.data.preferences

import de.westnordost.streetcomplete.BuildConfig

enum class Environment(
    val workspaceBaseUrl: String,
    val tdeiBaseUrl: String,
    val osmUrl: String,
    val tdeiWebUrl: String,
    /** Panoramax instance photos are uploaded to, with trailing slash */
    val panoramaxBaseUrl: String = PANORAMAX_DEV_BASE_URL,
    val appUpdateVersionCheckUrl: String = APP_UPDATE_VERSION_CHECKER_URL,
    val firebaseUpdateUrl: String = FIREBASE_UPDATE_URL,
) {
    STAGE(
        "https://api.workspaces-stage.sidewalks.washington.edu/api/v1/workspaces",
        "https://portal-api-stage.tdei.us/api/v1",
        "https://osm-workspaces-proxy.azurewebsites.net/stage/api/0.6/",
        "https://portal-stage.tdei.us",
        panoramaxBaseUrl = PANORAMAX_STAGE_BASE_URL,
    ),
    DEV(
        "https://api.workspaces-dev.sidewalks.washington.edu/api/v1/workspaces",
        "https://portal-api-dev.tdei.us/api/v1",
        "https://osm-workspaces-proxy.azurewebsites.net/dev/api/0.6/",
        "https://portal-dev.tdei.us",
        panoramaxBaseUrl = PANORAMAX_DEV_BASE_URL,
    ),
    PROD(
        "https://api.workspaces.sidewalks.washington.edu/api/v1/workspaces",
        "https://portal-api.tdei.us/api/v1",
        "https://osm-workspaces-proxy.azurewebsites.net/prod/api/0.6/",
        "https://portal.tdei.us",
        panoramaxBaseUrl = PANORAMAX_PROD_BASE_URL,
    );

    /** Bearer token for [panoramaxBaseUrl] - each instance has its own (build secrets, see
     *  secrets.properties.example) */
    val panoramaxAccessToken: String get() = when (this) {
        DEV -> BuildConfig.PANORAMAX_DEV_ACCESS_TOKEN
        STAGE -> BuildConfig.PANORAMAX_STAGE_ACCESS_TOKEN
        PROD -> BuildConfig.PANORAMAX_PROD_ACCESS_TOKEN
    }

    companion object {
        const val PANORAMAX_DEV_URL = PANORAMAX_DEV_BASE_URL
        const val PANORAMAX_STAGE_URL = PANORAMAX_STAGE_BASE_URL
        const val PANORAMAX_PROD_URL = PANORAMAX_PROD_BASE_URL
        const val APP_UPDATE_VERSION_CHECKER_URL =
            "https://raw.githubusercontent.com/TaskarCenterAtUW/asr-config/refs/heads/main/force-update/app-force-update.json"
        const val FIREBASE_UPDATE_URL =
            "https://appdistribution.firebase.google.com/testerapps"
    }
}

// outside of Environment's companion: an enum entry's arguments can't use that yet
private const val PANORAMAX_DEV_BASE_URL =
    "https://panoramax-dev-faaadghgcfgwcchk.westus2-01.azurewebsites.net/"
private const val PANORAMAX_STAGE_BASE_URL =
    "https://panoramax-stage-eubgh6hxgmf8ase5.westus2-01.azurewebsites.net/"
// TODO: the prod instance - until it exists, PROD uploads to the dev one (and with its token, see
//  panoramaxProdAccessToken in app/build.gradle.kts)
private const val PANORAMAX_PROD_BASE_URL = PANORAMAX_DEV_BASE_URL

class EnvironmentManager(val preferences: Preferences) {
    var currentEnvironment: Environment = Environment.valueOf(preferences.environment)
        get() {
            val name = preferences.environment
            return Environment.valueOf(name)
        }
        set(value) {
            field = value
            preferences.environment = value.name
        }
}
