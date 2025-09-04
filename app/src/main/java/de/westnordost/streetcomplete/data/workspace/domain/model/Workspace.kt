package de.westnordost.streetcomplete.data.workspace.domain.model

import de.westnordost.streetcomplete.util.satellite_layers.Imagery
import kotlinx.serialization.Serializable

@Serializable
data class Workspace(
    val id: Int,
    val quests: List<Int>? = null,
    val title: String,
    val type : String,
    val externalAppAccess: Int = 0,
    val imageryList : List<Imagery>? = null
)
