package com.tcatuw.goinfo.quests.shop_type

import de.westnordost.osmfeatures.Feature
import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.elementfilter.toElementFilterExpression
import com.tcatuw.goinfo.data.osm.geometry.ElementGeometry
import com.tcatuw.goinfo.data.osm.mapdata.Element
import com.tcatuw.goinfo.data.osm.mapdata.MapDataWithGeometry
import com.tcatuw.goinfo.data.osm.mapdata.filter
import com.tcatuw.goinfo.data.osm.osmquests.OsmElementQuestType
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.CITIZEN
import com.tcatuw.goinfo.osm.IS_SHOP_OR_DISUSED_SHOP_EXPRESSION
import com.tcatuw.goinfo.osm.LAST_CHECK_DATE_KEYS
import com.tcatuw.goinfo.osm.Tags
import com.tcatuw.goinfo.osm.isShopExpressionFragment
import com.tcatuw.goinfo.osm.updateCheckDate

class CheckShopExistence(
    private val getFeature: (tags: Map<String, String>) -> Feature?
) : OsmElementQuestType<Unit> {
    // opening hours quest acts as a de facto checker of shop existence, but some people disabled it.
    // separate from CheckExistence as very old shop with opening hours should show
    // opening hours resurvey quest rather than this one (which would cause edit date to be changed
    // and silence all resurvey quests)
    private val filter by lazy { ("""
        nodes, ways with (
             ${isShopExpressionFragment()}
             and !man_made
             and !historic
             and !military
             and !power
             and !attraction
             and !aeroway
             and !railway
        ) and (
          older today -2 years
          or ${LAST_CHECK_DATE_KEYS.joinToString(" or ") { "$it < today -2 years" }}
        )
        and (name or brand or noname = yes or name:signed = no)
    """).toElementFilterExpression() }

    override val changesetComment = "Survey if places (shops and other shop-like) still exist"
    override val wikiLink = "Key:disused:"
    override val icon = R.drawable.ic_quest_check_shop
    override val achievements = listOf(CITIZEN)

    override fun getTitle(tags: Map<String, String>) = R.string.quest_existence_title2

    override fun getApplicableElements(mapData: MapDataWithGeometry): Iterable<Element> =
        mapData.filter { isApplicableTo(it) }

    override fun isApplicableTo(element: Element): Boolean {
        if (!filter.matches(element)) return false
        val tags = element.tags
        // only show places that can be named somehow
        return hasName(tags)
    }

    override fun getHighlightedElements(element: Element, getMapData: () -> MapDataWithGeometry) =
        getMapData().filter(IS_SHOP_OR_DISUSED_SHOP_EXPRESSION)

    override fun createForm() = CheckShopExistenceForm()

    override fun applyAnswerTo(answer: Unit, tags: Tags, geometry: ElementGeometry, timestampEdited: Long) {
        tags.updateCheckDate()
    }

    private fun hasName(tags: Map<String, String>) = hasProperName(tags) || hasFeatureName(tags)

    private fun hasProperName(tags: Map<String, String>): Boolean =
        tags.containsKey("name") || tags.containsKey("brand")

    private fun hasFeatureName(tags: Map<String, String>) = getFeature(tags) != null
}
