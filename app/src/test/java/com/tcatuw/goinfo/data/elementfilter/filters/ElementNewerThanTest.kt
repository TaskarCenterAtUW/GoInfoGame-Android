package com.tcatuw.goinfo.data.elementfilter.filters

import com.tcatuw.goinfo.data.elementfilter.dateDaysAgo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ElementNewerThanTest {
    val c = ElementNewerThan(RelativeDate(-10f))

    @Test fun `does not match older element`() {
        assertFalse(c.matches(mapOf(), dateDaysAgo(11f)))
    }

    @Test fun `matches newer element`() {
        assertTrue(c.matches(mapOf(), dateDaysAgo(9f)))
    }

    @Test fun `does not match element from same day`() {
        assertFalse(c.matches(mapOf(), dateDaysAgo(10f)))
    }

    @Test fun toStringMethod() {
        assertEquals("newer ${c.dateFilter}", c.toString())
    }
}
