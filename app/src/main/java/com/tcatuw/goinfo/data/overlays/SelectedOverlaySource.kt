package com.tcatuw.goinfo.data.overlays

import com.tcatuw.goinfo.overlays.Overlay

interface SelectedOverlaySource {
    interface Listener {
        fun onSelectedOverlayChanged()
    }

    val selectedOverlay: Overlay?

    fun addListener(listener: Listener)
    fun removeListener(listener: Listener)
}
