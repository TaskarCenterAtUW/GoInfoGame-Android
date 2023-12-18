package com.tcatuw.goinfo.data.download.tiles

import com.tcatuw.goinfo.ApplicationConstants
import com.tcatuw.goinfo.util.Listeners
import com.tcatuw.goinfo.util.ktx.nowAsEpochMilliseconds

class DownloadedTilesController(
    private val dao: DownloadedTilesDao
) : DownloadedTilesSource {

    private val listeners = Listeners<DownloadedTilesSource.Listener>()

    override fun contains(tilesRect: TilesRect, ignoreOlderThan: Long): Boolean =
        dao.contains(tilesRect, ignoreOlderThan)

    override fun getAll(ignoreOlderThan: Long): List<TilePos> =
        dao.getAll(ignoreOlderThan)

    fun put(tilesRect: TilesRect) {
        dao.put(tilesRect)
        onUpdated()
    }

    fun clear() {
        dao.deleteAll()
        onUpdated()
    }

    fun invalidate(tilePos: TilePos) {
        dao.updateTimeNewerThan(tilePos, getOldTime())
        onUpdated()
    }

    fun invalidateAll() {
        dao.updateAllTimesNewerThan(getOldTime())
        onUpdated()
    }

    fun deleteOlderThan(time: Long) {
        dao.deleteOlderThan(time)
        onUpdated()
    }

    private fun getOldTime() =
        nowAsEpochMilliseconds() - ApplicationConstants.REFRESH_DATA_AFTER - 1

    override fun addListener(listener: DownloadedTilesSource.Listener) {
        listeners.add(listener)
    }
    override fun removeListener(listener: DownloadedTilesSource.Listener) {
        listeners.remove(listener)
    }

    private fun onUpdated() {
        listeners.forEach { it.onUpdated() }
    }
}
