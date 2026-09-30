package de.westnordost.streetcomplete.quests

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.databinding.QuestLongFormListBinding
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.LongFormAdapter
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.LongFormQuest
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.KARTAVIEW_URL_TAG
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.PhotoAttachment
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.PhotoState
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.PhotoTransition
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.afterPhotoCaptured
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.afterPhotoDeleted
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.afterPhotoUndoRemoval
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.computeLongFormSubmission
import de.westnordost.streetcomplete.util.ktx.toast
import kotlinx.coroutines.launch
import java.io.File

abstract class ALongForm<T> : AbstractOsmQuestForm<T>() {
    final override val contentLayoutResId = R.layout.quest_long_form_list
    private val binding by contentViewBinding(QuestLongFormListBinding::bind)
    protected lateinit var adapter: LongFormAdapter<T>

    override val defaultExpanded = false

    protected abstract val items: T

    /** The (at most one) photo attached to this quest - either just captured locally this visit
     *  (not yet uploaded - upload happens in the background sync, see AbstractOsmQuestForm) or
     *  read back from the element's own ext:kartaview_url tag on open (already synced from a
     *  previous visit) - plus whether an already-synced one was removed. The attachment is
     *  mirrored onto the adapter so the row showing the relevant choiceFollowUp can render it -
     *  see LongFormAdapter.updateChoiceFollowUp. Transitions live in LongFormSubmission.kt. */
    private var photoState = PhotoState()
        set(value) {
            field = value
            adapter.photoAttachment = value.attachment
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = LongFormAdapter(
            cameraIntent = { setCameraIntent() },
            onPhotoDeleted = ::onPhotoDeleted,
            onPhotoUndoRemoval = ::onPhotoUndoRemoval,
        )
    }

    // Gates the "resubmit every answered question on a no-diff recheck" behavior in onClickOk.
    // Multi-select would resubmit every answered field for every selected element, risking
    // overwriting a secondary element's genuinely different value with the primary session's one -
    // so this is turned off whenever multi-select is active (see onClickOk).
    protected var partialAnsweringRecheckEnabled: Boolean = true

    /** Whether this form was opened for a multi-select group (2+ quests selected on the map) -
     *  same listener lookup `AbstractOsmQuestForm`'s own private `listener` uses internally, but
     *  that property isn't exposed to subclasses so it's re-derived here. Used to disable partial
     *  answering entirely in multi-select: pre-filling from one element's tags and pre-computing
     *  "already answered" doesn't make sense when the answer is about to be applied to several
     *  different elements that may not share the same existing tag values. */
    protected val isMultiSelectActive: Boolean
        get() {
            val listener = parentFragment as? Listener ?: activity as? Listener
            return !listener?.mutableMultiSelectQuests.isNullOrEmpty()
        }

    override fun onClickOk() {
        partialAnsweringRecheckEnabled = !isMultiSelectActive
        val submission = computeLongFormSubmission(
            adapter.givenItems,
            photoState,
            recheckEnabled = partialAnsweringRecheckEnabled
        )
        submission.photoTransition?.let(::applyPhotoTransition)

        if (submission.isEmpty) {
            Toast.makeText(
                context,
                "No changes to submit. Please answer at least one question.",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            applyAnswer(
                submission.items as T,
                removeTagKeys = submission.removeTagKeys,
                photo = submission.photo
            )
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun setupRecyclerViewTouchListener(recyclerView: RecyclerView, editTextId: Int) {
        recyclerView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_MOVE) {
                val currentFocus =
                    recyclerView.findFocus() // Use RecyclerView's context to find focus

                if (currentFocus?.id == editTextId) {
                    hideKeyboardFrom(recyclerView.context, currentFocus) //recyclerView.context
                    currentFocus.clearFocus()
                }
            }
            false
        }
    }

    private fun hideKeyboardFrom(context: Context, view: View) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // pre-fills the photo preview from a previous visit's upload, same principle as
        // LongFormQuest.seedFrom for individual questions - not shown in multi-select, since
        // that's about to be applied to several elements that may not share the same photo
        if (!isMultiSelectActive) {
            element.tags[KARTAVIEW_URL_TAG]?.let { photoState = PhotoState(PhotoAttachment.Uploaded(it)) }
        }
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(activity)
            // DiffUtil-driven partial updates (see LongFormAdapter.items) now issue targeted
            // notifyItemChanged() calls instead of notifyDataSetChanged(). The default item
            // animator runs a cross-fade "change" animation on those, which reads as flicker on
            // image content - notifyDataSetChanged() never triggered that. Keep insert/remove/move
            // animations (questions appearing/disappearing still animates nicely), just drop the
            // content-change cross-fade.
            (itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        }
        setVisibilityOfItems()
        binding.recyclerView.adapter = adapter
        setupRecyclerViewTouchListener(binding.recyclerView, R.id.editText)
        binding.submitButton.apply {
            setOnClickListener {
                if (adapter.isErrorFree.value) {
                    onClickOk()
                } else {
                    context?.toast("Please correct the errors before submitting.")
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                adapter.isErrorFree.collect { isErrorFree ->
                    // stays clickable either way - isClickable = false would swallow the tap
                    // entirely, so there'd be no chance to show the user why nothing happened
                    binding.submitButton.alpha = if (isErrorFree) 1f else 0.5f
                }
            }
        }
    }

    override fun onPhotoCaptured(path: String, bearing: Float) {
        applyPhotoTransition(photoState.afterPhotoCaptured(path, bearing))
    }

    private fun onPhotoDeleted() {
        applyPhotoTransition(photoState.afterPhotoDeleted())
    }

    private fun onPhotoUndoRemoval() {
        photoState = photoState.afterPhotoUndoRemoval()
    }

    private fun applyPhotoTransition(transition: PhotoTransition) {
        transition.fileToDelete?.let { File(it).delete() }
        photoState = transition.state
    }

    /** Called when the sheet is closed/discarded without submitting - a locally-captured photo
     *  that was never attached to an edit would otherwise leak on disk forever. */
    override fun onDiscard() {
        super.onDiscard()
        (photoState.attachment as? PhotoAttachment.Pending)?.let { File(it.path).delete() }
    }

    private fun setVisibilityOfItems() {
        val itemCopy = items
        adapter.items = itemCopy as List<LongFormQuest>
    }
}
