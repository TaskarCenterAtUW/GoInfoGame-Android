package com.tcatuw.goinfo.data.osm.edits.delete

import com.tcatuw.goinfo.data.osm.edits.ElementEditAction
import com.tcatuw.goinfo.data.osm.edits.ElementIdProvider
import com.tcatuw.goinfo.data.osm.edits.IsRevertAction
import com.tcatuw.goinfo.data.osm.edits.NewElementsCount
import com.tcatuw.goinfo.data.osm.mapdata.ElementKey
import com.tcatuw.goinfo.data.osm.mapdata.MapDataChanges
import com.tcatuw.goinfo.data.osm.mapdata.MapDataRepository
import com.tcatuw.goinfo.data.osm.mapdata.Node
import com.tcatuw.goinfo.data.osm.mapdata.key
import com.tcatuw.goinfo.data.upload.ConflictException
import com.tcatuw.goinfo.util.ktx.nowAsEpochMilliseconds
import kotlinx.serialization.Serializable

/** Action that restores a POI node to the previous state before deletion/clearing of tags
 */
@Serializable
data class RevertDeletePoiNodeAction(
    val originalNode: Node
) : ElementEditAction, IsRevertAction {

    /** No "new" elements are created, instead, an old one is being revived */
    override val newElementsCount get() = NewElementsCount(0, 0, 0)

    override val elementKeys get() = listOf(originalNode.key)

    override fun idsUpdatesApplied(updatedIds: Map<ElementKey, Long>) = copy(
        originalNode = originalNode.copy(id = updatedIds[originalNode.key] ?: originalNode.id)
    )

    override fun createUpdates(
        mapDataRepository: MapDataRepository,
        idProvider: ElementIdProvider
    ): MapDataChanges {
        val newVersion = originalNode.version + 1
        val currentNode = mapDataRepository.getNode(originalNode.id)

        // already has been restored apparently
        if (currentNode != null && currentNode.version > newVersion) {
            throw ConflictException("Element has been restored already")
        }

        val restoredNode = originalNode.copy(
            version = newVersion,
            timestampEdited = nowAsEpochMilliseconds()
        )
        return MapDataChanges(modifications = listOf(restoredNode))
    }
}
