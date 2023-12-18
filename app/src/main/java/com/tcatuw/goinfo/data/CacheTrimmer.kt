package com.tcatuw.goinfo.data

import com.tcatuw.goinfo.data.osm.mapdata.MapDataController
import com.tcatuw.goinfo.data.quest.VisibleQuestsSource

class CacheTrimmer(
    private val visibleQuestsSource: VisibleQuestsSource,
    private val mapDataController: MapDataController,
) {
    fun trimCaches() {
        mapDataController.trimCache()
        visibleQuestsSource.trimCache()
    }

    fun clearCaches() {
        mapDataController.clearCache()
        visibleQuestsSource.clearCache()
    }
}
