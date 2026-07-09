package de.westnordost.streetcomplete.data.osm.edits.update_tags

import de.westnordost.streetcomplete.data.osm.edits.ElementEditType
import de.westnordost.streetcomplete.data.osm.mapdata.ElementType
import de.westnordost.streetcomplete.data.osm.mapdata.LatLon

/** A single OSM tag on a specific element where the user's local edit collided with a concurrent
 *  remote edit on the same key. The whole edit is held back from uploading (blocked) while any of
 *  these exist for it; once the user has decided per tag - keep their own answer or accept the
 *  other edit's value - the decisions are folded into the edit and it uploads as one unit. */
data class PendingTagConflict(
    var id: Long,
    /** id of the [de.westnordost.streetcomplete.data.osm.edits.ElementEdit] held back by this conflict */
    val editId: Long,
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
