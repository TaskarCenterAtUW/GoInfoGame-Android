package de.westnordost.streetcomplete.data.workspace.domain.model

data class LoginResponse(
    val access_token: String,
    val expires_in: Int,
    val refresh_expires_in: Int,
    val refresh_token: String
)
