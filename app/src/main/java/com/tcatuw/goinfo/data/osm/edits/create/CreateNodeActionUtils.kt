package com.tcatuw.goinfo.data.osm.edits.create

import com.tcatuw.goinfo.data.osm.edits.ElementEditAction
import com.tcatuw.goinfo.data.osm.edits.MapDataWithEditsSource
import com.tcatuw.goinfo.data.osm.edits.update_tags.StringMapChangesBuilder
import com.tcatuw.goinfo.util.math.PositionOnWay
import com.tcatuw.goinfo.util.math.PositionOnWaySegment
import com.tcatuw.goinfo.util.math.VertexOfWay

fun createNodeAction(
    positionOnWay: PositionOnWay,
    mapDataWithEditsSource: MapDataWithEditsSource,
    createChanges: (StringMapChangesBuilder) -> Unit
): ElementEditAction? {
    when (positionOnWay) {
        is PositionOnWaySegment -> {
            val tagChanges = StringMapChangesBuilder(mapOf())
            createChanges(tagChanges)
            val insertIntoWayAt = InsertIntoWayAt(
                positionOnWay.wayId,
                positionOnWay.segment.first,
                positionOnWay.segment.second
            )
            return CreateNodeAction(positionOnWay.position, tagChanges, listOf(insertIntoWayAt))
        }
        is VertexOfWay -> {
            val node = mapDataWithEditsSource.getNode(positionOnWay.nodeId) ?: return null
            val tagChanges = StringMapChangesBuilder(node.tags)
            createChanges(tagChanges)
            val containingWayIds = mapDataWithEditsSource.getWaysForNode(positionOnWay.nodeId).map { it.id }
            return CreateNodeFromVertexAction(node, tagChanges.create(), containingWayIds)
        }
    }
}
