package de.westnordost.streetcomplete.data.workspace

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProjectGroupItem(
    @SerialName("project_group_name")
    val projectGroupName: String,
    @SerialName("roles")
    val roles: List<String>,
    @SerialName("tdei_project_group_id")
    val tdeiProjectGroupId: String,
)
