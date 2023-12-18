package com.tcatuw.goinfo.osm.lit

import com.tcatuw.goinfo.data.osm.edits.update_tags.StringMapChangesBuilder
import com.tcatuw.goinfo.data.osm.edits.update_tags.StringMapEntryAdd
import com.tcatuw.goinfo.data.osm.edits.update_tags.StringMapEntryChange
import com.tcatuw.goinfo.data.osm.edits.update_tags.StringMapEntryModify
import com.tcatuw.goinfo.osm.lit.LitStatus.*
import com.tcatuw.goinfo.osm.nowAsCheckDateString
import org.assertj.core.api.Assertions
import kotlin.test.Test
import kotlin.test.assertFailsWith

class LitStatusKtTest {

    @Test
    fun `apply normally`() {
        verifyAnswer(mapOf(), YES, arrayOf(StringMapEntryAdd("lit", "yes")))
        verifyAnswer(mapOf(), NO, arrayOf(StringMapEntryAdd("lit", "no")))
        verifyAnswer(mapOf(), AUTOMATIC, arrayOf(StringMapEntryAdd("lit", "automatic")))
        verifyAnswer(mapOf(), NIGHT_AND_DAY, arrayOf(StringMapEntryAdd("lit", "24/7")))
    }

    @Test
    fun `applying invalid throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            UNSUPPORTED.applyTo(StringMapChangesBuilder(mapOf()))
        }
    }

    @Test fun `apply updates check date`() {
        val today = nowAsCheckDateString()
        verifyAnswer(
            mapOf("lit" to "yes"),
            YES,
            arrayOf(
                StringMapEntryModify("lit", "yes", "yes"),
                StringMapEntryAdd("check_date:lit", today)
            )
        )
        verifyAnswer(
            mapOf("lit" to "no"),
            NO,
            arrayOf(
                StringMapEntryModify("lit", "no", "no"),
                StringMapEntryAdd("check_date:lit", today)
            )
        )
        verifyAnswer(
            mapOf("lit" to "automatic"),
            AUTOMATIC,
            arrayOf(
                StringMapEntryModify("lit", "automatic", "automatic"),
                StringMapEntryAdd("check_date:lit", today)
            )
        )
        verifyAnswer(
            mapOf("lit" to "24/7"),
            NIGHT_AND_DAY,
            arrayOf(
                StringMapEntryModify("lit", "24/7", "24/7"),
                StringMapEntryAdd("check_date:lit", today)
            )
        )
    }

    @Test fun `apply does not overwrite unsupported value if 'yes'`() {
        verifyAnswer(
            mapOf("lit" to "limited"),
            YES,
            arrayOf(StringMapEntryAdd("check_date:lit", nowAsCheckDateString()))
        )
        verifyAnswer(
            mapOf("lit" to "22:00-05:00"),
            YES,
            arrayOf(StringMapEntryAdd("check_date:lit", nowAsCheckDateString()))
        )
    }

    @Test fun `apply does overwrite unsupported value if not 'yes'`() {
        verifyAnswer(
            mapOf("lit" to "limited"),
            NO,
            arrayOf(StringMapEntryModify("lit", "limited", "no"))
        )
        verifyAnswer(
            mapOf("lit" to "22:00-05:00"),
            AUTOMATIC,
            arrayOf(StringMapEntryModify("lit", "22:00-05:00", "automatic"))
        )
    }
}

private fun verifyAnswer(tags: Map<String, String>, answer: LitStatus, expectedChanges: Array<StringMapEntryChange>) {
    val cb = StringMapChangesBuilder(tags)
    answer.applyTo(cb)
    val changes = cb.create().changes
    Assertions.assertThat(changes).containsExactlyInAnyOrder(*expectedChanges)
}
