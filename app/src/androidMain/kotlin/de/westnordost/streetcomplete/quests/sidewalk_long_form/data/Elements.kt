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
    @SerialName("recency_period_in_days")
    val recencyPeriodInDays: Int? = null,
    @SerialName("elements")
    val elements: List<Elements> = emptyList(),
    @SerialName("feature-presets")
    val featurePresets: List<FeaturePreset> = emptyList(),
    @SerialName("custom-icons")
    val customIcons: List<CustomIcon> = emptyList()
) : Parcelable

@Parcelize
@Serializable
data class FeaturePreset(
    @SerialName("name")
    val name: String,
    @SerialName("icon")
    val icon: String? = null,
    @SerialName("tags")
    val tags: Map<String, String> = emptyMap()
) : Parcelable

@Parcelize
@Serializable
data class CustomIcon(
    @SerialName("name")
    val name: String,
    @SerialName("url")
    val url: String,
    @SerialName("type")
    val type: String? = null
) : Parcelable
