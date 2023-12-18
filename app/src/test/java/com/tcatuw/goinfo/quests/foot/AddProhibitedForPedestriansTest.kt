package com.tcatuw.goinfo.quests.foot

import com.tcatuw.goinfo.data.osm.edits.update_tags.StringMapEntryAdd
import com.tcatuw.goinfo.data.osm.edits.update_tags.StringMapEntryDelete
import com.tcatuw.goinfo.data.osm.edits.update_tags.StringMapEntryModify
import com.tcatuw.goinfo.quests.foot.ProhibitedForPedestriansAnswer.HAS_SEPARATE_SIDEWALK
import com.tcatuw.goinfo.quests.foot.ProhibitedForPedestriansAnswer.IS_LIVING_STREET
import com.tcatuw.goinfo.quests.foot.ProhibitedForPedestriansAnswer.NO
import com.tcatuw.goinfo.quests.foot.ProhibitedForPedestriansAnswer.YES
import com.tcatuw.goinfo.quests.verifyAnswer
import kotlin.test.Test

class AddProhibitedForPedestriansTest {

    private val questType = AddProhibitedForPedestrians()

    @Test fun `apply yes answer`() {
        questType.verifyAnswer(YES, StringMapEntryAdd("foot", "no"))
    }

    @Test fun `apply no answer`() {
        questType.verifyAnswer(NO, StringMapEntryAdd("foot", "yes"))
    }

    @Test fun `apply separate sidewalk answer`() {
        questType.verifyAnswer(
            mapOf("sidewalk" to "no"),
            HAS_SEPARATE_SIDEWALK,
            StringMapEntryModify("sidewalk", "no", "separate")
        )
    }

    @Test fun `remove wrong sidewalk tagging`() {
        questType.verifyAnswer(
            mapOf("sidewalk" to "no", "sidewalk:both" to "no"),
            HAS_SEPARATE_SIDEWALK,
            StringMapEntryModify("sidewalk", "no", "separate"),
            StringMapEntryDelete("sidewalk:both", "no")
        )
    }

    @Test fun `apply living street answer`() {
        questType.verifyAnswer(
            mapOf("highway" to "residential"),
            IS_LIVING_STREET,
            StringMapEntryModify("highway", "residential", "living_street")
        )
    }
}
