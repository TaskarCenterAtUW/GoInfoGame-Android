package com.tcatuw.goinfo.quests.summit

import com.tcatuw.goinfo.data.elementfilter.toElementFilterExpression
import com.tcatuw.goinfo.data.osm.geometry.ElementPolylinesGeometry
import com.tcatuw.goinfo.data.osm.mapdata.MapDataWithGeometry

private val hikingPathsFilter by lazy { """
    ways with
      highway = path
      and sac_scale ~ mountain_hiking|demanding_mountain_hiking|alpine_hiking|demanding_alpine_hiking|difficult_alpine_hiking
""".toElementFilterExpression() }

private fun getHikingPaths(mapData: MapDataWithGeometry) =
    mapData.ways.filter { hikingPathsFilter.matches(it) }
        .mapNotNull { mapData.getWayGeometry(it.id) as? ElementPolylinesGeometry }

private fun getHikingRoutes(mapData: MapDataWithGeometry) =
    mapData.relations.filter { it.tags["route"] == "hiking" }
        .mapNotNull { mapData.getRelationGeometry(it.id) as? ElementPolylinesGeometry }

fun getHikingPathsAndRoutes(mapData: MapDataWithGeometry) =
    getHikingPaths(mapData) + getHikingRoutes(mapData)
