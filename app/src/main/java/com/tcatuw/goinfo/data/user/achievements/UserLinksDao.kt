package com.tcatuw.goinfo.data.user.achievements

import com.tcatuw.goinfo.data.Database
import com.tcatuw.goinfo.data.user.achievements.UserLinksTable.Columns.LINK
import com.tcatuw.goinfo.data.user.achievements.UserLinksTable.NAME

/** Stores which link ids have been unlocked by the user */
class UserLinksDao(private val db: Database) {

    fun getAll(): List<String> =
        db.query(NAME) { it.getString(LINK) }

    fun clear() {
        db.delete(NAME)
    }

    fun add(link: String) {
        db.insertOrIgnore(NAME, listOf(LINK to link))
    }

    fun addAll(links: List<String>) {
        if (links.isEmpty()) return
        db.insertOrIgnoreMany(NAME,
            arrayOf(LINK),
            links.map { arrayOf(it) }
        )
    }
}
