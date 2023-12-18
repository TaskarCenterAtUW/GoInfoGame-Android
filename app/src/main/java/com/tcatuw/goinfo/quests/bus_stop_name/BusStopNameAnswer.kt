package com.tcatuw.goinfo.quests.bus_stop_name

import com.tcatuw.goinfo.osm.LocalizedName

sealed interface BusStopNameAnswer

object NoBusStopName : BusStopNameAnswer
data class BusStopName(val localizedNames: List<LocalizedName>) : BusStopNameAnswer
