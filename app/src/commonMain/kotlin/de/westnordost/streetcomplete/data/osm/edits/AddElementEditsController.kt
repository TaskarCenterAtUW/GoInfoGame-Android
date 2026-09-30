package de.westnordost.streetcomplete.data.osm.edits

import de.westnordost.streetcomplete.data.osm.geometry.ElementGeometry

interface AddElementEditsController {
    /** Adds the edit to the to-be-uploaded queue and returns its id.
     *
     *  [beforeAnnouncing] runs once the edit is stored (with its id) but before anyone is told
     *  about it - storing an edit announces it to the upload queue, which may start uploading it
     *  right away, so anything that must be uploaded along with it (e.g. its photo) has to be
     *  stored in here, not after this returns. */
    fun add(
        type: ElementEditType,
        geometry: ElementGeometry,
        source: String,
        action: ElementEditAction,
        isNearUserLocation: Boolean,
        beforeAnnouncing: (editId: Long) -> Unit = {},
    ): Long
}
