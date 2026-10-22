package de.westnordost.streetcomplete.data.download.tiles

import de.westnordost.streetcomplete.ApplicationConstants
import de.westnordost.streetcomplete.util.Listeners
import de.westnordost.streetcomplete.util.ktx.nowAsEpochMilliseconds
import kotlin.concurrent.Volatile

class DownloadedTilesController(
    private val dao: DownloadedTilesDao
) : DownloadedTilesSource {

    private val listeners = Listeners<DownloadedTilesSource.Listener>()

    /** Goes up whenever all tiles are invalidated or cleared (workspace switched, new app version
     *  etc.) - a download that was already running when this changed fetches data that is no
     *  longer wanted, see [Downloader]. Not for a single invalidated tile (after an upload), for
     *  which the rest of a running download is still good. */
    @Volatile var invalidationCount: Int = 0
        private set

    override fun contains(tilesRect: TilesRect, ignoreOlderThan: Long): Boolean =
        dao.contains(tilesRect, ignoreOlderThan)

    override fun getAll(ignoreOlderThan: Long): List<TilePos> =
        dao.getAll(ignoreOlderThan)

    fun put(tilesRect: TilesRect) {
        dao.put(tilesRect)
        onUpdated()
    }

    fun clear() {
        invalidationCount++
        dao.deleteAll()
        onUpdated()
    }

    fun invalidate(tilePos: TilePos) {
        dao.updateTimeNewerThan(tilePos, getOldTime())
        onUpdated()
    }

    fun invalidateAll() {
        invalidationCount++
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
