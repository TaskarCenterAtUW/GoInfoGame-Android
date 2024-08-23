package de.westnordost.streetcomplete.data.workspace.data.remote

import de.westnordost.streetcomplete.data.preferences.Preferences

enum class Environment(
    val baseUrl: String,
    val loginUrl: String,
    val tdeiUrl: String,
    val osmUrl: String,
) {
    STAGE(
        "https://api.workspaces-stage.sidewalks.washington.edu/api/v1/workspaces",
        "https://tdei-gateway-stage.azurewebsites.net/api/v1/authenticate",
        "https://tdei-usermanagement-stage.azurewebsites.net/api/v1/user-profile",
        "https://osm.workspaces-stage.sidewalks.washington.edu/api/0.6/"
    ),
    DEV(
        "https://api.workspaces-stage.sidewalks.washington.edu/api/v1/workspaces",
        "https://tdei-gateway-stage.azurewebsites.net/api/v1/authenticate",
        "https://tdei-usermanagement-stage.azurewebsites.net/api/v1/user-profile",
        "https://osm.workspaces-stage.sidewalks.washington.edu/api/0.6/"
    ),
}

class EnvironmentManager(val preferences: Preferences) {
    var currentEnvironment: Environment = Environment.STAGE
        get() {
            val name = preferences.environment
            return Environment.valueOf(name)
        }
        set(value) {
            field = value
            preferences.environment = value.name
        }
}
