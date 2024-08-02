package de.westnordost.streetcomplete.data

import kotlinx.serialization.Serializable

@Serializable
data class Address(
    val building: String? = null,
    val city: String? = null,
    val country: String? = null,
    val country_code: String? = null,
    val county: String? = null,
    val house_number: String? = null,
    val neighbourhood: String? = null,
    val postcode: String? = null,
    val road: String? = null,
    val state: String? = null,
    val suburb: String? = null
)
