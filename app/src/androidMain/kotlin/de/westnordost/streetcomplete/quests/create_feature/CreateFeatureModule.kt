package de.westnordost.streetcomplete.quests.create_feature

import de.westnordost.streetcomplete.data.osm.edits.create_feature.CreateFeatureRegistry
import org.koin.dsl.module

val createFeatureModule = module {
    single { CreateFeatureRegistry(listOf(0 to AddFeaturePreset)) }
}
