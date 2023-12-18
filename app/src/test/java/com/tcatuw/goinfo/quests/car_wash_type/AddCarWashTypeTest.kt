package com.tcatuw.goinfo.quests.car_wash_type

import com.tcatuw.goinfo.data.osm.edits.update_tags.StringMapEntryAdd
import com.tcatuw.goinfo.quests.car_wash_type.CarWashType.AUTOMATED
import com.tcatuw.goinfo.quests.car_wash_type.CarWashType.SELF_SERVICE
import com.tcatuw.goinfo.quests.car_wash_type.CarWashType.SERVICE
import com.tcatuw.goinfo.quests.verifyAnswer
import kotlin.test.Test

class AddCarWashTypeTest {

    private val questType = AddCarWashType()

    @Test fun `only self service`() {
        questType.verifyAnswer(
            listOf(SELF_SERVICE),
            StringMapEntryAdd("self_service", "only"),
            StringMapEntryAdd("automated", "no")
        )
    }

    @Test fun `only automated`() {
        questType.verifyAnswer(
            listOf(AUTOMATED),
            StringMapEntryAdd("self_service", "no"),
            StringMapEntryAdd("automated", "yes")
        )
    }

    @Test fun `only staff`() {
        questType.verifyAnswer(
            listOf(SERVICE),
            StringMapEntryAdd("self_service", "no"),
            StringMapEntryAdd("automated", "no")
        )
    }

    @Test fun `automated and self service`() {
        questType.verifyAnswer(
            listOf(AUTOMATED, SELF_SERVICE),
            StringMapEntryAdd("self_service", "yes"),
            StringMapEntryAdd("automated", "yes")
        )
    }

    @Test fun `automated and staff cleans car is tagged the same as automated only`() {
        questType.verifyAnswer(
            listOf(AUTOMATED, SERVICE),
            StringMapEntryAdd("self_service", "no"),
            StringMapEntryAdd("automated", "yes")
        )
    }

    @Test fun `self service and staff cleans car`() {
        questType.verifyAnswer(
            listOf(SELF_SERVICE, SERVICE),
            StringMapEntryAdd("self_service", "yes"),
            StringMapEntryAdd("automated", "no")
        )
    }

    @Test fun `automated, self service and staff cleans car is tagged the same way as automated and self service`() {
        questType.verifyAnswer(
            listOf(AUTOMATED, SELF_SERVICE, SERVICE),
            StringMapEntryAdd("self_service", "yes"),
            StringMapEntryAdd("automated", "yes")
        )
    }
}
