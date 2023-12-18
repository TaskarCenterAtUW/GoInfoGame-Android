package com.tcatuw.goinfo.data.elementfilter.filters

import com.tcatuw.goinfo.testutils.any
import com.tcatuw.goinfo.testutils.mock
import com.tcatuw.goinfo.testutils.node
import com.tcatuw.goinfo.testutils.on
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CombineFiltersTest {

    @Test fun `does not match if one doesn't match`() {
        val f1: ElementFilter = mock()
        on(f1.matches(any())).thenReturn(true)
        val f2: ElementFilter = mock()
        on(f2.matches(any())).thenReturn(false)
        assertFalse(CombineFilters(f1, f2).matches(node()))
    }

    @Test fun `does match if all match`() {
        val f1: ElementFilter = mock()
        on(f1.matches(any())).thenReturn(true)
        val f2: ElementFilter = mock()
        on(f2.matches(any())).thenReturn(true)
        assertTrue(CombineFilters(f1, f2).matches(node()))
    }
}
