package de.westnordost.streetcomplete.data.osm.edits.create_feature

import de.westnordost.streetcomplete.data.osm.edits.ElementEditType
import de.westnordost.streetcomplete.data.osm.mapdata.ElementType
import de.westnordost.streetcomplete.data.osm.mapdata.LatLon

/** A notice that a not-yet-synced edit's attached photo(s) have repeatedly failed to upload to
 *  KartaView (see [de.westnordost.streetcomplete.data.osm.edits.upload.ElementEditsUploader]).
 *  Unlike [de.westnordost.streetcomplete.data.osm.edits.DiscardedEditNotice], this IS something
 *  the user can act on: keep retrying, or drop the photo so the rest of the edit's tag changes
 *  can still be submitted without it - which is why, unlike that notice, this one carries
 *  [editId], needed to know which edit/photo(s) those actions apply to. */
data class StuckPhotoUploadNotice(
    var id: Long,
    val editId: Long,
    val editType: ElementEditType,
    /** null if the action this edit was for didn't refer to any existing element (e.g. creating a
     *  new node) - there's nothing to identify in that case */
    val elementType: ElementType?,
    val elementId: Long?,
    val position: LatLon,
    val createdTimestamp: Long,
    var workspaceId: Int = 0
)
