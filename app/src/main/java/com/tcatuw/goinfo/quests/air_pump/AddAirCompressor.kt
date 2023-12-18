package com.tcatuw.goinfo.quests.air_pump

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.osm.geometry.ElementGeometry
import com.tcatuw.goinfo.data.osm.mapdata.Element
import com.tcatuw.goinfo.data.osm.mapdata.MapDataWithGeometry
import com.tcatuw.goinfo.data.osm.mapdata.filter
import com.tcatuw.goinfo.data.osm.osmquests.OsmFilterQuestType
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.BICYCLIST
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.CAR
import com.tcatuw.goinfo.osm.Tags
import com.tcatuw.goinfo.osm.updateWithCheckDate
import com.tcatuw.goinfo.quests.YesNoQuestForm
import com.tcatuw.goinfo.util.ktx.toYesNo

class AddAirCompressor : OsmFilterQuestType<Boolean>() {

    override val elementFilter = """
       nodes, ways with
       amenity = fuel
       and (
           !compressed_air
           or compressed_air older today -6 years
       )
       and access !~ private|no
    """

    override val changesetComment = "Survey availability of air compressors"
    override val wikiLink = "Key:compressed_air"
    override val icon = R.drawable.ic_quest_car_air_compressor
    override val achievements = listOf(CAR, BICYCLIST)

    override fun getTitle(tags: Map<String, String>) = R.string.quest_air_pump_compressor_title

    override fun getHighlightedElements(element: Element, getMapData: () -> MapDataWithGeometry) =
        getMapData().filter("""
            nodes, ways with
            compressed_air = yes
            or amenity ~ compressed_air|fuel
        """)

    override fun createForm() = YesNoQuestForm()

    override fun applyAnswerTo(answer: Boolean, tags: Tags, geometry: ElementGeometry, timestampEdited: Long) {
        tags.updateWithCheckDate("compressed_air", answer.toYesNo())
    }
}
