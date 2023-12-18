package com.tcatuw.goinfo.data.edithistory

import org.koin.dsl.module

val editHistoryModule = module {
    single<EditHistorySource> { get<EditHistoryController>() }
    single { EditHistoryController(get(), get(), get(), get()) }
}
