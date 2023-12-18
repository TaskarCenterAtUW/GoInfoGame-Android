package com.tcatuw.goinfo

import com.tcatuw.goinfo.data.elementfilter.toOverpassQLString
import com.tcatuw.goinfo.data.osm.osmquests.OsmElementQuestType
import com.tcatuw.goinfo.data.osm.osmquests.OsmFilterQuestType
import com.tcatuw.goinfo.quests.questTypeRegistry
import com.tcatuw.goinfo.testutils.mock

fun main() {

    val registry = questTypeRegistry(mock(), mock(), mock(), mock(), mock())

    for (questType in registry) {
        if (questType is OsmElementQuestType<*>) {
            println("### " + questType.name)
            if (questType is OsmFilterQuestType<*>) {
                val query = "[bbox:{{bbox}}];\n" + questType.filter.toOverpassQLString() + "\n out meta geom;"
                println("```\n$query\n```")
            } else {
                println("Not available, see source code")
            }
            println()
        }
    }
}
