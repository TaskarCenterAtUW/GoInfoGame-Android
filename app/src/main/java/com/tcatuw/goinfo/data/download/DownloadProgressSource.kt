package com.tcatuw.goinfo.data.download

interface DownloadProgressSource {
    val isPriorityDownloadInProgress: Boolean
    val isDownloadInProgress: Boolean

    fun addDownloadProgressListener(listener: DownloadProgressListener)
    fun removeDownloadProgressListener(listener: DownloadProgressListener)
}
