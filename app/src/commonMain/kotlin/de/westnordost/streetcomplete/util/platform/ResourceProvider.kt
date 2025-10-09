package de.westnordost.streetcomplete.util.platform

expect class ResourceProvider {
    fun getIconResId(elementType: String?, elementTypeIcon: String?): Int
    fun getStringResId(elementType: String?): Int
}

expect interface PlatformQuest
