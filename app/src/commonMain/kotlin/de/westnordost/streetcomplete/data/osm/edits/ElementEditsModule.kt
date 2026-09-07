package de.westnordost.streetcomplete.data.osm.edits

import de.westnordost.streetcomplete.data.osm.edits.create_feature.FeaturePhotosController
import de.westnordost.streetcomplete.data.osm.edits.create_feature.FeaturePhotosDao
import de.westnordost.streetcomplete.data.osm.edits.update_tags.PendingTagConflictsController
import de.westnordost.streetcomplete.data.osm.edits.update_tags.PendingTagConflictsDao
import de.westnordost.streetcomplete.data.osm.edits.upload.ElementEditUploader
import de.westnordost.streetcomplete.data.osm.edits.upload.ElementEditsUploader
import de.westnordost.streetcomplete.data.osm.edits.upload.changesets.OpenChangesetsDao
import de.westnordost.streetcomplete.data.osm.edits.upload.changesets.OpenChangesetsManager
import org.koin.dsl.module

val elementEditsModule = module {
    factory { ElementEditUploader(get(), get(), get(), get(), get()) }

    factory { ElementEditsDao(get(), get(), get()) }
    factory { ElementIdProviderDao(get()) }
    factory { OpenChangesetsDao(get()) }
    factory { EditElementsDao(get()) }
    factory { PendingTagConflictsDao(get(), get(), get()) }
    factory { DiscardedEditNoticesDao(get(), get(), get()) }
    factory { FeaturePhotosDao(get(), get()) }

    single { OpenChangesetsManager(get(), get(), get(), get()) }

    single { ElementEditsUploader(get(), get(), get(), get(), get(), get(), get(), get(), get()) }

    single<ElementEditsSource> { get<ElementEditsController>() }
    single { ElementEditsController(get(), get(), get(), get()) }
    single { MapDataWithEditsSource(get(), get(), get()) }

    /* must be a singleton because there is a listener that should respond to a change in the
     * underlying database table */
    single { PendingTagConflictsController(get(), get()) }
    single { DiscardedEditNoticesController(get()) }
    single { FeaturePhotosController(get(), get(), get()) }
}
