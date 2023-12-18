package com.tcatuw.goinfo.data.osm.edits.move

import com.tcatuw.goinfo.data.osm.edits.ElementEditAction
import com.tcatuw.goinfo.data.osm.edits.ElementIdProvider
import com.tcatuw.goinfo.data.osm.edits.IsRevertAction
import com.tcatuw.goinfo.data.osm.mapdata.ElementKey
import com.tcatuw.goinfo.data.osm.mapdata.MapDataChanges
import com.tcatuw.goinfo.data.osm.mapdata.MapDataRepository
import com.tcatuw.goinfo.data.osm.mapdata.Node
import com.tcatuw.goinfo.data.osm.mapdata.key
import com.tcatuw.goinfo.data.upload.ConflictException
import com.tcatuw.goinfo.util.ktx.nowAsEpochMilliseconds
import kotlinx.serialization.Serializable

/** Action reverts moving a node. */
@Serializable
data class RevertMoveNodeAction(
    val originalNode: Node,
) : ElementEditAction, IsRevertAction {

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

        return MapDataChanges(modifications = listOf(currentNode.copy(
            position = originalNode.position,
            timestampEdited = nowAsEpochMilliseconds()
        )))
    }
}
