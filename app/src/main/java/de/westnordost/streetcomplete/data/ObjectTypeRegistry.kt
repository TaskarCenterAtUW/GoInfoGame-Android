package de.westnordost.streetcomplete.data

import de.westnordost.streetcomplete.data.quest.Quest
import de.westnordost.streetcomplete.quests.sidewalk_long_form.AddGenericLong

/** A class where objects of a certain type are
 *  1. registered and can be recalled by class name
 *  2. or recalled by ordinal
 *  3. or iterated in the order as specified in the constructor
 */
open class ObjectTypeRegistry<T>(var ordinalsAndEntries: MutableList<Pair<Int, T & Any>>) : AbstractList<T>() {

    private lateinit var byName: Map<String, T>
    private lateinit var byOrdinal: Map<Int, T>
    private lateinit var ordinalByObject: Map<T, Int>
    private var objects = ordinalsAndEntries.map { it.second }.toMutableList()

    init {
        initFunc(ordinalsAndEntries)
    }

    private fun initFunc(ordinalsAndEntries: List<Pair<Int, T & Any>>) {
        val byNameMap = mutableMapOf<String, T>()
        val highestOrdinal = ordinalsAndEntries.maxBy { it.first }.first
        val byOrdinalMap = HashMap<Int, T>(highestOrdinal + 1)
        for ((ordinal, objectType) in ordinalsAndEntries) {
            val typeName = if (objectType is AddGenericLong){
                objectType.name
            }else{
                objectType::class.simpleName!!
            }
            // require(!byNameMap.containsKey(typeName)) {
            //     "A object type's name must be unique! \"$typeName\" is defined twice!"
            // }
            require(!byOrdinalMap.containsKey(ordinal)) {
                val otherTypeName = byOrdinalMap[ordinal]!!::class.simpleName!!
                "Duplicate ordinal for \"$typeName\" and \"$otherTypeName\""
            }
            byNameMap[typeName] = objectType
            byOrdinalMap[ordinal] = objectType
        }
        ordinalByObject = ordinalsAndEntries.associate { it.second to it.first }
        byName = byNameMap
        byOrdinal = byOrdinalMap
    }

    fun getByName(typeName: String): T? = byName[typeName]

    fun getByOrdinal(ordinal: Int): T? = byOrdinal[ordinal]

    fun getOrdinalOf(type: T): Int? = ordinalByObject[type]

    fun addItem(item: List<Pair<Int, T & Any>>) {
        objects.clear()
        ordinalsAndEntries.clear()
        ordinalsAndEntries.addAll(item)
        objects = ordinalsAndEntries.map { it.second }.toMutableList()
        initFunc(ordinalsAndEntries)
    }

    fun clearAll() {
        objects.clear()
    }

    override val size: Int get() = objects.size
    override fun get(index: Int): T = objects[index]
}
