package com.tcatuw.goinfo.data.osm.edits.move

import com.tcatuw.goinfo.data.osm.edits.ElementEditAction
import com.tcatuw.goinfo.data.osm.edits.ElementIdProvider
import com.tcatuw.goinfo.data.osm.edits.IsActionRevertable
import com.tcatuw.goinfo.data.osm.edits.update_tags.isGeometrySubstantiallyDifferent
import com.tcatuw.goinfo.data.osm.mapdata.ElementKey
import com.tcatuw.goinfo.data.osm.mapdata.LatLon
import com.tcatuw.goinfo.data.osm.mapdata.MapDataChanges
import com.tcatuw.goinfo.data.osm.mapdata.MapDataRepository
import com.tcatuw.goinfo.data.osm.mapdata.Node
import com.tcatuw.goinfo.data.osm.mapdata.key
import com.tcatuw.goinfo.data.upload.ConflictException
import com.tcatuw.goinfo.util.ktx.nowAsEpochMilliseconds
import kotlinx.serialization.Serializable

/** Action that moves a node. */
@Serializable
data class MoveNodeAction(
    val originalNode: Node,
    val position: LatLon
) : ElementEditAction, IsActionRevertable {

    override val elementKeys get() = listOf(originalNode.key)

    override fun idsUpdatesApplied(updatedIds: Map<ElementKey, Long>) = copy(
        originalNode = originalNode.copy(id = updatedIds[originalNode.key] ?: originalNode.id)
    )

    override fun createUpdates(
        mapDataRepository: MapDataRepository,
        idProvider: ElementIdProvider
    ): MapDataChanges {
        val currentNode = mapDataRepository.getNode(originalNode.id)
            ?: throw ConflictException("Element deleted")
        val node = currentNode as? Node ?: throw ConflictException("Element deleted")

        if (isGeometrySubstantiallyDifferent(originalNode, currentNode)) {
            throw ConflictException("Element geometry changed substantially")
        }

        return MapDataChanges(modifications = listOf(node.copy(
            position = position,
            timestampEdited = nowAsEpochMilliseconds()
        )))
    }

    override fun createReverted(idProvider: ElementIdProvider) =
        RevertMoveNodeAction(originalNode)
}
