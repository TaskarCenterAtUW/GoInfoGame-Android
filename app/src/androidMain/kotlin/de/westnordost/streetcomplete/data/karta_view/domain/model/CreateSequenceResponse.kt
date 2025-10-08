package de.westnordost.streetcomplete.data.karta_view.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CreateSequenceResponse(
    val osv: Osv,
    val status: Status
)
