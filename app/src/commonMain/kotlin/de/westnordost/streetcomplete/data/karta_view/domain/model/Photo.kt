package de.westnordost.streetcomplete.data.karta_view.domain.model


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Photo(
    @SerialName("id")
    val id : String,
    @SerialName("path")
    val path: String,
    @SerialName("photoName")
    val photoName: String,
)
