package de.westnordost.streetcomplete.quests

import android.content.Context
import android.graphics.Bitmap
import android.hardware.GeomagneticField
import android.hardware.SensorManager
import android.location.Location
import android.os.Bundle
import android.os.Environment
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.content.getSystemService
import androidx.exifinterface.media.ExifInterface
import androidx.exifinterface.media.ExifInterface.TAG_GPS_IMG_DIRECTION
import androidx.exifinterface.media.ExifInterface.TAG_GPS_IMG_DIRECTION_REF
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.photos.CapturedPhoto
import de.westnordost.streetcomplete.util.decodeScaledBitmapAndNormalize
import de.westnordost.streetcomplete.util.ktx.nowAsEpochMilliseconds
import de.westnordost.streetcomplete.util.logs.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/** How a captured photo is stored: scaled down to at most [maxSize] px on each side, saved as JPEG
 *  at [quality]. */
data class PhotoStorageSize(val maxSize: Int, val quality: Int)

/**
 * The shared logic for every place that attaches photos (long form, notes, note comments, new
 * features). It takes a photo with the camera app, scales it down off the main thread and records
 * when it was taken and the direction the camera faced. That way each photo, even one of several,
 * gets its own [CapturedPhoto.bearing] and [CapturedPhoto.capturedAt].
 *
 * The direction is the one the camera app wrote into the photo, if any, because it knows the
 * moment of the shot. Otherwise it is where the back camera points right after the shot (see
 * [CameraBearingSensor]): the user is usually still aiming then. The GPS direction of travel is not
 * used, because it is not where the camera points.
 *
 * Create it while [fragment] is initialized or in its onCreate, because it registers for an
 * activity result. Forward onCreate's savedInstanceState to [restoreState] and onSaveInstanceState
 * to [saveState], so a photo being taken survives the app process being killed meanwhile.
 *
 * @param getLocation where the user is, used to correct magnetic north to true north. It is called
 *                    once the photo is taken.
 * @param onCaptured called on the main thread with each photo once it is processed
 */
class PhotoCapture(
    private val fragment: Fragment,
    private val fileNamePrefix: String,
    private val size: PhotoStorageSize,
    private val getLocation: () -> Location?,
    private val onCaptured: (CapturedPhoto) -> Unit,
) {
    private var pendingFile: File? = null
    private val launcher = fragment.registerForActivityResult(ActivityResultContracts.TakePicture(), ::onResult)
    private val bearingSensor by lazy {
        CameraBearingSensor(fragment.requireContext().getSystemService<SensorManager>()!!)
    }
    private val stateKey get() = "photoCapture_$fileNamePrefix"

    fun restoreState(savedInstanceState: Bundle?) {
        pendingFile = savedInstanceState?.getString(stateKey)?.let { File(it) }
    }

    fun saveState(outState: Bundle) {
        pendingFile?.let { outState.putString(stateKey, it.path) }
    }

    /** Opens the camera app.
     *  @throws android.content.ActivityNotFoundException if there is no camera app
     *  @throws IOException if the photo file could not be created */
    fun launch() {
        val context = fragment.requireContext()
        val file = createPhotoFile(context, fileNamePrefix)
        pendingFile = file
        try {
            val uri = FileProvider.getUriForFile(context, context.getString(R.string.fileprovider_authority), file)
            launcher.launch(uri)
        } catch (e: Exception) {
            file.delete()
            pendingFile = null
            throw e
        }
    }

    private fun onResult(hasSavedPhoto: Boolean) {
        val file = pendingFile
        pendingFile = null
        if (!hasSavedPhoto || file == null) {
            file?.delete()
            return
        }
        // control only comes back once the photo is taken, so this is when it was taken
        val capturedAt = nowAsEpochMilliseconds()
        val location = getLocation()
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            val sensorBearing = async { bearingSensor.awaitBearing(location) }
            // decoding, scaling and re-encoding the full camera photo takes long enough to freeze
            // the UI visibly when done on the main thread
            val exifBearing = try {
                withContext(Dispatchers.IO) {
                    val exif = ExifInterface(file)
                    rescalePhotoFile(file, size)
                    copyDirectionExifData(file, exif)
                    Result.success(exif.getTrueNorthImageDirection(location))
                }
            } catch (e: IOException) {
                Log.e(TAG, "Unable to process photo", e)
                null
            } catch (e: CancellationException) {
                // the screen was closed meanwhile: nobody takes ownership of the file
                file.delete()
                throw e
            }
            if (exifBearing == null) {
                sensorBearing.cancel()
                file.delete()
                return@launch
            }
            val bearing = exifBearing.getOrNull()
                ?.also { sensorBearing.cancel() }
                ?: sensorBearing.await()
            onCaptured(CapturedPhoto(file.path, bearing, capturedAt))
        }
    }

    companion object {
        private const val TAG = "PhotoCapture"
    }
}

private fun createPhotoFile(context: Context, prefix: String): File {
    val directory = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    val file = File(directory, prefix + "_" + nowAsEpochMilliseconds() + ".jpg")
    if (!file.createNewFile()) throw IOException("Photo file with exactly the same name already exists")
    return file
}

private fun rescalePhotoFile(file: File, size: PhotoStorageSize) {
    val bitmap = decodeScaledBitmapAndNormalize(file.path, size.maxSize, size.maxSize) ?: throw IOException()
    FileOutputStream(file.path).use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, size.quality, out)
    }
    bitmap.recycle()
}

/** The GPSImgDirection the camera app wrote into the photo (0-359, clockwise from true north),
 *  or null if it didn't. If the camera recorded it relative to magnetic north, it is converted
 *  using the declination at [location]. */
private fun ExifInterface.getTrueNorthImageDirection(location: Location?): Float? {
    val direction = getAttributeDouble(TAG_GPS_IMG_DIRECTION, Double.NaN)
    if (direction.isNaN()) return null
    val declination = if (getAttribute(TAG_GPS_IMG_DIRECTION_REF) == "M" && location != null) {
        GeomagneticField(
            location.latitude.toFloat(), location.longitude.toFloat(), location.altitude.toFloat(),
            nowAsEpochMilliseconds()
        ).declination
    } else 0f
    return (direction.toFloat() + declination).mod(360f)
}

private fun copyDirectionExifData(file: File, exif: ExifInterface) {
    val newExif = ExifInterface(file.path)
    newExif.setAttribute(TAG_GPS_IMG_DIRECTION, exif.getAttribute(TAG_GPS_IMG_DIRECTION))
    newExif.setAttribute(TAG_GPS_IMG_DIRECTION_REF, exif.getAttribute(TAG_GPS_IMG_DIRECTION_REF))
    newExif.saveAttributes()
}
