package com.tcatuw.goinfo.data.osm.edits.update_tags

import com.tcatuw.goinfo.data.osm.edits.ElementEditAction
import com.tcatuw.goinfo.data.osm.edits.ElementIdProvider
import com.tcatuw.goinfo.data.osm.edits.IsActionRevertable
import com.tcatuw.goinfo.data.osm.mapdata.Element
import com.tcatuw.goinfo.data.osm.mapdata.ElementKey
import com.tcatuw.goinfo.data.osm.mapdata.MapDataChanges
import com.tcatuw.goinfo.data.osm.mapdata.MapDataRepository
import com.tcatuw.goinfo.data.osm.mapdata.key
import com.tcatuw.goinfo.data.upload.ConflictException
import com.tcatuw.goinfo.util.ktx.copy
import kotlinx.serialization.Serializable

/** Action that updates the tags on an element.
 *
 *  The tag updates are passed in as a diff to be more robust when handling conflicts.
 *
 *  The original element is passed in in order to decide if an updated element is still compatible
 *  with the action: Basically, if the geometry changed significantly, there is a possibility that
 *  the tag update made may not be correct anymore, so that is considered a conflict.
 *  */
@Serializable
data class UpdateElementTagsAction(
    val originalElement: Element,
    val changes: StringMapChanges
) : ElementEditAction, IsActionRevertable {

    override val elementKeys get() = listOf(originalElement.key)

    override fun idsUpdatesApplied(updatedIds: Map<ElementKey, Long>) = copy(
        originalElement = originalElement.copy(id = updatedIds[originalElement.key] ?: originalElement.id)
    )

    override fun createUpdates(
        mapDataRepository: MapDataRepository,
        idProvider: ElementIdProvider
    ): MapDataChanges {
        val currentElement = mapDataRepository.get(originalElement.type, originalElement.id)
            ?: throw ConflictException("Element deleted")

        if (isGeometrySubstantiallyDifferent(originalElement, currentElement)) {
            throw ConflictException("Element geometry changed substantially")
        }

        return MapDataChanges(modifications = listOf(currentElement.changesApplied(changes)))
    }

    override fun createReverted(idProvider: ElementIdProvider) =
        RevertUpdateElementTagsAction(originalElement, changes.reversed())
}
