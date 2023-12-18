package com.tcatuw.goinfo.screens

interface HasTitle {
    val title: String
    val subtitle: String? get() = null
}
