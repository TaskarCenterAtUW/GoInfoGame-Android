package com.tcatuw.goinfo.quests.existence

import com.tcatuw.goinfo.data.osm.edits.update_tags.StringMapEntryAdd
import com.tcatuw.goinfo.data.osm.edits.update_tags.StringMapEntryDelete
import com.tcatuw.goinfo.data.osm.edits.update_tags.StringMapEntryModify
import com.tcatuw.goinfo.osm.nowAsCheckDateString
import com.tcatuw.goinfo.quests.verifyAnswer
import com.tcatuw.goinfo.testutils.mock
import com.tcatuw.goinfo.testutils.node
import com.tcatuw.goinfo.util.ktx.nowAsEpochMilliseconds
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CheckExistenceTest {
    private val questType = CheckExistence { tags ->
        if (tags["amenity"] == "telephone") mock() else null
    }

    @Test fun `apply answer adds check date`() {
        questType.verifyAnswer(
            Unit,
            StringMapEntryAdd("check_date", nowAsCheckDateString())
        )
    }

    @Test fun `apply answer removes all previous survey keys`() {
        questType.verifyAnswer(
            mapOf(
                "check_date" to "1",
                "lastcheck" to "a",
                "last_checked" to "b",
                "survey:date" to "c",
                "survey_date" to "d"
            ),
            Unit,
            StringMapEntryModify("check_date", "1", nowAsCheckDateString()),
            StringMapEntryDelete("lastcheck", "a"),
            StringMapEntryDelete("last_checked", "b"),
            StringMapEntryDelete("survey:date", "c"),
            StringMapEntryDelete("survey_date", "d"),
        )
    }

    @Test fun `isApplicableTo returns false for known places with recently edited amenity=telephone`() {
        assertFalse(
            questType.isApplicableTo(
                node(
                    tags = mapOf(
                        "amenity" to "telephone",
                    ), timestamp = nowAsEpochMilliseconds()
                )
            )
        )
    }

    @Test fun `isApplicableTo returns true for known places with old amenity=telephone`() {
        val millisecondsFor800Days: Long = 1000L * 60 * 60 * 24 * 800
        assertTrue(
            questType.isApplicableTo(
                node(
                    tags = mapOf(
                        "amenity" to "telephone",
                    ), timestamp = nowAsEpochMilliseconds() - millisecondsFor800Days
                )
            )
        )
    }
}
