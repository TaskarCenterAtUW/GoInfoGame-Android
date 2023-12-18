package com.tcatuw.goinfo.quests.traffic_signals_vibrate

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.elementfilter.toElementFilterExpression
import com.tcatuw.goinfo.data.osm.geometry.ElementGeometry
import com.tcatuw.goinfo.data.osm.mapdata.Element
import com.tcatuw.goinfo.data.osm.mapdata.MapDataWithGeometry
import com.tcatuw.goinfo.data.osm.osmquests.OsmElementQuestType
import com.tcatuw.goinfo.data.quest.AllCountriesExcept
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.BLIND
import com.tcatuw.goinfo.osm.Tags
import com.tcatuw.goinfo.osm.isCrossingWithTrafficSignals
import com.tcatuw.goinfo.osm.updateWithCheckDate
import com.tcatuw.goinfo.util.ktx.toYesNo

class AddTrafficSignalsVibration : OsmElementQuestType<Boolean> {

    private val crossingFilter by lazy { """
        nodes with
         crossing = traffic_signals
         and highway ~ crossing|traffic_signals
         and foot != no
         and (
          !$VIBRATING_BUTTON
          or $VIBRATING_BUTTON = no and $VIBRATING_BUTTON older today -4 years
          or $VIBRATING_BUTTON older today -8 years
         )
    """.toElementFilterExpression() }

    private val excludedWaysFilter by lazy { """
        ways with
          highway = cycleway
          and foot !~ yes|designated
    """.toElementFilterExpression() }

    override val changesetComment = "Specify whether traffic signals have tactile indications that it's safe to cross"
    override val wikiLink = "Key:$VIBRATING_BUTTON"
    override val icon = R.drawable.ic_quest_blind_traffic_lights
    override val achievements = listOf(BLIND)
    override val enabledInCountries = AllCountriesExcept(
        "RU" // see https://github.com/streetcomplete/StreetComplete/issues/4021
    )

    override fun getTitle(tags: Map<String, String>) = R.string.quest_traffic_signals_vibrate_title

    override fun getHighlightedElements(element: Element, getMapData: () -> MapDataWithGeometry) =
        getMapData().filter { it.isCrossingWithTrafficSignals() }.asSequence()

    override fun getApplicableElements(mapData: MapDataWithGeometry): Iterable<Element> {
        val excludedWayNodeIds = mapData.ways
            .filter { excludedWaysFilter.matches(it) }
            .flatMapTo(HashSet()) { it.nodeIds }

        return mapData.nodes
            .filter { crossingFilter.matches(it) && it.id !in excludedWayNodeIds }
    }

    override fun isApplicableTo(element: Element): Boolean? =
        if (!crossingFilter.matches(element)) false else null

    override fun createForm() = AddTrafficSignalsVibrationForm()

    override fun applyAnswerTo(answer: Boolean, tags: Tags, geometry: ElementGeometry, timestampEdited: Long) {
        tags.updateWithCheckDate(VIBRATING_BUTTON, answer.toYesNo())
    }
}

private const val VIBRATING_BUTTON = "traffic_signals:vibration"
