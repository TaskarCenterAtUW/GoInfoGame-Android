package de.westnordost.streetcomplete.testutils

import android.util.Log
import de.westnordost.streetcomplete.data.maptiles.MapTilesDownloader
import de.westnordost.streetcomplete.data.osm.mapdata.BoundingBox
import java.util.Collections

/**
 * Stands in for the app's map tiles downloader in all instrumented tests (see
 * [SandboxedTestRunner]): downloading the map tiles for offline use goes to the real tile servers
 * (through MapLibre, so [RealNetworkGuard] doesn't cover it), which made tests slow and flaky - a
 * tile download that took long or hung kept the next map data download from starting. The tiles
 * don't matter for any test, the map data download is what tests check (see MockOsmServer).
 */
object OfflineMapTilesDownloader : MapTilesDownloader {
    const val TAG = "OfflineMapTiles"

    /** The areas the app wanted map tiles for, in order. */
    val downloads: MutableList<BoundingBox> = Collections.synchronizedList(mutableListOf())

    override suspend fun download(bbox: BoundingBox) {
        downloads.add(bbox)
        Log.i(TAG, "Not downloading map tiles for $bbox (offline in tests)")
    }

    override suspend fun clear() {}
}
