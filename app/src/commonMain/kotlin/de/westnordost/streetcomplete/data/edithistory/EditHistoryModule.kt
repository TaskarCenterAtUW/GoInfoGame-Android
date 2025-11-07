package de.westnordost.streetcomplete.data.edithistory

import org.koin.dsl.module

val editHistoryModule = module {
    single<EditHistorySource> { get<EditHistoryController>() }
    factory { EditHistoryController(get(), get(), get(), get(), get(), get(), get()) }
}
