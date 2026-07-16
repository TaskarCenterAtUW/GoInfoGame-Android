package de.westnordost.streetcomplete.data.osm.edits.create_feature

import de.westnordost.streetcomplete.data.ObjectTypeRegistry
import de.westnordost.streetcomplete.data.osm.edits.ElementEditType

/** Registry for the edit type(s) used when creating new features from workspace-defined
 *  feature presets. Kept separate from the quest and overlay registries so the type is always
 *  registered, independent of the workspace's long-form definition - a queued edit must be able
 *  to rehydrate from the database even if the preset it was created from no longer exists. */
class CreateFeatureRegistry(ordinalsAndEntries: List<Pair<Int, ElementEditType>>) :
    ObjectTypeRegistry<ElementEditType>(ordinalsAndEntries.toMutableList())
