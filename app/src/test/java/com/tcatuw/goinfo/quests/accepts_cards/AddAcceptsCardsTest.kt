package com.tcatuw.goinfo.quests.accepts_cards

import com.tcatuw.goinfo.data.osm.edits.update_tags.StringMapEntryAdd
import com.tcatuw.goinfo.quests.TestMapDataWithGeometry
import com.tcatuw.goinfo.quests.verifyAnswer
import com.tcatuw.goinfo.testutils.node
import kotlin.test.Test
import kotlin.test.assertEquals

class AddAcceptsCardsTest {
    private val questType = AddAcceptsCards()

    @Test
    fun `sets expected tags`() {
        questType.verifyAnswer(
            mapOf(),
            CardAcceptance.DEBIT_AND_CREDIT,
            StringMapEntryAdd("payment:debit_cards", "yes"),
            StringMapEntryAdd("payment:credit_cards", "yes"),
        )
    }

    @Test
    fun `applicable to greengrocer shops`() {
        val mapData = TestMapDataWithGeometry(
            listOf(
                node(1, tags = mapOf("shop" to "greengrocer", "name" to "Foobar")),
            ),
        )
        assertEquals(1, questType.getApplicableElements(mapData).toList().size)
    }
}
