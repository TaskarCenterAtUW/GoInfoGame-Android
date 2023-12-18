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

class AddSummitRegister : OsmElementQuestType<Boolean> {

    private val filter by lazy { """
        nodes with
          natural = peak
          and name
          and (!summit:register or summit:register older today -4 years)
    """.toElementFilterExpression() }

    override val changesetComment = "Specify whether summit registers are present"
    override val wikiLink = "Key:summit:register"
    override val icon = R.drawable.ic_quest_peak
    override val achievements = listOf(RARE, OUTDOORS)
    override val enabledInCountries = NoCountriesExcept(
        // regions gathered in
        // https://github.com/streetcomplete/StreetComplete/issues/561#issuecomment-325623974

        // Europe
        "AT", // https://de.wikipedia.org/wiki/Gipfelkreuz
        "CH", // https://de.wikipedia.org/wiki/Gipfelkreuz
        "CZ", // https://cs.wikipedia.org/wiki/Vrcholov%C3%A1_kniha
        "DE", // https://de.wikipedia.org/wiki/Gipfelbuch
        "ES", // https://es.wikipedia.org/wiki/Comprobante_de_cumbre
        "FR", // https://it.wikipedia.org/wiki/Libro_di_vetta#Alcuni_esempi_di_libri_di_vetta
        "GR", // https://it.wikipedia.org/wiki/Libro_di_vetta#Alcuni_esempi_di_libri_di_vetta
        "IT", // https://it.wikipedia.org/wiki/Libro_di_vetta
        // not "NL": https://nl.wikipedia.org/wiki/Gipfelbuch is about "foreign" summit registers e.g. in the Alps
        // not "PL": https://github.com/westnordost/StreetComplete/issues/561#issuecomment-325504455
        "RO", // https://es.wikipedia.org/wiki/Cruz_de_la_cumbre#Ejemplos
        "SI", // https://it.wikipedia.org/wiki/Libro_di_vetta#Alcuni_esempi_di_libri_di_vetta
        "SK", // https://it.wikipedia.org/wiki/Croce_di_vetta#Alcuni_esempi_di_croci_di_vetta

        // Americas
        "AR", // https://en.wikipedia.org/wiki/Summit_cross#Gallery
        "PE", // https://es.wikipedia.org/wiki/Cruz_de_la_cumbre#Ejemplos
        "US", // https://de.wikipedia.org/wiki/Gipfelkreuz
    )

    override fun getTitle(tags: Map<String, String>) = R.string.quest_summit_register_title2

    override fun getApplicableElements(mapData: MapDataWithGeometry): Iterable<Element> {
        val peaks = mapData.nodes.filter { filter.matches(it) }
        if (peaks.isEmpty()) return emptyList()

        val hikingPathsAndRoutes = getHikingPathsAndRoutes(mapData)

        // yes, this is very inefficient, however, peaks are very rare
        return peaks.filter { peak ->
            peak.tags["summit:cross"] == "yes" || peak.tags.containsKey("summit:register") ||
            hikingPathsAndRoutes.any { hikingPath ->
                hikingPath.polylines.any { ways ->
                    peak.position.distanceToArcs(ways) <= 10
                }
            }
        }
    }

    override fun isApplicableTo(element: Element) = when {
        !filter.matches(element) -> false
        element.tags["summit:cross"] == "yes" || element.tags.containsKey("summit:register") -> true
        else -> null
    }

    override fun createForm() = YesNoQuestForm()

    override fun applyAnswerTo(answer: Boolean, tags: Tags, geometry: ElementGeometry, timestampEdited: Long) {
        tags.updateWithCheckDate("summit:register", answer.toYesNo())
    }
}
