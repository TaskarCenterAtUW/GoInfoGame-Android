package de.westnordost.streetcomplete.data.karta_view.domain.model


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StatusX(
    @SerialName("apiCode")
    val apiCode: Int,
    @SerialName("apiMessage")
    val apiMessage: String,
    @SerialName("httpCode")
    val httpCode: Int,
    @SerialName("httpMessage")
    val httpMessage: String
)
