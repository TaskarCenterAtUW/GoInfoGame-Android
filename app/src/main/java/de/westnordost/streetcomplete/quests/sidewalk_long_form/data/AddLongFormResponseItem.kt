package de.westnordost.streetcomplete.quests.sidewalk_long_form.data


import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class AddLongFormResponseItem(
    @SerialName("element_type")
    val elementType: String? = null,
    @SerialName("quest_query")
    val questQuery: String? = null,
    @SerialName("quests")
    val quests: List<Quest?>? = null
) : Parcelable
