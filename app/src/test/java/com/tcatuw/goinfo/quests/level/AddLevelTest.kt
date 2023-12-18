package com.tcatuw.goinfo.quests.level

import com.tcatuw.goinfo.data.osm.geometry.ElementPointGeometry
import com.tcatuw.goinfo.data.osm.geometry.ElementPolygonsGeometry
import com.tcatuw.goinfo.data.osm.mapdata.LatLon
import com.tcatuw.goinfo.quests.createMapData
import com.tcatuw.goinfo.testutils.node
import com.tcatuw.goinfo.testutils.p
import com.tcatuw.goinfo.testutils.way
import kotlin.test.Test
import kotlin.test.assertEquals

class AddLevelTest {
    private val questType = AddLevel()

    @Test fun `does not create quest for shop`() {
        val mapData = createMapData(mapOf(shopWithoutLevel()))
        assertEquals(0, questType.getApplicableElements(mapData).toList().size)
    }

    @Test fun `does not create quest for shop in single-storey mall`() {
        val mapData = createMapData(mapOf(
            mall,
            thingWithLevel("1"),
            thingWithLevel("1"),
            shopWithoutLevel(),
        ))
        assertEquals(0, questType.getApplicableElements(mapData).toList().size)
    }

    @Test fun `does not create quest for shop just outside multi-storey mall`() {
        val mapData = createMapData(mapOf(
            mall,
            thingWithLevel("1"),
            thingWithLevel("0"),
            shopWithoutLevel(p(0.24, 0.5)),
        ))
        assertEquals(0, questType.getApplicableElements(mapData).toList().size)
    }

    @Test fun `does create quest for shop inside multi-storey mall (level)`() {
        val mapData = createMapData(mapOf(
            mall,
            thingWithLevel("1"),
            thingWithLevel("0"),
            shopWithoutLevel(p(0.5, 0.5)),
        ))
        assertEquals(0, questType.getApplicableElements(mapData).toList().size)
    }
}

private val P1 = p(0.25, 0.25)
private val P2 = p(0.25, 0.75)
private val P3 = p(0.75, 0.75)
private val P4 = p(0.75, 0.25)
private val PC = p(0.5, 0.5)

private val NODES1 = listOf<Long>(1, 2, 3, 4, 1)

private val POSITIONS1 = ElementPolygonsGeometry(listOf(listOf(P1, P2, P3, P4, P1)), PC)

private var i = 1L

private val mall =
    way(1L, NODES1, mapOf("shop" to "mall")) to POSITIONS1

private fun shopWithoutLevel(pos: LatLon = PC) =
    node(1L, pos, mapOf("shop" to "clothes")) to ElementPointGeometry(pos)

private fun thingWithLevel(level: String, pos: LatLon = PC) =
    node(++i, pos, mapOf("level" to level)) to ElementPointGeometry(pos)
