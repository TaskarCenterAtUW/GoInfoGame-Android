package de.westnordost.streetcomplete.data.workspace.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppUpdateCheckerResponse(
    @SerialName("android")
    val android: Android,
) {
    @Serializable
    data class Android(
        @SerialName("dev")
        val dev: AppVersions,
        @SerialName("prod")
        val prod: AppVersions,
        @SerialName("stage")
        val stage: AppVersions,
    ) {
        @Serializable
        data class AppVersions(
            @SerialName("latest_version")
            val latestVersion: String,
            @SerialName("min_required_version")
            val minRequiredVersion: String,
        )
    }
}
