package de.westnordost.streetcomplete.quests.sidewalk_long_form.data


import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class QuestAnswerChoice(
    @SerialName("choice_follow_up")
    val choiceFollowUp: String? = null,
    @SerialName("choice_text")
    val choiceText: String? = null,
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("value")
    val value: String? = null
) : Parcelable
