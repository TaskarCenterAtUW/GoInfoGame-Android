package com.tcatuw.goinfo.overlays

import com.tcatuw.goinfo.data.osm.mapdata.ElementKey

interface IsShowingElement {
    /** The element that is showing right now, if any */
    val elementKey: ElementKey?
}
