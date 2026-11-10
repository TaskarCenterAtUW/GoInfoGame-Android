package de.westnordost.streetcomplete.quests.note_discussion

import android.content.ActivityNotFoundException
import android.content.pm.PackageManager.FEATURE_CAMERA_ANY
import android.location.Location
import android.os.Bundle
import android.view.View
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import de.westnordost.streetcomplete.ApplicationConstants
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.photos.CapturedPhoto
import de.westnordost.streetcomplete.databinding.FragmentAttachPhotoBinding
import de.westnordost.streetcomplete.quests.PhotoCapture
import de.westnordost.streetcomplete.quests.PhotoStorageSize
import de.westnordost.streetcomplete.util.ktx.toast
import de.westnordost.streetcomplete.util.logs.Log
import de.westnordost.streetcomplete.util.viewBinding
import de.westnordost.streetcomplete.view.AdapterDataChangedWatcher
import kotlinx.serialization.json.Json
import java.io.File

class AttachPhotoFragment : Fragment(R.layout.fragment_attach_photo) {

    /** Where the user is, so each photo's bearing is relative to true north (see PhotoCapture) */
    interface Listener {
        val displayedMapLocation: Location?
    }
    private val listener: Listener?
        get() = generateSequence(parentFragment) { it.parentFragment }.firstNotNullOfOrNull { it as? Listener }
            ?: activity as? Listener

    private val binding by viewBinding(FragmentAttachPhotoBinding::bind)

    private val photoCapture = PhotoCapture(
        fragment = this,
        fileNamePrefix = "photo",
        size = PhotoStorageSize(ApplicationConstants.ATTACH_PHOTO_MAX_SIZE, ApplicationConstants.ATTACH_PHOTO_QUALITY),
        getLocation = { listener?.displayedMapLocation },
        onCaptured = ::onCapturedPhoto,
    )

    private lateinit var noteImageAdapter: NoteImageAdapter
    /** the bearing and capture time of each photo, by path. The adapter holds the paths in their
     *  order, and the user can delete photos there. */
    private val capturedPhotos = mutableMapOf<String, CapturedPhoto>()

    val imagePaths: List<String> get() = noteImageAdapter.list
    /** The attached photos in the order they were taken, each with its own bearing and capture time */
    val photos: List<CapturedPhoto> get() = imagePaths.map { capturedPhotos[it] ?: CapturedPhoto(it) }
    var hasGpxAttached = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val photos = savedInstanceState?.getString(PHOTOS)
            ?.let { Json.decodeFromString<List<CapturedPhoto>>(it) }
            .orEmpty()
        photos.associateByTo(capturedPhotos) { it.path }
        noteImageAdapter = NoteImageAdapter(photos.map { it.path }, requireContext())
        photoCapture.restoreState(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.isGone = !requireActivity().packageManager.hasSystemFeature(FEATURE_CAMERA_ANY)

        binding.takePhotoButton.setOnClickListener { takePhoto() }
        binding.photosList.adapter = noteImageAdapter
        noteImageAdapter.registerAdapterDataObserver(AdapterDataChangedWatcher { updateHintVisibility() })

        binding.attachedGpxView.isGone = !hasGpxAttached

        updateHintVisibility()
    }

    private fun updateHintVisibility() {
        binding.photosList.isGone = imagePaths.isEmpty()
        binding.photosAreUsefulExplanation.isGone = imagePaths.isNotEmpty()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(PHOTOS, Json.encodeToString(photos))
        photoCapture.saveState(outState)
    }

    private fun takePhoto() {
        try {
            photoCapture.launch()
        } catch (e: Exception) {
            when (e) {
                is ActivityNotFoundException -> context?.toast(R.string.no_camera_app)
                else -> {
                    Log.e(TAG, "Unable to create photo", e)
                    context?.toast(R.string.quest_leave_new_note_create_image_error)
                }
            }
        }
    }

    private fun onCapturedPhoto(photo: CapturedPhoto) {
        capturedPhotos[photo.path] = photo
        noteImageAdapter.list.add(photo.path)
        noteImageAdapter.notifyItemInserted(imagePaths.size - 1)
    }

    fun deleteImages() {
        for (path in imagePaths) {
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        }
    }

    companion object {
        private const val TAG = "AttachPhotoFragment"
        private const val PHOTOS = "photos"
    }
}
