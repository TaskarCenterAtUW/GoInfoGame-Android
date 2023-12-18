package com.tcatuw.goinfo.quests.summit

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.elementfilter.toElementFilterExpression
import com.tcatuw.goinfo.data.osm.geometry.ElementGeometry
import com.tcatuw.goinfo.data.osm.mapdata.Element
import com.tcatuw.goinfo.data.osm.mapdata.MapDataWithGeometry
import com.tcatuw.goinfo.data.osm.osmquests.OsmElementQuestType
import com.tcatuw.goinfo.data.quest.NoCountriesExcept
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.OUTDOORS
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.RARE
import com.tcatuw.goinfo.osm.Tags
import com.tcatuw.goinfo.osm.updateWithCheckDate
import com.tcatuw.goinfo.quests.YesNoQuestForm
import com.tcatuw.goinfo.util.ktx.toYesNo
import com.tcatuw.goinfo.util.math.distanceToArcs

class AddSummitCross : OsmElementQuestType<Boolean> {

    private val filter by lazy { """
        nodes with
          natural = peak
          and name
          and (!summit:cross or summit:cross older today -16 years)
    """.toElementFilterExpression() }

    override val changesetComment = "Specify whether summit crosses are present"
    override val wikiLink = "Key:summit:cross"
    override val icon = R.drawable.ic_quest_summit_cross
    override val achievements = listOf(RARE, OUTDOORS)
    override val enabledInCountries = NoCountriesExcept(
        // Europe
        "AT", // https://de.wikipedia.org/wiki/Gipfelkreuz
        "CH", // https://de.wikipedia.org/wiki/Gipfelkreuz
        "CO", // https://github.com/streetcomplete/StreetComplete/issues/5128
        "DE", // https://de.wikipedia.org/wiki/Gipfelbuch
        "ES", // https://es.wikipedia.org/wiki/Cruz_de_la_cumbre
        "FR", // https://fr.wikipedia.org/wiki/Croix_sommitale
        "IT", // https://it.wikipedia.org/wiki/Croce_di_vetta
        // not "NL": https://nl.wikipedia.org/wiki/Gipfelkreuz is about "foreign" summit crosses e.g. in the Alps
        "PL", // https://it.wikipedia.org/wiki/Croce_di_vetta#Alcuni_esempi_di_croci_di_vetta
        "RO", // https://es.wikipedia.org/wiki/Cruz_de_la_cumbre#Ejemplos
        "SK", // https://it.wikipedia.org/wiki/Croce_di_vetta#Alcuni_esempi_di_croci_di_vetta

        // Americas
        "AR", // https://en.wikipedia.org/wiki/Summit_cross#Gallery
        "PE", // https://es.wikipedia.org/wiki/Cruz_de_la_cumbre#Ejemplos
    )

    override fun getTitle(tags: Map<String, String>) = R.string.quest_summit_cross_title

    override fun getApplicableElements(mapData: MapDataWithGeometry): Iterable<Element> {
        val peaks = mapData.nodes.filter { filter.matches(it) }
        if (peaks.isEmpty()) return emptyList()

        val hikingPathsAndRoutes = getHikingPathsAndRoutes(mapData)

        // yes, this is very inefficient, however, peaks are very rare
        return peaks.filter { peak ->
            peak.tags["summit:register"] == "yes" || peak.tags.containsKey("summit:cross") ||
            hikingPathsAndRoutes.any { hikingPath ->
                hikingPath.polylines.any { ways ->
                    peak.position.distanceToArcs(ways) <= 10
                }
            }
        }
    }

    override fun isApplicableTo(element: Element) = when {
        !filter.matches(element) -> false
        element.tags["summit:register"] == "yes" || element.tags.containsKey("summit:cross") -> true
        else -> null
    }

    override fun createForm() = YesNoQuestForm()

    override fun applyAnswerTo(answer: Boolean, tags: Tags, geometry: ElementGeometry, timestampEdited: Long) {
        tags.updateWithCheckDate("summit:cross", answer.toYesNo())
    }
}
