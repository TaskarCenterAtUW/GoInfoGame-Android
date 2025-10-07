package de.westnordost.streetcomplete.quests.sidewalk_long_form.data


import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class QuestAnswerValidation(
    @SerialName("min")
    val min: Int? = null,
    @SerialName("max")
    val max: Int? = null
) : Parcelable
