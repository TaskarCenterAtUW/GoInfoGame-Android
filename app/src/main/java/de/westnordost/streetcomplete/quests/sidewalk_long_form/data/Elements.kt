package de.westnordost.streetcomplete.quests.sidewalk_long_form.data


import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class Elements(
    @SerialName("element_type")
    val elementType: String? = null,
    @SerialName("element_type_icon")
    val elementTypeIcon: String? = null,
    @SerialName("quest_query")
    val questQuery: String? = null,
    @SerialName("quests")
    val quests: List<LongFormQuest?> = emptyList()
) : Parcelable

@Parcelize
@Serializable
data class LongFormResponse(
    @SerialName("version")
    val version: String? = null,
    @SerialName("elements")
    val elements: List<Elements> = emptyList()
) : Parcelable
