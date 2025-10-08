package de.westnordost.streetcomplete.data.workspace.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    val access_token: String,
    val expires_in: Long,
    val refresh_expires_in: Long,
    val refresh_token: String
)
