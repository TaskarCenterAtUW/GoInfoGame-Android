package de.westnordost.streetcomplete.testutils

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import org.koin.core.Koin
import org.koin.core.qualifier.named
import java.io.IOException
import java.util.Collections

/**
 * Keeps every instrumented test off the real network on the app's own Ktor clients - the main
 * workspace/TDEI client, "osmClient", "kartaViewClient" and "refreshClient" (the one the 401
 * token refresh uses). A request that no mock answered is blocked: it fails like an unreachable
 * server (IOException, so the app treats it as offline), is logged under [TAG] and recorded in
 * [blocked]. Installed once per test process by [SandboxedTestRunner].
 *
 * Why: tests once reached the production OSM proxy (and most likely the production TDEI refresh
 * endpoint) because a mock was bypassed - see MockOsmServer.attachTo. This makes that impossible
 * instead of relying on every test being set up right. (Map tiles are fetched by MapLibre, not
 * these clients, and still go out.)
 */
object RealNetworkGuard {
    const val TAG = "RealNetworkGuard"

    val blocked: MutableList<String> = Collections.synchronizedList(mutableListOf())

    fun install(koin: Koin) {
        guard(koin.get<HttpClient>(), "default") { false }
        guard(koin.get(named("osmClient")), "osmClient") { MockOsmServer.isActive }
        guard(koin.get(named("kartaViewClient")), "kartaViewClient") { MockKartaView.isActive }
        guard(koin.get(named("refreshClient")), "refreshClient") { false }
    }

    private fun guard(client: HttpClient, name: String, answeredByMock: () -> Boolean) {
        client.plugin(HttpSend).intercept { request ->
            // a mock hooked into the same client answers it (whichever of the two runs first)
            if (answeredByMock()) return@intercept execute(request)
            val line = "$name: ${request.method.value} ${request.url.buildString()}"
            blocked.add(line)
            Log.e(TAG, "Blocked real network request - $line")
            throw IOException("$TAG: real network blocked in tests ($line)")
        }
    }
}
