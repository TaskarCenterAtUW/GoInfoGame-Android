package com.tcatuw.goinfo.data.osmnotes.notequests

import org.koin.dsl.module

val osmNoteQuestModule = module {
    factory { NoteQuestsHiddenDao(get()) }

    single { NotesPreferences(get()) }
    single<OsmNoteQuestSource> { get<OsmNoteQuestController>() }
    single { OsmNoteQuestController(get(), get(), get(), get(), get()) }
}
