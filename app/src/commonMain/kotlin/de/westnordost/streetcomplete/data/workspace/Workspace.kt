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
    // per-workspace conflict-resolution mode from WorkspaceDetailsResponse.overrideConflicts,
    // persisted so ElementEditUploader can look it up by workspace id independent of whether the
    // workspace screen is open. false (default, matches null/missing from the API) = RESOLVE
    // (shows the conflict dialog); true = OVERRIDE (auto-prefers the app's own value, no dialog).
    val overrideConflicts: Boolean = false,
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
