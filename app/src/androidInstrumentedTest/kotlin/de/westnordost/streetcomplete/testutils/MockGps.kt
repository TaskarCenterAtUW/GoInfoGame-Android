package de.westnordost.streetcomplete.testutils

import android.content.Context
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import java.util.Timer
import kotlin.concurrent.fixedRateTimer

/**
 * Puts the device at a fixed position for the app: GPS and network test providers that keep
 * reporting [lat]/[lon] (re-sent every second, since the app also asks for fresh fixes).
 * The test runs in the app's own process, so it can register the providers itself once the app
 * is allowed to mock locations.
 */
class MockGps(private val lat: Double, private val lon: Double) {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var timer: Timer? = null

    fun start() {
        shell("appops set ${context.packageName} android:mock_location allow")
        shell("cmd location set-location-enabled true")
        for (provider in PROVIDERS) {
            try { locationManager.removeTestProvider(provider) } catch (_: Exception) {}
            if (Build.VERSION.SDK_INT >= 31) {
                locationManager.addTestProvider(
                    provider, false, false, false, false, true, true, true,
                    ProviderProperties.POWER_USAGE_LOW, ProviderProperties.ACCURACY_FINE
                )
            } else {
                @Suppress("DEPRECATION")
                locationManager.addTestProvider(
                    provider, false, false, false, false, true, true, true,
                    Criteria.POWER_LOW, Criteria.ACCURACY_FINE
                )
            }
            locationManager.setTestProviderEnabled(provider, true)
        }
        timer = fixedRateTimer("MockGps", daemon = true, initialDelay = 0L, period = 1_000L) { push() }
    }

    fun stop() {
        timer?.cancel()
        timer = null
        for (provider in PROVIDERS) {
            try { locationManager.removeTestProvider(provider) } catch (_: Exception) {}
        }
    }

    private fun push() {
        for (provider in PROVIDERS) {
            val location = Location(provider).apply {
                latitude = lat
                longitude = lon
                accuracy = 5f
                altitude = 0.0
                time = System.currentTimeMillis()
                elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
            }
            try { locationManager.setTestProviderLocation(provider, location) } catch (_: Exception) {}
        }
    }

    private fun shell(command: String) {
        val pfd = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes() }
    }

    private companion object {
        val PROVIDERS = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
    }
}
