package com.tcatuw.goinfo.data.osm.edits.create

import com.tcatuw.goinfo.data.osm.edits.ElementIdProvider
import com.tcatuw.goinfo.data.osm.edits.update_tags.StringMapChanges
import com.tcatuw.goinfo.data.osm.edits.update_tags.StringMapEntryAdd
import com.tcatuw.goinfo.data.osm.edits.update_tags.changesApplied
import com.tcatuw.goinfo.data.osm.mapdata.ElementKey
import com.tcatuw.goinfo.data.osm.mapdata.ElementType
import com.tcatuw.goinfo.data.osm.mapdata.MapDataChanges
import com.tcatuw.goinfo.data.osm.mapdata.MapDataRepository
import com.tcatuw.goinfo.data.upload.ConflictException
import com.tcatuw.goinfo.testutils.mock
import com.tcatuw.goinfo.testutils.node
import com.tcatuw.goinfo.testutils.on
import com.tcatuw.goinfo.testutils.way
import com.tcatuw.goinfo.util.math.translate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class CreateNodeFromVertexActionTest {
    private lateinit var repos: MapDataRepository
    private lateinit var provider: ElementIdProvider

    @BeforeTest
    fun setUp() {
        repos = mock()
        provider = mock()
    }

    @Test
    fun `conflict when node position changed`() {
        val n = node()
        val n2 = n.copy(position = n.position.translate(1.0, 0.0)) // moved by 1 meter
        on(repos.getNode(n.id)).thenReturn(n2)
        on(repos.getWaysForNode(n.id)).thenReturn(listOf())

        assertFailsWith<ConflictException> {
            CreateNodeFromVertexAction(n, StringMapChanges(listOf()), listOf())
                .createUpdates(repos, provider)
        }
    }

    @Test
    fun `conflict when node is not part of exactly the same ways as before`() {
        val n = node()
        on(repos.getNode(n.id)).thenReturn(n)
        on(repos.getWaysForNode(n.id)).thenReturn(listOf(way(1), way(2)))

        assertFailsWith<ConflictException> {
            CreateNodeFromVertexAction(n, StringMapChanges(listOf()), listOf(1L))
                .createUpdates(repos, provider)
        }
    }

    @Test
    fun `create updates`() {
        val n = node()
        on(repos.getNode(n.id)).thenReturn(n)
        on(repos.getWaysForNode(n.id)).thenReturn(listOf(way(1), way(2)))

        val changes = StringMapChanges(listOf(StringMapEntryAdd("a", "b")))

        val data = CreateNodeFromVertexAction(n, changes, listOf(1L, 2L)).createUpdates(repos, provider)

        val n2 = n.changesApplied(changes)

        assertEquals(MapDataChanges(modifications = listOf(n2)), data)
    }

    @Test fun idsUpdatesApplied() {
        val node = node(id = -1)
        val action = CreateNodeFromVertexAction(
            node,
            StringMapChanges(listOf()),
            listOf(-1, -2, 3) // and one that doesn't get updated
        )
        val idUpdates = mapOf(
            ElementKey(ElementType.WAY, -1) to 99L,
            ElementKey(ElementType.WAY, -2) to 5L,
            ElementKey(ElementType.NODE, -1) to 999L,
        )

        assertEquals(
            CreateNodeFromVertexAction(
                node.copy(id = 999),
                StringMapChanges(listOf()),
                listOf(99, 5, 3)
            ),
            action.idsUpdatesApplied(idUpdates)
        )
    }

    @Test fun elementKeys() {
        assertEquals(
            listOf(
                ElementKey(ElementType.WAY, -1),
                ElementKey(ElementType.WAY, -2),
                ElementKey(ElementType.WAY, 3),
                ElementKey(ElementType.NODE, -1),
            ),
            CreateNodeFromVertexAction(
                node(id = -1),
                StringMapChanges(listOf()),
                listOf(-1, -2, 3) // and one that doesn't get updated
            ).elementKeys
        )
    }
}
