package com.tcatuw.goinfo.data.user

interface UserDataSource {
    interface Listener {
        fun onUpdated()
    }

    val userId: Long
    val userName: String?
    val unreadMessagesCount: Int

    fun addListener(listener: Listener)
    fun removeListener(listener: Listener)
}
