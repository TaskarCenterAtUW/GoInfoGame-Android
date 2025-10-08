package de.westnordost.streetcomplete.data.karta_view.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Status(
    val apiCode: Int,
    val apiMessage: String,
    val httpCode: Int,
    val httpMessage: String
)
