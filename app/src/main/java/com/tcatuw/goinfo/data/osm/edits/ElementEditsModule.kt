package com.tcatuw.goinfo.data.osm.edits

import com.tcatuw.goinfo.data.osm.edits.upload.ElementEditUploader
import com.tcatuw.goinfo.data.osm.edits.upload.ElementEditsUploader
import com.tcatuw.goinfo.data.osm.edits.upload.LastEditTimeStore
import com.tcatuw.goinfo.data.osm.edits.upload.changesets.ChangesetAutoCloser
import com.tcatuw.goinfo.data.osm.edits.upload.changesets.ChangesetAutoCloserWorker
import com.tcatuw.goinfo.data.osm.edits.upload.changesets.OpenChangesetsDao
import com.tcatuw.goinfo.data.osm.edits.upload.changesets.OpenChangesetsManager
import org.koin.androidx.workmanager.dsl.worker
import org.koin.dsl.module

val elementEditsModule = module {
    factory { ChangesetAutoCloser(get()) }
    factory { ElementEditUploader(get(), get(), get()) }

    factory { ElementEditsDao(get(), get(), get()) }
    factory { ElementIdProviderDao(get()) }
    factory { LastEditTimeStore(get()) }
    factory { OpenChangesetsDao(get()) }
    factory { EditElementsDao(get()) }

    single { OpenChangesetsManager(get(), get(), get(), get()) }

    single { ElementEditsUploader(get(), get(), get(), get(), get(), get()) }

    single<ElementEditsSource> { get<ElementEditsController>() }
    single { ElementEditsController(get(), get(), get(), get()) }
    single { MapDataWithEditsSource(get(), get(), get()) }

    worker { ChangesetAutoCloserWorker(get(), get(), get()) }
}
