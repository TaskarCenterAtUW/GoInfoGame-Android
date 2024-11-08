package de.westnordost.streetcomplete.data

import de.westnordost.streetcomplete.data.osm.edits.EditType

class AllEditTypes(
    var registries: MutableList<ObjectTypeRegistry<out EditType>>
) : AbstractCollection<EditType>() {

    private var byName = registries.flatten().associateByTo(LinkedHashMap()) { it.name }

    override val size: Int get() = byName.size

    override fun iterator(): Iterator<EditType> = byName.values.iterator()

    fun getByName(typeName: String): EditType? = byName[typeName]

    fun updateByName() {
        byName.clear()
        byName = registries.flatten().associateByTo(byName) { it.name }
    }
}
