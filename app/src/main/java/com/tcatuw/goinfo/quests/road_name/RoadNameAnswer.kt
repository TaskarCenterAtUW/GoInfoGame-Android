package com.tcatuw.goinfo.quests.road_name

import com.tcatuw.goinfo.data.osm.mapdata.LatLon
import com.tcatuw.goinfo.osm.LocalizedName

sealed interface RoadNameAnswer

data class RoadName(
    val localizedNames: List<LocalizedName>,
    val wayId: Long,
    val wayGeometry: List<LatLon>
) : RoadNameAnswer

object NoRoadName : RoadNameAnswer
object RoadIsServiceRoad : RoadNameAnswer
object RoadIsTrack : RoadNameAnswer
object RoadIsLinkRoad : RoadNameAnswer
