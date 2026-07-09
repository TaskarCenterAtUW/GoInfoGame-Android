package de.westnordost.streetcomplete.data.osm.edits

import de.westnordost.streetcomplete.data.osm.mapdata.ElementType
import de.westnordost.streetcomplete.data.osm.mapdata.LatLon

/** A local edit that could not be applied because the underlying element was deleted or changed
 *  too substantially in the meantime (see ElementEditsUploader). Unlike a [de.westnordost.streetcomplete.data.osm.edits.update_tags.PendingTagConflict],
 *  there is nothing to resolve here - the edit is unsalvageable - so this is purely informational:
 *  it lets the user find out their answer was discarded instead of the quest just silently
 *  reappearing later with no explanation. */
data class DiscardedEditNotice(
    var id: Long,
    val editType: ElementEditType,
    /** null if the action this edit was for didn't refer to any existing element (e.g. creating a
     *  new node) - there's nothing to identify in that case */
    val elementType: ElementType?,
    val elementId: Long?,
    val position: LatLon,
    val reason: String,
    val createdTimestamp: Long,
    var workspaceId: Int = 0
)
