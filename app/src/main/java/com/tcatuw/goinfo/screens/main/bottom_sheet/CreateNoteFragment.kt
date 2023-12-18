package com.tcatuw.goinfo.screens.main.bottom_sheet

import android.content.res.Configuration
import android.graphics.PointF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.BounceInterpolator
import android.view.animation.TranslateAnimation
import androidx.core.graphics.toPointF
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import com.tcatuw.goinfo.ApplicationConstants
import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.osm.mapdata.LatLon
import com.tcatuw.goinfo.data.osmnotes.edits.NoteEditAction
import com.tcatuw.goinfo.data.osmnotes.edits.NoteEditsController
import com.tcatuw.goinfo.data.osmtracks.Trackpoint
import com.tcatuw.goinfo.databinding.FormLeaveNoteBinding
import com.tcatuw.goinfo.databinding.FragmentCreateNoteBinding
import com.tcatuw.goinfo.quests.note_discussion.AttachPhotoFragment
import com.tcatuw.goinfo.util.ktx.childFragmentManagerOrNull
import com.tcatuw.goinfo.util.ktx.getLocationInWindow
import com.tcatuw.goinfo.util.ktx.hideKeyboard
import com.tcatuw.goinfo.util.ktx.viewLifecycleScope
import com.tcatuw.goinfo.util.viewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

/** Bottom sheet fragment with which the user can create a new note, including moving the note */
class CreateNoteFragment : AbstractCreateNoteFragment() {

    private val noteEditsController: NoteEditsController by inject()

    private var _binding: FragmentCreateNoteBinding? = null
    private val binding: FragmentCreateNoteBinding get() = _binding!!

    private val bottomSheetBinding get() = binding.questAnswerLayout

    override val bottomSheetContainer get() = bottomSheetBinding.bottomSheetContainer
    override val bottomSheet get() = bottomSheetBinding.bottomSheet
    override val scrollViewChild get() = bottomSheetBinding.scrollViewChild
    override val bottomSheetTitle get() = bottomSheetBinding.speechBubbleTitleContainer
    override val bottomSheetContent get() = bottomSheetBinding.speechbubbleContentContainer
    override val floatingBottomView get() = bottomSheetBinding.okButton
    override val backButton get() = bottomSheetBinding.closeButton
    override val okButton get() = bottomSheetBinding.okButton
    override val okButtonContainer get() = bottomSheetBinding.okButtonContainer

    private val contentBinding by viewBinding(FormLeaveNoteBinding::bind, R.id.content)

    override val noteInput get() = contentBinding.noteInput

    private var hasGpxAttached: Boolean = false

    interface Listener {
        fun getMapPositionAt(screenPos: PointF): LatLon?
        fun getRecordedTrack(): List<Trackpoint>?

        fun onCreatedNote(position: LatLon)
    }
    private val listener: Listener? get() = parentFragment as? Listener ?: activity as? Listener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hasGpxAttached = arguments?.getBoolean(ARG_HAS_GPX_ATTACHED) ?: false

        childFragmentManagerOrNull?.addFragmentOnAttachListener { _, fragment ->
            if (fragment is AttachPhotoFragment) {
                fragment.hasGpxAttached = hasGpxAttached
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCreateNoteBinding.inflate(inflater, container, false)
        inflater.inflate(R.layout.form_leave_note, bottomSheetBinding.content)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bottomSheetBinding.buttonPanel.isGone = true

        if (savedInstanceState == null) {
            binding.markerCreateLayout.markerLayoutContainer.startAnimation(createFallDownAnimation())
        }

        bottomSheetBinding.titleLabel.text = getString(R.string.map_btn_create_note)
        contentBinding.descriptionLabel.text = getString(R.string.create_new_note_description)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        binding.markerCreateLayout.centeredMarkerLayout.setPadding(
            resources.getDimensionPixelSize(R.dimen.quest_form_leftOffset),
            resources.getDimensionPixelSize(R.dimen.quest_form_topOffset),
            resources.getDimensionPixelSize(R.dimen.quest_form_rightOffset),
            resources.getDimensionPixelSize(R.dimen.quest_form_bottomOffset)
        )
    }

    private fun createFallDownAnimation(): Animation {
        val a = AnimationSet(false)
        a.startOffset = 200

        val ta = TranslateAnimation(0, 0f, 0, 0f, 1, -0.2f, 0, 0f)
        ta.interpolator = BounceInterpolator()
        ta.duration = 400
        a.addAnimation(ta)

        val aa = AlphaAnimation(0f, 1f)
        aa.interpolator = AccelerateInterpolator()
        aa.duration = 200
        a.addAnimation(aa)

        return a
    }

    override fun isRejectingClose() = super.isRejectingClose() || hasGpxAttached

    override fun onDiscard() {
        super.onDiscard()
        binding.markerCreateLayout.markerLayoutContainer.visibility = View.INVISIBLE
    }

    override fun onComposedNote(text: String, imagePaths: List<String>) {
        /* pressing once on "OK" should first only close the keyboard, so that the user can review
           the position of the note he placed */
        if (contentBinding.noteInput.hideKeyboard() == true) return

        val createNoteMarker = binding.markerCreateLayout.createNoteMarker
        val screenPos = createNoteMarker.getLocationInWindow()
        screenPos.offset(createNoteMarker.width / 2, createNoteMarker.height / 2)
        val position = listener?.getMapPositionAt(screenPos.toPointF()) ?: return

        binding.markerCreateLayout.markerLayoutContainer.visibility = View.INVISIBLE

        val fullText = "$text\n\nvia ${ApplicationConstants.USER_AGENT}"
        viewLifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val recordedTrack =
                    if (hasGpxAttached) listener?.getRecordedTrack().orEmpty() else emptyList()
                noteEditsController.add(0, NoteEditAction.CREATE, position, fullText, imagePaths, recordedTrack)
            }
        }

        listener?.onCreatedNote(position)
    }

    companion object {
        private const val ARG_HAS_GPX_ATTACHED = "hasGpxAttached"

        fun create(hasGpxAttached: Boolean) = CreateNoteFragment().also {
            it.arguments = bundleOf(ARG_HAS_GPX_ATTACHED to hasGpxAttached)
        }
    }
}
