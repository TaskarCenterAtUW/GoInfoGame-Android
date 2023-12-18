package com.tcatuw.goinfo.data.osm.edits.move

import com.tcatuw.goinfo.data.osm.edits.ElementIdProvider
import com.tcatuw.goinfo.data.osm.mapdata.ElementKey
import com.tcatuw.goinfo.data.osm.mapdata.ElementType
import com.tcatuw.goinfo.data.osm.mapdata.MapDataRepository
import com.tcatuw.goinfo.testutils.mock
import com.tcatuw.goinfo.testutils.node
import com.tcatuw.goinfo.testutils.on
import com.tcatuw.goinfo.testutils.p
import com.tcatuw.goinfo.util.ktx.copy
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RevertMoveNodeActionTest {

    private lateinit var repos: MapDataRepository
    private lateinit var provider: ElementIdProvider

    @BeforeTest fun setUp() {
        repos = mock()
        provider = mock()
    }

    @Test fun unmoveIt() {
        val n = node()
        val p = p(0.0, 1.0)
        val movedNode = n.copy(position = p)
        on(repos.getNode(n.id)).thenReturn(movedNode)
        val updates = RevertMoveNodeAction(n).createUpdates(repos, provider)
        assertTrue(updates.creations.isEmpty())
        assertTrue(updates.deletions.isEmpty())
        assertEquals(n, updates.modifications.single().copy(timestampEdited = n.timestampEdited))
    }

    @Test fun idsUpdatesApplied() {
        val node = node(id = -1)
        val action = RevertMoveNodeAction(node)
        val idUpdates = mapOf(ElementKey(ElementType.NODE, -1) to 5L)

        assertEquals(
            RevertMoveNodeAction(node.copy(id = 5)),
            action.idsUpdatesApplied(idUpdates)
        )
    }

    @Test fun elementKeys() {
        assertEquals(
            listOf(ElementKey(ElementType.NODE, -1)),
            RevertMoveNodeAction(node(id = -1)).elementKeys
        )
    }
}
