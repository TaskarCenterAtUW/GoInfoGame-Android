package de.westnordost.streetcomplete.util.creds_manager

import kotlinx.serialization.Serializable

@Serializable
data class EnvCredentials(
    val username: String,
    val password: String
)
