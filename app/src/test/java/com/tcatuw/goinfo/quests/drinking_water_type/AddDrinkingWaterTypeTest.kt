package com.tcatuw.goinfo.quests.drinking_water_type

import com.tcatuw.goinfo.data.osm.edits.update_tags.StringMapEntryAdd
import com.tcatuw.goinfo.data.osm.edits.update_tags.StringMapEntryDelete
import com.tcatuw.goinfo.osm.SURVEY_MARK_KEY
import com.tcatuw.goinfo.osm.nowAsCheckDateString
import com.tcatuw.goinfo.quests.verifyAnswer
import kotlin.test.Test

class AddDrinkingWaterTypeTest {
    private val questType = AddDrinkingWaterType()

    @Test
    fun `set drinking water as spring`() {
        questType.verifyAnswer(
            mapOf(
                "amenity" to "drinking_water",
            ),
            DrinkingWaterType.SPRING,
            StringMapEntryAdd("natural", "spring"),
        )
    }

    @Test
    fun `set drinking water as water tap`() {
        questType.verifyAnswer(
            mapOf(
                "amenity" to "drinking_water",
            ),
            DrinkingWaterType.WATER_TAP,
            StringMapEntryAdd("man_made", "water_tap"),
        )
    }

    @Test
    fun `set formerly disused drinking water as water tap`() {
        questType.verifyAnswer(
            mapOf(
                "disused:amenity" to "drinking_water",
            ),
            DrinkingWaterType.WATER_TAP,
            StringMapEntryAdd("man_made", "water_tap"),
            StringMapEntryAdd("amenity", "drinking_water"),
            StringMapEntryDelete("disused:amenity", "drinking_water"),
        )
    }

    @Test
    fun `set as disused`() {
        questType.verifyAnswer(
            mapOf(
                "amenity" to "drinking_water",
            ),
            DrinkingWaterType.DISUSED_DRINKING_WATER,
            StringMapEntryDelete("amenity", "drinking_water"),
            StringMapEntryAdd("disused:amenity", "drinking_water"),
        )
    }

    @Test
    fun `keep as disused`() {
        questType.verifyAnswer(
            mapOf(
                "disused:amenity" to "drinking_water",
            ),
            DrinkingWaterType.DISUSED_DRINKING_WATER,
            StringMapEntryAdd(SURVEY_MARK_KEY, nowAsCheckDateString()),
        )
    }
}
