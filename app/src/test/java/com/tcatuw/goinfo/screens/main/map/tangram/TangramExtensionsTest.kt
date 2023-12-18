package com.tcatuw.goinfo.screens.main.map.tangram

import com.tcatuw.goinfo.testutils.p
import kotlin.test.Test
import kotlin.test.assertEquals

class TangramExtensionsTest {

    @Test fun `convert single`() {
        val pos = p(5.0, 10.0)
        val pos2 = pos.toLngLat().toLatLon()

        assertEquals(pos, pos2)
    }
}
