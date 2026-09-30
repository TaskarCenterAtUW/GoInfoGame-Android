package de.westnordost.streetcomplete.testutils

import android.app.Application
import androidx.test.runner.AndroidJUnitRunner
import org.koin.core.context.GlobalContext

/** The instrumentation runner for all instrumented tests: blocks real network requests on the
 *  app's Ktor clients from the moment the app has started (see [RealNetworkGuard]). */
class SandboxedTestRunner : AndroidJUnitRunner() {
    override fun callApplicationOnCreate(app: Application) {
        super.callApplicationOnCreate(app)
        RealNetworkGuard.install(GlobalContext.get())
    }
}
