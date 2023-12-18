package com.tcatuw.goinfo.quests.bus_stop_name

import com.tcatuw.goinfo.data.osm.edits.update_tags.StringMapEntryAdd
import com.tcatuw.goinfo.osm.LocalizedName
import com.tcatuw.goinfo.quests.verifyAnswer
import kotlin.test.Test

class AddBusStopNameTest {

    private val questType = AddBusStopName()

    @Test
    fun `apply no name answer`() {
        questType.verifyAnswer(
            NoBusStopName,
            StringMapEntryAdd("name:signed", "no")
        )
    }

    @Test fun `apply name answer with one name`() {
        questType.verifyAnswer(
            BusStopName(listOf(LocalizedName("", "my name"))),
            StringMapEntryAdd("name", "my name")
        )
    }

    @Test fun `apply name answer with multiple names`() {
        questType.verifyAnswer(
            BusStopName(listOf(
                LocalizedName("", "Altona / All-Too-Close"),
                LocalizedName("de", "Altona"),
                LocalizedName("en", "All-Too-Close")
            )),
            StringMapEntryAdd("name", "Altona / All-Too-Close"),
            StringMapEntryAdd("name:en", "All-Too-Close"),
            StringMapEntryAdd("name:de", "Altona")
        )
    }
}
