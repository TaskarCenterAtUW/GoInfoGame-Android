package de.westnordost.streetcomplete.data.workspace.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Workspace(
    val id: Int,
    val quests: List<Int>? = null,
    val title: String,
    val type: String,
    val externalAppAccess: Int = 0,
)
