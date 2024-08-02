package de.westnordost.streetcomplete.data

import kotlinx.serialization.Serializable

@Serializable
data class AddressModel(
    val address: Address? = null,
)
