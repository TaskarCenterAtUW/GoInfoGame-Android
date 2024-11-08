package de.westnordost.streetcomplete.data

import de.westnordost.osmapi.OsmConnection
import de.westnordost.osmapi.user.UserApi
import de.westnordost.streetcomplete.ApplicationConstants
import de.westnordost.streetcomplete.data.osm.GIGOsmConnection
import de.westnordost.streetcomplete.data.osm.mapdata.MapDataApi
import de.westnordost.streetcomplete.data.osm.mapdata.MapDataApiImpl
import de.westnordost.streetcomplete.data.osmnotes.NotesApi
import de.westnordost.streetcomplete.data.osmnotes.NotesApiImpl
import de.westnordost.streetcomplete.data.osmtracks.TracksApi
import de.westnordost.streetcomplete.data.osmtracks.TracksApiImpl
import de.westnordost.streetcomplete.data.preferences.Preferences
import de.westnordost.streetcomplete.data.workspace.data.remote.EnvironmentManager
import org.koin.androidx.workmanager.dsl.worker
import org.koin.core.qualifier.named
import org.koin.dsl.module

//private const val OSM_API_URL = "https://osm.workspaces-dev.sidewalks.washington.edu/api/0.6/" //Dev
private const val OSM_API_URL = "https://osm.workspaces-stage.sidewalks.washington.edu/api/0.6/" //Stage
//private const val OSM_API_URL = "https://master.apis.dev.openstreetmap.org/api/0.6/"
//private const val OSM_API_URL = "https://master.apis.dev.openstreetmap.org/api/0.6/"
//https://workspaces-osm-stage.sidewalks.washington.edu/api/0.6/
//https://master.apis.dev.openstreetmap.org/api/0.6/
val osmApiModule = module {
    factory { Cleaner(get(), get(), get(), get(), get(), get()) }
    factory { CacheTrimmer(get(), get()) }
    factory<MapDataApi> { MapDataApiImpl(get()) }
    factory<NotesApi> { NotesApiImpl(get()) }
    factory<TracksApi> { TracksApiImpl(get()) }
    factory { Preloader(get(named("CountryBoundariesLazy")), get(named("FeatureDictionaryLazy"))) }
    factory { UserApi(get()) }

    single<OsmConnection> { GIGOsmConnection(
        EnvironmentManager(get()).currentEnvironment.osmUrl,
        ApplicationConstants.USER_AGENT,
        get<Preferences>()
    ) }
    single { UnsyncedChangesCountSource(get(), get()) }

    worker { CleanerWorker(get(), get(), get()) }
}
