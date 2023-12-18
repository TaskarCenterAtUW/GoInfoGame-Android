package com.tcatuw.goinfo.data.osm.edits

import com.tcatuw.goinfo.data.osm.geometry.ElementGeometry

interface AddElementEditsController {
    fun add(
        type: ElementEditType,
        geometry: ElementGeometry,
        source: String,
        action: ElementEditAction
    )
}
