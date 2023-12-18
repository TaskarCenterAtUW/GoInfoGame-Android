package com.tcatuw.goinfo.quests.place_name

import com.tcatuw.goinfo.data.osm.edits.update_tags.StringMapEntryAdd
import com.tcatuw.goinfo.osm.LocalizedName
import com.tcatuw.goinfo.quests.verifyAnswer
import com.tcatuw.goinfo.testutils.mock
import kotlin.test.Test

class AddPlaceNameTest {

    private val questType = AddPlaceName(mock())

    @Test fun `apply no name answer`() {
        questType.verifyAnswer(
            NoPlaceNameSign,
            StringMapEntryAdd("name:signed", "no")
        )
    }

    @Test fun `apply name answer`() {
        questType.verifyAnswer(
            PlaceName(listOf(
                LocalizedName("", "Hey ya!"),
                LocalizedName("de", "He ja!"),
            )),
            StringMapEntryAdd("name", "Hey ya!"),
            StringMapEntryAdd("name:de", "He ja!"),
        )
    }
}
