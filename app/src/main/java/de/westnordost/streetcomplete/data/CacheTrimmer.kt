package de.westnordost.streetcomplete.data

import de.westnordost.streetcomplete.data.osm.mapdata.MapDataController
import de.westnordost.streetcomplete.data.preferences.Preferences
import de.westnordost.streetcomplete.data.quest.VisibleQuestsSource

/** Trims caches in case the memory becomes scarce */
class CacheTrimmer(
    private val preferences: Preferences,
    private val visibleQuestsSource: VisibleQuestsSource,
    private val mapDataController: MapDataController,
) {
    fun trimCaches() {
        preferences.workspaceId?.let {
            mapDataController.trimCache()
            visibleQuestsSource.trimCache()
        }
    }

    fun clearCaches() {
        preferences.workspaceId?.let {
            mapDataController.clearCache()
            visibleQuestsSource.clearCache()
        }
    }
}
