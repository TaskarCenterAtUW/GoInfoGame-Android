package de.westnordost.streetcomplete.data.user.statistics

import de.westnordost.streetcomplete.data.ApplicationDbTestCase
import de.westnordost.streetcomplete.data.osm.testutils.mock
import de.westnordost.streetcomplete.data.preferences.Preferences
import org.mockito.Mockito
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class EditTypeStatisticsDaoTest : ApplicationDbTestCase() {
    private lateinit var daoType: EditTypeStatisticsDao
    private lateinit var preferences: Preferences

    @BeforeTest fun createDao() {
        val workspaceId  = 301
        preferences = mock()
        Mockito.`when`(preferences.workspaceId).thenReturn(workspaceId)
        daoType = EditTypeStatisticsDao(database, EditTypeStatisticsTables.NAME, preferences)
    }

    @Test fun getZero() {
        assertEquals(0, daoType.getAmount(ONE))
    }

    @Test fun getOne() {
        daoType.addOne(ONE)
        assertEquals(1, daoType.getAmount(ONE))
    }

    @Test fun getTwo() {
        daoType.addOne(ONE)
        daoType.addOne(ONE)
        assertEquals(2, daoType.getAmount(ONE))
    }

    @Test fun getTotal() {
        daoType.addOne(ONE)
        daoType.addOne(ONE)
        daoType.addOne(TWO)
        assertEquals(3, daoType.getTotalAmount())
    }

    @Test fun subtract() {
        daoType.addOne(ONE)
        daoType.subtractOne(ONE)
        assertEquals(0, daoType.getAmount(ONE))
    }

    @Test fun getAmountOfSeveral() {
        daoType.addOne(ONE)
        daoType.addOne(ONE)
        daoType.addOne(TWO)
        daoType.addOne(THREE)
        assertEquals(3, daoType.getAmount(listOf(ONE, TWO)))
    }

    @Test fun replaceAll() {
        daoType.addOne(ONE)
        daoType.addOne(TWO)
        daoType.replaceAll(mapOf(
            ONE to 4,
            THREE to 1
        ))
        assertEquals(4, daoType.getAmount(ONE))
        assertEquals(0, daoType.getAmount(TWO))
        assertEquals(1, daoType.getAmount(THREE))
    }

    @Test fun getAll() {
        daoType.addOne(ONE)
        daoType.addOne(ONE)
        daoType.addOne(TWO)
        assertEquals(listOf(
            EditTypeStatistics(ONE, 2),
            EditTypeStatistics(TWO, 1)
        ), daoType.getAll())
    }
}

private const val ONE = "one"
private const val TWO = "two"
private const val THREE = "three"
