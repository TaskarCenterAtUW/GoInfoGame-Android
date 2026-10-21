package de.westnordost.streetcomplete.testutils

import android.app.Application
import androidx.test.runner.AndroidJUnitRunner
import de.westnordost.streetcomplete.data.maptiles.MapTilesDownloader
import org.koin.core.context.GlobalContext
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module

/** The instrumentation runner for all instrumented tests: blocks real network requests on the
 *  app's Ktor clients from the moment the app has started (see [RealNetworkGuard]) and doesn't
 *  download map tiles (see [OfflineMapTilesDownloader]). */
class SandboxedTestRunner : AndroidJUnitRunner() {
    override fun callApplicationOnCreate(app: Application) {
        super.callApplicationOnCreate(app)
        RealNetworkGuard.install(GlobalContext.get())
        // before anything got a Downloader (a single, created on first use) - it keeps the
        // map tiles downloader it was created with
        loadKoinModules(module {
            factory<MapTilesDownloader> { OfflineMapTilesDownloader }
        })
    }
}
