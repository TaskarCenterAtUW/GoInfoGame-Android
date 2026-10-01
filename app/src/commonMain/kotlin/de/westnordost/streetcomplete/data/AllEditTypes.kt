package de.westnordost.streetcomplete.data

import de.westnordost.streetcomplete.data.osm.edits.EditType
import kotlinx.atomicfu.locks.ReentrantLock
import kotlinx.atomicfu.locks.withLock

class AllEditTypes(
    var registries: MutableList<ObjectTypeRegistry<out EditType>>
) : AbstractCollection<EditType>() {

    private var byName = registries.flatten().associateByTo(LinkedHashMap()) { it.name }

    /** Resolves names that are no longer in any registry, e.g. edits stored for a long-form
     *  element type that was later renamed or removed from the workspace's long form. Without it,
     *  loading such an edit from the DB would crash. */
    var unknownTypeFallback: ((String) -> EditType)? = null
    private val fallbacksByName = HashMap<String, EditType>()
    private val fallbacksLock = ReentrantLock()

    override val size: Int get() = byName.size

    override fun iterator(): Iterator<EditType> = byName.values.iterator()

    fun getByName(typeName: String): EditType? =
        byName[typeName] ?: unknownTypeFallback?.let { fallback ->
            fallbacksLock.withLock { fallbacksByName.getOrPut(typeName) { fallback(typeName) } }
        }

    fun updateByName() {
        byName.clear()
        byName = registries.flatten().associateByTo(byName) { it.name }
    }
}
