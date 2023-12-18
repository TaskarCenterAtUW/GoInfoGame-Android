package com.tcatuw.goinfo.data.quest

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.osm.geometry.ElementGeometry
import com.tcatuw.goinfo.data.osm.mapdata.Element
import com.tcatuw.goinfo.data.osm.mapdata.MapDataWithGeometry
import com.tcatuw.goinfo.data.osm.osmquests.OsmElementQuestType
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement
import com.tcatuw.goinfo.osm.Tags
import com.tcatuw.goinfo.quests.AbstractOsmQuestForm

open class TestQuestTypeA : OsmElementQuestType<String> {

    override fun getTitle(tags: Map<String, String>) = 0
    override fun isApplicableTo(element: Element): Boolean? = null
    override fun applyAnswerTo(answer: String, tags: Tags, geometry: ElementGeometry, timestampEdited: Long) {}
    override fun createForm(): AbstractOsmQuestForm<String> = object : AbstractOsmQuestForm<String>() {}
    override val changesetComment = "test me"
    override fun getApplicableElements(mapData: MapDataWithGeometry) = mapData.filter { isApplicableTo(it) == true }
    override val wikiLink: String? = null
    override val icon = 0
    override val achievements = emptyList<EditTypeAchievement>()
}

class TestQuestTypeB : TestQuestTypeA()
class TestQuestTypeC : TestQuestTypeA()
class TestQuestTypeD : TestQuestTypeA()
class TestQuestTypeE : TestQuestTypeA()

class TestQuestTypeDisabled : TestQuestTypeA() {
    override val defaultDisabledMessage = R.string.default_disabled_msg_go_inside
}
