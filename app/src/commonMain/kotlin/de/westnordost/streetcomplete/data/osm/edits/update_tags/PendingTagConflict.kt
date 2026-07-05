package de.westnordost.streetcomplete.data.osm.edits.update_tags

import de.westnordost.streetcomplete.data.osm.edits.ElementEditType
import de.westnordost.streetcomplete.data.osm.mapdata.ElementType
import de.westnordost.streetcomplete.data.osm.mapdata.LatLon

/** A single OSM tag on a specific element where the user's local edit collided with a concurrent
 *  remote edit on the same key. Held here instead of being discarded together with the rest of
 *  the edit, so the user can decide - next time the app is in the foreground - whether to keep
 *  their own answer or the other edit's value. */
data class PendingTagConflict(
    var id: Long,
    val elementType: ElementType,
    val elementId: Long,
    val tagKey: String,
    /** the value the user answered locally, or null if the user's edit was to delete this tag */
    val mineValue: String?,
    /** the conflicting value found on the server when the conflict was detected. Only used for
     *  display purposes - resolving re-fetches the current value to avoid re-racing a third edit
     *  that may have landed in the meantime */
    val theirsValueAtDetection: String?,
    val editType: ElementEditType,
    val source: String,
    val position: LatLon,
    val createdTimestamp: Long,
    var workspaceId: Int = 0
)
