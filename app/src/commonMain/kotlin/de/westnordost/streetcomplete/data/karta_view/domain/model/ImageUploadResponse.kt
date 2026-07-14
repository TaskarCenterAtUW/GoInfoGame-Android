package de.westnordost.streetcomplete.data.karta_view.domain.model


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ImageUploadResponse(
    @SerialName("osv")
    val osv: OsvX,
    @SerialName("status")
    val status: StatusX
)
