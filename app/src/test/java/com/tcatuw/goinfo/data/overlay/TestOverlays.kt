package com.tcatuw.goinfo.data.overlay

import com.tcatuw.goinfo.data.osm.mapdata.Element
import com.tcatuw.goinfo.data.osm.mapdata.MapDataWithGeometry
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement
import com.tcatuw.goinfo.overlays.AbstractOverlayForm
import com.tcatuw.goinfo.overlays.Overlay
import com.tcatuw.goinfo.overlays.Style

open class TestOverlayA : Overlay {
    override fun getStyledElements(mapData: MapDataWithGeometry): Sequence<Pair<Element, Style>> = sequenceOf()
    override fun createForm(element: Element?): AbstractOverlayForm? = null
    override val changesetComment: String = "test"
    override val icon: Int = 0
    override val title: Int = 0
    override val wikiLink: String? = null
    override val achievements: List<EditTypeAchievement> = emptyList()
}

class TestOverlayB : TestOverlayA()
class TestOverlayC : TestOverlayA()
