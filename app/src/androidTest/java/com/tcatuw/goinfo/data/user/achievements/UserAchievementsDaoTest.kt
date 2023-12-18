package com.tcatuw.goinfo.data.user.achievements

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class UserAchievementsDaoTest : com.tcatuw.goinfo.data.ApplicationDbTestCase() {
    private lateinit var dao: UserAchievementsDao

    @BeforeTest fun createDao() {
        dao = UserAchievementsDao(database)
    }

    @Test fun putGetAll() {
        dao.put(ONE, 1)
        dao.put(ONE, 4)
        dao.put(TWO, 2)
        assertEquals(mapOf(
            ONE to 4,
            TWO to 2
        ), dao.getAll())
    }
}

private const val ONE = "one"
private const val TWO = "two"
