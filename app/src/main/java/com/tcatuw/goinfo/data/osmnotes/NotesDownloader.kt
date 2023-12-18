package com.tcatuw.goinfo.data.osmnotes

import android.util.Log
import com.tcatuw.goinfo.data.osm.mapdata.BoundingBox
import com.tcatuw.goinfo.util.ktx.format
import com.tcatuw.goinfo.util.ktx.nowAsEpochMilliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

/** Takes care of downloading notes and referenced avatar pictures into persistent storage */
class NotesDownloader(
    private val notesApi: NotesApi,
    private val noteController: NoteController
) {
    suspend fun download(bbox: BoundingBox) = withContext(Dispatchers.IO) {
        val time = nowAsEpochMilliseconds()

        val notes = notesApi
            .getAll(bbox, 10000, 0)
            // exclude invalid notes (#1338)
            .filter { it.comments.isNotEmpty() }

        val seconds = (nowAsEpochMilliseconds() - time) / 1000.0
        Log.i(TAG, "Downloaded ${notes.size} notes in ${seconds.format(1)}s")

        yield()

        noteController.putAllForBBox(bbox, notes)
    }

    companion object {
        private const val TAG = "NotesDownload"
    }
}
