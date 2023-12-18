package com.tcatuw.goinfo.quests.postbox_ref

import com.tcatuw.goinfo.data.osm.edits.update_tags.StringMapEntryAdd
import com.tcatuw.goinfo.quests.verifyAnswer
import kotlin.test.Test

class AddPostboxRefTest {

    private val questType = AddPostboxRef()

    @Test fun `apply no ref answer`() {
        questType.verifyAnswer(
            NoVisiblePostboxRef,
            StringMapEntryAdd("ref:signed", "no")
        )
    }

    @Test fun `apply ref answer`() {
        questType.verifyAnswer(
            PostboxRef("12d"),
            StringMapEntryAdd("ref", "12d")
        )
    }
}
