package de.westnordost.streetcomplete.data.workspace

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Workspace(
    val id: Int,
    val quests: List<Int>? = null,
    val title: String,
    val type: String,
    val externalAppAccess: Int = 0,
    // same field names as WorkspaceDetailsResponse (the per-workspace endpoint) - /mine also
    // returns these per workspace; kept nullable defensively since not every workspace is
    // guaranteed to have them populated.
    @SerialName("createdAt")
    val createdAt: String? = null,
    @SerialName("createdByName")
    val createdByName: String? = null,
    @SerialName("tdeiProjectGroupId")
    val tdeiProjectGroupId: String? = null,
)
