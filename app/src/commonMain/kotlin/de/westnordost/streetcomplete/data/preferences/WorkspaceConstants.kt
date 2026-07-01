package de.westnordost.streetcomplete.data.preferences

enum class Environment(
    val baseUrl: String,
    val tdeiUserManagementEndpoint: String,
    val tdeiApiUrl: String,
    val osmUrl: String,
    val tdeiWebUrl: String,
    val appUpdateVersionCheckUrl: String = APP_UPDATE_VERSION_CHECKER_URL,
    val firebaseUpdateUrl: String = FIREBASE_UPDATE_URL
) {
    STAGE(
        "https://api.workspaces-stage.sidewalks.washington.edu/api/v1/workspaces",
        "https://tdei-gateway-stage.azurewebsites.net/api/v1",
        "https://tdei-usermanagement-stage.azurewebsites.net/api/v1/user-profile",
        "https://osm-workspaces-proxy.azurewebsites.net/stage/api/0.6/",
        "https://portal-stage.tdei.us"
    ),
    DEV(
        "https://api.workspaces-dev.sidewalks.washington.edu/api/v1/workspaces",
        "https://tdei-api-dev.azurewebsites.net/api/v1",
        "https://tdei-usermanagement-be-dev.azurewebsites.net/api/v1/user-profile",
        "https://osm-workspaces-proxy.azurewebsites.net/dev/api/0.6/",
        "https://portal-dev.tdei.us"
    ),
    PROD(
        "https://api.workspaces.sidewalks.washington.edu/api/v1/workspaces",
        "https://tdei-gateway-prod.azurewebsites.net/api/v1",
        "https://tdei-usermanagement-prod.azurewebsites.net/api/v1/user-profile",
        "https://osm-workspaces-proxy.azurewebsites.net/prod/api/0.6/",
        "https://portal.tdei.us"
    );

    companion object {
        const val APP_UPDATE_VERSION_CHECKER_URL =
            "https://raw.githubusercontent.com/TaskarCenterAtUW/asr-config/refs/heads/main/force-update/app-force-update.json"
        const val FIREBASE_UPDATE_URL =
            "https://appdistribution.firebase.google.com/testerapps"

    }
}

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
