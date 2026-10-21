package de.westnordost.streetcomplete.data.download

import de.westnordost.streetcomplete.data.download.tiles.DownloadedTilesController
import de.westnordost.streetcomplete.data.download.tiles.DownloadedTilesDao
import de.westnordost.streetcomplete.data.download.tiles.TilePos
import de.westnordost.streetcomplete.data.maptiles.MapTilesDownloader
import de.westnordost.streetcomplete.data.osm.mapdata.BoundingBox
import de.westnordost.streetcomplete.data.osm.mapdata.MapDataDownloader
import de.westnordost.streetcomplete.testutils.any
import de.westnordost.streetcomplete.testutils.mock
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse

class DownloaderTest {

    private lateinit var dao: DownloadedTilesDao
    private lateinit var downloadedTiles: DownloadedTilesController
    private lateinit var listener: DownloadProgressSource.Listener
    /** runs while the (fake) map tiles download is in progress */
    private var duringDownload: suspend () -> Unit = {}
    private lateinit var downloader: Downloader

    private val bbox = BoundingBox(47.6540, -122.3050, 47.6560, -122.3020)

    @BeforeTest fun setUp() {
        dao = mock()
        downloadedTiles = DownloadedTilesController(dao)
        listener = mock()
        val mapTilesDownloader = object : MapTilesDownloader {
            override suspend fun download(bbox: BoundingBox) { duringDownload() }
            override suspend fun clear() {}
        }
        downloader = Downloader(mock(), mock<MapDataDownloader>(), mapTilesDownloader, downloadedTiles, mock(), Mutex())
        downloader.addListener(listener)
    }

    @Test fun `marks tiles as downloaded`() {
        runBlocking { downloader.download(bbox, false) }

        verify(dao).put(any())
        verify(listener).onSuccess()
    }

    @Test fun `does not mark tiles as downloaded when they were invalidated during the download`() {
        // e.g. the workspace was switched while an auto-download of the previous one was running
        duringDownload = { downloadedTiles.invalidateAll() }

        runBlocking { downloader.download(bbox, false) }

        verify(dao, never()).put(any())
        // so that the auto-syncer checks again - and finds the tiles not downloaded
        verify(listener).onSuccess()
    }

    @Test fun `does not mark tiles as downloaded when they were cleared during the download`() {
        duringDownload = { downloadedTiles.clear() }

        runBlocking { downloader.download(bbox, false) }

        verify(dao, never()).put(any())
    }

    @Test fun `an invalidation before the download does not keep it from marking its tiles`() {
        downloadedTiles.invalidateAll()

        runBlocking { downloader.download(bbox, false) }

        verify(dao).put(any())
    }

    @Test fun `a single invalidated tile does not keep a running download from marking its tiles`() {
        // e.g. the uploader invalidates the tile of an element it found changed - the rest of the
        // downloaded area is still good
        duringDownload = { downloadedTiles.invalidate(TilePos(0, 0)) }

        runBlocking { downloader.download(bbox, false) }

        verify(dao).put(any())
    }

    @Test fun `a hanging download is cancelled when all tiles are invalidated, and reports success`() {
        // e.g. the tile server is unreachable when the workspace is switched
        duringDownload = { awaitCancellation() }

        runBlocking {
            val download = launch { downloader.download(bbox, false) }
            while (!downloader.isDownloadInProgress) yield()
            yield() // let it get to the (hanging) map tiles download

            downloadedTiles.invalidateAll()

            withTimeout(5_000) { download.join() }
        }

        assertFalse(downloader.isDownloadInProgress)
        verify(dao, never()).put(any())
        // so that the auto-syncer checks again and starts a fresh download
        verify(listener).onSuccess()
    }

    @Test fun `a download cancelled from outside is not reported as success`() {
        // e.g. the user cancelled it from the notification
        duringDownload = { awaitCancellation() }

        runBlocking {
            val download = launch { downloader.download(bbox, false) }
            while (!downloader.isDownloadInProgress) yield()
            yield()

            download.cancel()
            withTimeout(5_000) { download.join() }
        }

        assertFalse(downloader.isDownloadInProgress)
        verify(listener, never()).onSuccess()
    }
}
