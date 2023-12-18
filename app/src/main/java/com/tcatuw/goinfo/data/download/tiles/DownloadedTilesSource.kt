package com.tcatuw.goinfo.data.download.tiles

interface DownloadedTilesSource {
    interface Listener {
        fun onUpdated()
    }

    fun contains(tilesRect: TilesRect, ignoreOlderThan: Long): Boolean

    fun getAll(ignoreOlderThan: Long): List<TilePos>

    fun addListener(listener: Listener)
    fun removeListener(listener: Listener)
}
