package de.westnordost.streetcomplete.data.user.achievements

import de.westnordost.streetcomplete.quests.sidewalk_long_form.AddGenericLong
import org.koin.core.qualifier.named
import org.koin.dsl.module

val editTypeAliasesModule = module {
    factory(named("TypeAliases")) { typeAliases }
}

// list of (quest) synonyms (this alternate name is mentioned to aid searching for this code)
private val typeAliases = listOf(
    "AddGenericLong" to AddGenericLong::class.simpleName!!
)
