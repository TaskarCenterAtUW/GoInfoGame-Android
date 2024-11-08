package de.westnordost.streetcomplete.data.workspace.domain.model


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserInfoResponse(
    @SerialName("apiKey")
    val apiKey: String? = null,
    @SerialName("email")
    val email: String? = null,
    @SerialName("emailVerified")
    val emailVerified: Boolean? = null,
    @SerialName("firstName")
    val firstName: String? = null,
    @SerialName("id")
    val id: String? = null,
    @SerialName("lastName")
    val lastName: String? = null,
    @SerialName("phone")
    val phone: String? = null,
    @SerialName("username")
    val username: String? = null
)
