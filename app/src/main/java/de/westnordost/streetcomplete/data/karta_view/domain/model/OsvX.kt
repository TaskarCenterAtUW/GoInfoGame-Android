package de.westnordost.streetcomplete.data.karta_view.domain.model


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OsvX(
    @SerialName("photo")
    val photo: Photo
)
