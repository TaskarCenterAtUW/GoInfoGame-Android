package de.westnordost.streetcomplete.data.osm.edits

import de.westnordost.streetcomplete.data.osm.geometry.ElementGeometry

interface AddElementEditsController {
    /** Adds the edit to the to-be-uploaded queue and returns its id */
    fun add(
        type: ElementEditType,
        geometry: ElementGeometry,
        source: String,
        action: ElementEditAction,
        isNearUserLocation: Boolean
    ): Long
}
