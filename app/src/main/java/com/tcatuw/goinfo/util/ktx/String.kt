package com.tcatuw.goinfo.util.ktx

fun String.truncate(length: Int): String =
    if (this.length > length) substring(0, length - 1) + "…" else this

fun String.containsAll(words: List<String>) = words.all { this.contains(it) }
