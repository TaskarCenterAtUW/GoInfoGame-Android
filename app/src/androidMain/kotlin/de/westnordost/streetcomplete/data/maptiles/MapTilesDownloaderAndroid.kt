package de.westnordost.streetcomplete.data.maptiles

import android.content.Context
import de.westnordost.streetcomplete.ApplicationConstants
import de.westnordost.streetcomplete.data.osm.mapdata.BoundingBox
import de.westnordost.streetcomplete.screens.main.map.maplibre.awaitCreateOfflineRegion
import de.westnordost.streetcomplete.screens.main.map.maplibre.awaitDownload
import de.westnordost.streetcomplete.screens.main.map.maplibre.awaitGetOfflineRegions
import de.westnordost.streetcomplete.screens.main.map.maplibre.awaitGetStatus
import de.westnordost.streetcomplete.screens.main.map.maplibre.awaitResetDatabase
import de.westnordost.streetcomplete.screens.main.map.maplibre.toLatLng
import de.westnordost.streetcomplete.util.ktx.format
import de.westnordost.streetcomplete.util.ktx.nowAsEpochMilliseconds
import de.westnordost.streetcomplete.util.logs.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import org.maplibre.android.MapLibre
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.offline.OfflineTilePyramidRegionDefinition

class MapTilesDownloaderAndroid(private val context: Context) : MapTilesDownloader {

    init {
        try {
            // must be called before getting OfflineManager instance but will throw an exception if
            // not called from the main thread
            MapLibre.getInstance(context)
        } catch (_: Exception) { }
    }

    override suspend fun clear() {
        try {
            OfflineManager.getInstance(context).awaitResetDatabase()
        } catch (e: Exception) {
            Log.w(TAG, e.message.orEmpty(), e)
        }
    }

    override suspend fun download(bbox: BoundingBox) = withContext(Dispatchers.IO) {
        val bounds = LatLngBounds.fromLatLngs(listOf(bbox.max.toLatLng(), bbox.min.toLatLng()))
        val pixelRatio = context.resources.displayMetrics.density
        val regionDefinition = OfflineTilePyramidRegionDefinition(styleUrl, bounds, 0.0, 16.0, pixelRatio)

        // store timestamp as metadata for deleting areas older than
        // (could also be done directly from the long)
        val metadata = nowAsEpochMilliseconds().toString().toByteArray(Charsets.UTF_8)
        try {
            // the map tiles don't depend on the workspace - when only the map data needs to be
            // downloaded again (e.g. workspace switched), don't fetch the same tiles again
            if (hasFreshTiles(bounds)) {
                Log.i(TAG, "Not downloading tiles, already downloaded recently")
                return@withContext
            }
            val offlineRegion = OfflineManager.getInstance(context).awaitCreateOfflineRegion(regionDefinition, metadata)
            val time = nowAsEpochMilliseconds()
            val status = offlineRegion.awaitDownload()
            val seconds = (nowAsEpochMilliseconds() - time) / 1000.0
            Log.i(TAG, "Downloaded ${status.completedTileCount} tiles (${status.completedTileSize / 1000}kB) in ${seconds.format(1)}s")
            // note that the numbers include tiles that were already on device
            //  no idea how to check which tiles were really downloaded (other than in android log for MapLibre)
            // status.requiredResourceCount and status.completedResourceSize might be interesting too
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, e.message.orEmpty(), e)
        }
    }

    /** Whether a completed download of the same map style within [REFRESH_TILES_AFTER] covers
     *  [bounds]. Its timestamp is stored when the region is created, so whether it completed
     *  (rather than failed or was cancelled) is checked separately. */
    private suspend fun hasFreshTiles(bounds: LatLngBounds): Boolean {
        val freshAfter = nowAsEpochMilliseconds() - REFRESH_TILES_AFTER
        return OfflineManager.getInstance(context).awaitGetOfflineRegions().any { region ->
            val timestamp = region.metadata.toString(Charsets.UTF_8).toLongOrNull() ?: 0
            val definition = region.definition as? OfflineTilePyramidRegionDefinition
            timestamp > freshAfter
                && definition?.styleURL == styleUrl
                && definition.bounds?.contains(bounds) == true
                && region.awaitGetStatus().isComplete
        }
    }

    companion object {
        private const val TAG = "MapTilesDownload"

        /** The base map hardly changes - much less often than the map data
         *  ([ApplicationConstants.REFRESH_DATA_AFTER]), and well before the downloaded regions are
         *  deleted ([ApplicationConstants.DELETE_OLD_DATA_AFTER]) */
        private const val REFRESH_TILES_AFTER = 7L * 24 * 60 * 60 * 1000 // 7 days in ms

        private const val styleUrl = "https://streetcomplete.app/map-jawg/streetcomplete.json"
    }
}
