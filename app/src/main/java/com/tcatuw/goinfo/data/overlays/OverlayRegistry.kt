package com.tcatuw.goinfo.data.overlays

import com.tcatuw.goinfo.data.ObjectTypeRegistry
import com.tcatuw.goinfo.overlays.Overlay

/** Every overlay must be registered here
 *
 * Could theoretically be done with Reflection, but that doesn't really work on Android.
 *
 * It is also used to assign each overlay an ordinal for serialization.
 */
class OverlayRegistry(ordinalsAndEntries: List<Pair<Int, Overlay>>) : ObjectTypeRegistry<Overlay>(ordinalsAndEntries)
