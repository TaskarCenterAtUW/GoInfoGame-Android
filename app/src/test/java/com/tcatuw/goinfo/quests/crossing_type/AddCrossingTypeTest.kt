package com.tcatuw.goinfo.quests.crossing_type

import com.tcatuw.goinfo.data.osm.edits.update_tags.StringMapEntryAdd
import com.tcatuw.goinfo.data.osm.edits.update_tags.StringMapEntryModify
import com.tcatuw.goinfo.osm.nowAsCheckDateString
import com.tcatuw.goinfo.quests.crossing_type.CrossingType.MARKED
import com.tcatuw.goinfo.quests.crossing_type.CrossingType.TRAFFIC_SIGNALS
import com.tcatuw.goinfo.quests.verifyAnswer
import kotlin.test.Test

class AddCrossingTypeTest {

    private val questType = AddCrossingType()

    @Test fun `apply normal answer`() {
        questType.verifyAnswer(
            TRAFFIC_SIGNALS,
            StringMapEntryAdd("crossing", "traffic_signals")
        )
    }

    @Test fun `apply answer for crossing = island`() {
        questType.verifyAnswer(
            mapOf("crossing" to "island"),
            TRAFFIC_SIGNALS,
            StringMapEntryModify("crossing", "island", "traffic_signals"),
            StringMapEntryAdd("crossing:island", "yes")
        )
    }

    @Test fun `apply answer for crossing = island and crossing_island set`() {
        questType.verifyAnswer(
            mapOf("crossing" to "island", "crossing:island" to "something"),
            TRAFFIC_SIGNALS,
            StringMapEntryModify("crossing", "island", "traffic_signals"),
            StringMapEntryModify("crossing:island", "something", "yes")
        )
    }

    @Test fun `apply marked answer does not change the type of marked value`() {
        questType.verifyAnswer(
            mapOf("crossing" to "zebra"),
            MARKED,
            StringMapEntryAdd("check_date:crossing", nowAsCheckDateString())
        )

        questType.verifyAnswer(
            mapOf("crossing" to "marked"),
            MARKED,
            StringMapEntryAdd("check_date:crossing", nowAsCheckDateString())
        )

        questType.verifyAnswer(
            mapOf("crossing" to "uncontrolled"),
            MARKED,
            StringMapEntryAdd("check_date:crossing", nowAsCheckDateString())
        )

        questType.verifyAnswer(
            mapOf("crossing" to "unmarked"),
            MARKED,
            StringMapEntryModify("crossing", "unmarked", "marked")
        )
    }
}
