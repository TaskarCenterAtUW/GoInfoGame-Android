package de.westnordost.streetcomplete.quests.sidewalk_long_form.data


import de.westnordost.streetcomplete.util.satellite_layers.Imagery
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class WorkspaceDetailsResponse(
    @SerialName("createdAt")
    val createdAt: String,
    @SerialName("createdBy")
    val createdBy: String,
    @SerialName("createdByName")
    val createdByName: String,
    @SerialName("description")
    val description: String?,
    @SerialName("externalAppAccess")
    val externalAppAccess: Int,
    @SerialName("id")
    val id: Int,
    @SerialName("imageryListDef")
    val imageryListDef: List<Imagery>?,
    @SerialName("kartaViewToken")
    val kartaViewToken: String?,
    @SerialName("longFormQuestDef")
    val longFormQuestDef: JsonElement?,
    @SerialName("tdeiMetadata")
    val tdeiMetadata: String,
    @SerialName("tdeiProjectGroupId")
    val tdeiProjectGroupId: String,
    @SerialName("tdeiRecordId")
    val tdeiRecordId: String,
    @SerialName("tdeiServiceId")
    val tdeiServiceId: String,
    @SerialName("title")
    val title: String,
    @SerialName("type")
    val type: String
)
