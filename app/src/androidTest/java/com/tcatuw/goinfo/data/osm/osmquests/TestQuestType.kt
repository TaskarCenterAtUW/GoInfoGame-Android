package com.tcatuw.goinfo.data.osm.osmquests

import com.tcatuw.goinfo.data.osm.geometry.ElementGeometry
import com.tcatuw.goinfo.data.osm.mapdata.Element
import com.tcatuw.goinfo.data.osm.mapdata.MapDataWithGeometry
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement
import com.tcatuw.goinfo.osm.Tags
import com.tcatuw.goinfo.quests.AbstractOsmQuestForm

open class TestQuestType : OsmElementQuestType<String> {

    override fun isApplicableTo(element: Element): Boolean? = null
    override fun applyAnswerTo(answer: String, tags: Tags, geometry: ElementGeometry, timestampEdited: Long) {}
    override val icon = 0
    override fun createForm(): AbstractOsmQuestForm<String> = object : AbstractOsmQuestForm<String>() {}
    override val changesetComment = ""
    override fun getTitle(tags: Map<String, String>) = 0
    override fun getApplicableElements(mapData: MapDataWithGeometry) = emptyList<Element>()
    override val wikiLink: String? = null
    override val achievements = emptyList<EditTypeAchievement>()
}
