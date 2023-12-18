package com.tcatuw.goinfo.data.messages

import org.koin.dsl.module

val messagesModule = module {
    single { MessagesSource(get(), get(), get(), get()) }
    single { QuestSelectionHintController(get(), get()) }
}
