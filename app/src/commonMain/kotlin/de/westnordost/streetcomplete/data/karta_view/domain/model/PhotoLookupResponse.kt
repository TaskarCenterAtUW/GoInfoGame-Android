package de.westnordost.streetcomplete.data.karta_view.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PhotoLookupResponse(
    val result: PhotoLookupResult? = null
)

@Serializable
data class PhotoLookupResult(
    val data: PhotoLookup? = null
)

@Serializable
data class PhotoLookup(
    val imageLthUrl: String
)
