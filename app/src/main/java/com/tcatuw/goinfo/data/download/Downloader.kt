package com.tcatuw.goinfo.data.download

import android.util.Log
import com.tcatuw.goinfo.ApplicationConstants
import com.tcatuw.goinfo.data.download.tiles.DownloadedTilesController
import com.tcatuw.goinfo.data.download.tiles.TilesRect
import com.tcatuw.goinfo.data.maptiles.MapTilesDownloader
import com.tcatuw.goinfo.data.osm.mapdata.MapDataDownloader
import com.tcatuw.goinfo.data.osmnotes.NotesDownloader
import com.tcatuw.goinfo.util.ktx.format
import com.tcatuw.goinfo.util.ktx.nowAsEpochMilliseconds
import com.tcatuw.goinfo.util.math.area
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.max

/** Downloads all the things */
class Downloader(
    private val notesDownloader: NotesDownloader,
    private val mapDataDownloader: MapDataDownloader,
    private val mapTilesDownloader: MapTilesDownloader,
    private val downloadedTilesController: DownloadedTilesController,
    private val mutex: Mutex
) {
    suspend fun download(tiles: TilesRect, ignoreCache: Boolean) {
        val bbox = tiles.asBoundingBox(ApplicationConstants.DOWNLOAD_TILE_ZOOM)
        val bboxString = "${bbox.min.latitude.format(7)},${bbox.min.longitude.format(7)},${bbox.max.latitude.format(7)},${bbox.max.longitude.format(7)}"
        val sqkm = (bbox.area() / 1000 / 1000).format(1)

        if (!ignoreCache && hasDownloadedAlready(tiles)) {
            Log.i(TAG, "Not downloading ($sqkm km², bbox: $bboxString), data still fresh")
            return
        }
        Log.i(TAG, "Starting download ($sqkm km², bbox: $bboxString)")

        val time = nowAsEpochMilliseconds()

        mutex.withLock {
            coroutineScope {
                // all downloaders run concurrently
                launch { notesDownloader.download(bbox) }
                launch { mapDataDownloader.download(bbox) }
                launch { mapTilesDownloader.download(bbox) }
            }
        }
        putDownloadedAlready(tiles)

        val seconds = (nowAsEpochMilliseconds() - time) / 1000.0
        Log.i(TAG, "Finished download ($sqkm km², bbox: $bboxString) in ${seconds.format(1)}s")
    }

    private fun hasDownloadedAlready(tiles: TilesRect): Boolean {
        val freshTime = ApplicationConstants.REFRESH_DATA_AFTER
        val ignoreOlderThan = max(0, nowAsEpochMilliseconds() - freshTime)
        return downloadedTilesController.contains(tiles, ignoreOlderThan)
    }

    private fun putDownloadedAlready(tiles: TilesRect) {
        downloadedTilesController.put(tiles)
    }

    companion object {
        private const val TAG = "Download"
    }
}
