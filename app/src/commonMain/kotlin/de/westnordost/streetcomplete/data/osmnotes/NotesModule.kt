package de.westnordost.streetcomplete.data.osmnotes

import org.koin.core.qualifier.named
import org.koin.dsl.module

val notesModule = module {
    factory { AvatarsDownloader(get(), get(), get(), get(named("AvatarsCacheDirectory"))) }
    factory { AvatarsInNotesUpdater(get()) }
    factory { NoteDao(get(), get()) }
    factory { NotesDownloader(get(), get()) }

    single {
        NoteController(get()).apply {
            // on notes have been updated, avatar images should be downloaded (cached) referenced in note discussions
            addListener(get<AvatarsInNotesUpdater>())
        }
    }
}
