package com.tcatuw.goinfo.data.user

interface UserLoginStatusSource {
    interface Listener {
        fun onLoggedIn()
        fun onLoggedOut()
    }

    val isLoggedIn: Boolean

    fun addListener(listener: Listener)
    fun removeListener(listener: Listener)
}
