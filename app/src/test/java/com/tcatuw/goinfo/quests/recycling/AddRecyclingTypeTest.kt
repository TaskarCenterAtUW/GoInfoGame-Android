package com.tcatuw.goinfo.quests.recycling

import com.tcatuw.goinfo.data.osm.edits.update_tags.StringMapEntryAdd
import com.tcatuw.goinfo.quests.verifyAnswer
import kotlin.test.Test

class AddRecyclingTypeTest {

    private val questType = AddRecyclingType()

    @Test fun `apply recycling centre answer`() {
        questType.verifyAnswer(
            RecyclingType.RECYCLING_CENTRE,
            StringMapEntryAdd("recycling_type", "centre")
        )
    }

    @Test fun `apply underground recycling container answer`() {
        questType.verifyAnswer(
            RecyclingType.UNDERGROUND_CONTAINER,
            StringMapEntryAdd("recycling_type", "container"),
            StringMapEntryAdd("location", "underground")
        )
    }

    @Test fun `apply overground recycling container answer`() {
        questType.verifyAnswer(
            RecyclingType.OVERGROUND_CONTAINER,
            StringMapEntryAdd("recycling_type", "container"),
            StringMapEntryAdd("location", "overground")
        )
    }
}
