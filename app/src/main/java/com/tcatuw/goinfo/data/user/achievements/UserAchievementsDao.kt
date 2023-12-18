package com.tcatuw.goinfo.data.user.achievements

import com.tcatuw.goinfo.data.Database
import com.tcatuw.goinfo.data.user.achievements.UserAchievementsTable.Columns.ACHIEVEMENT
import com.tcatuw.goinfo.data.user.achievements.UserAchievementsTable.Columns.LEVEL
import com.tcatuw.goinfo.data.user.achievements.UserAchievementsTable.NAME

/** Stores which achievement ids have been unlocked by the user and at which level */
class UserAchievementsDao(private val db: Database) {

    fun getAll(): Map<String, Int> =
        db.query(NAME) { it.getString(ACHIEVEMENT) to it.getInt(LEVEL) }.toMap()

    fun clear() {
        db.delete(NAME)
    }

    fun put(achievement: String, level: Int) {
        db.replace(NAME, listOf(
            ACHIEVEMENT to achievement,
            LEVEL to level
        ))
    }
}
