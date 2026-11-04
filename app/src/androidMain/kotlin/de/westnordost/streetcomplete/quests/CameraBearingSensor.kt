package de.westnordost.streetcomplete.quests

import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.SystemClock
import de.westnordost.streetcomplete.util.ktx.nowAsEpochMilliseconds
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.math.atan2
import kotlin.math.hypot

/** Measures the compass direction the back camera of the device is facing - not the direction of
 *  the device's top edge, like the map compass (which is meant for a device lying flat): when a
 *  photo is taken, the device is held upright and the camera looks out of its back (-Z axis). */
class CameraBearingSensor(private val sensorManager: SensorManager) {

    /** Waits up to [timeoutMillis] for a fresh reading and returns the direction the back camera
     *  faces, in degrees clockwise from true north (from magnetic north if [location] is null), or
     *  null if there is no usable reading: no sensor, no reading in time, or the camera pointing
     *  (almost) straight up or down. */
    suspend fun awaitBearing(location: Location?, timeoutMillis: Long = 1500): Float? {
        val bearing = withTimeoutOrNull(timeoutMillis) { awaitMagneticBearing() } ?: return null
        val declination = location?.let {
            GeomagneticField(
                it.latitude.toFloat(), it.longitude.toFloat(), it.altitude.toFloat(),
                nowAsEpochMilliseconds()
            ).declination
        } ?: 0f
        return (bearing + declination).mod(360f)
    }

    private suspend fun awaitMagneticBearing(): Float? {
        val rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        if (rotationVector == null && (accelerometer == null || magnetometer == null)) return null

        return suspendCancellableCoroutine { cont ->
            val start = SystemClock.elapsedRealtimeNanos()
            val rotationMatrix = FloatArray(9)
            var gravity: FloatArray? = null
            var geomagnetic: FloatArray? = null

            val listener = object : SensorEventListener {
                override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

                override fun onSensorChanged(event: SensorEvent) {
                    // the first events after (re-)registering can still carry an old state
                    // (event.timestamp's time base differs between devices, so measured on receipt)
                    if (SystemClock.elapsedRealtimeNanos() - start < SETTLE_NANOS) return
                    val hasMatrix = when (event.sensor.type) {
                        Sensor.TYPE_ROTATION_VECTOR -> {
                            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                            true
                        }
                        else -> {
                            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                                gravity = event.values.copyOf()
                            } else {
                                geomagnetic = event.values.copyOf()
                            }
                            val g = gravity ?: return
                            val m = geomagnetic ?: return
                            SensorManager.getRotationMatrix(rotationMatrix, null, g, m)
                        }
                    }
                    if (!hasMatrix || !cont.isActive) return
                    sensorManager.unregisterListener(this)
                    cont.resume(getBackCameraBearing(rotationMatrix))
                }
            }
            if (rotationVector != null) {
                sensorManager.registerListener(listener, rotationVector, SensorManager.SENSOR_DELAY_GAME)
            } else {
                sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
                sensorManager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_GAME)
            }
            cont.invokeOnCancellation { sensorManager.unregisterListener(listener) }
        }
    }

    /** [r] maps device coordinates to world coordinates (x = east, y = magnetic north, z = up).
     *  The back camera looks along the device's -Z axis, i.e. the world vector -(r[2], r[5], r[8]).
     *  Returns its horizontal direction, or null if it points (almost) straight up or down. */
    private fun getBackCameraBearing(r: FloatArray): Float? {
        val east = -r[2]
        val north = -r[5]
        if (hypot(east, north) < MIN_HORIZONTAL) return null
        return Math.toDegrees(atan2(east, north).toDouble()).toFloat()
    }

    companion object {
        private const val SETTLE_NANOS = 200_000_000L
        /** sin(~10°): pointing more steeply up or down than that, the direction is meaningless */
        private const val MIN_HORIZONTAL = 0.17f
    }
}
