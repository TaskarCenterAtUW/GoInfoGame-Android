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
import de.westnordost.streetcomplete.data.osm.edits.create_feature.FeaturePhoto
import de.westnordost.streetcomplete.databinding.QuestLongFormListBinding
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.LongFormAdapter
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.LongFormQuest
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.PhotoAttachment
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.contentEquals
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
     *  previous visit). Mirrored onto the adapter so the row showing the relevant
     *  choiceFollowUp can render it - see LongFormAdapter.updateChoiceFollowUp. */
    private var photoAttachment: PhotoAttachment = PhotoAttachment.None
        set(value) {
            field = value
            adapter.photoAttachment = value
        }

    /** Set when the user deletes an already-synced photo (from a previous visit) without
     *  attaching a replacement - signals onClickOk to actually remove the tag on submit. Reset
     *  whenever a fresh photo is captured, since that supersedes the removal. */
    private var existingPhotoRemoved = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = LongFormAdapter(cameraIntent = { setCameraIntent() }, onPhotoDeleted = ::onPhotoDeleted)
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
        // No null/isEmpty guard here on purpose: a question the user deselected/cleared back to
        // nothing (userInput null or empty) after it had a seeded answer must still be included -
        // otherwise the clear is silently dropped and the stale tag from before never gets
        // removed (contentEquals(null, null) already excludes a question that was never touched,
        // so this alone is sufficient to also exclude untouched blanks).
        //
        // A question currently hidden by questAnswerDependency (its controlling answer no longer
        // satisfies the dependency - e.g. the user deselected/changed the controlling answer this
        // visit) is submitted as cleared too, via a .copy() used ONLY for this comparison/submit -
        // the live item in adapter.givenItems is left untouched. This matters: if the user flips
        // the controlling answer back before submitting, the dependent question must still show
        // whatever was already entered, not something wiped out mid-edit. Only the final state at
        // submit time decides whether a hidden question's old value actually gets removed.
        val editedItems =
            adapter.givenItems.mapNotNull { quest ->
                val effective = if (quest.visible) quest else quest.copy(userInput = null)
                effective.takeIf { !it.userInput.contentEquals(it.seededAnswer) }
            }
        // the photo's URL isn't known yet if it was just captured this visit (upload happens
        // later, in the background sync - see AbstractOsmQuestForm.applyAnswer) - only an
        // explicit removal of an already-synced photo (without a replacement) is a tag change we
        // can express right away
        val photo = (photoAttachment as? PhotoAttachment.Pending)?.let { FeaturePhoto(it.path, it.bearing) }
        val removeTagKeys = if (photo == null && existingPhotoRemoved) {
            listOf(KARTAVIEW_URL_TAG)
        } else {
            emptyList()
        }

        // a "recheck" of an already fully-answered element - reopened purely because
        // AddGenericLong.isApplicableTo's recency check resurfaced it - has nothing to actually change, so editedItems above is
        // empty and this used to always be blocked with the "No changes" toast, leaving the pin
        // stuck reappearing forever (submitting never bumped the element's OSM timestamp).
        //
        // Only when EVERY currently-visible question already has a seeded answer (i.e. there is no
        // genuinely unanswered question left - the same condition AddGenericLong.isApplicableTo's
        // recency branch itself gates on) do we resubmit every already-answered question's OWN
        // existing value unchanged, purely to force a real, non-empty edit through so the element's
        // timestamp bumps. StringMapChangesBuilder records a same-value set as a real change
        // regardless of value equality, so this produces one Modify(key, x, x) per question - undo
        // reverts each back to the same value, so this is safe. If a real gap remains (some visible
        // question was never answered), this is NOT a recheck - fall through to the "No changes"
        // toast as before, so the user is still nudged to actually answer it.
        //
        // partialAnsweringRecheckEnabled is off in multi-select mode: resubmitting every answered
        // field there would apply to every selected element, and an element whose field genuinely
        // differs from the primary/session value would get silently overwritten. In that case a
        // no-diff recheck just falls through to the "No changes" toast instead.
        partialAnsweringRecheckEnabled = !isMultiSelectActive
        val isFullyAnswered = adapter.givenItems.none { it.visible && it.seededAnswer == null }
        val submittedItems = editedItems.ifEmpty {
            if (isFullyAnswered && partialAnsweringRecheckEnabled) {
                adapter.givenItems.filter { it.visible && it.seededAnswer != null }
            } else emptyList()
        }

        if (submittedItems.isEmpty()) {
            Toast.makeText(
                context,
                "No changes to submit. Please answer at least one question.",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            applyAnswer(submittedItems as T, removeTagKeys = removeTagKeys, photo = photo)
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
            element.tags[KARTAVIEW_URL_TAG]?.let { photoAttachment = PhotoAttachment.Uploaded(it) }
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
        // single-photo model: a new capture replaces whatever was pending before. If it's
        // replacing an already-synced photo (from a previous visit) rather than a not-yet-synced
        // local one, remember to remove that old tag on submit unless this new one ends up kept.
        (photoAttachment as? PhotoAttachment.Pending)?.let { File(it.path).delete() }
        if (photoAttachment is PhotoAttachment.Uploaded) existingPhotoRemoved = true
        photoAttachment = PhotoAttachment.Pending(path, bearing)
    }

    private fun onPhotoDeleted() {
        android.util.Log.d("PhotoDebug", "onPhotoDeleted called, current=$photoAttachment")
        when (val attachment = photoAttachment) {
            is PhotoAttachment.Pending -> File(attachment.path).delete()
            is PhotoAttachment.Uploaded -> existingPhotoRemoved = true
            PhotoAttachment.None -> {}
        }
        photoAttachment = PhotoAttachment.None
    }

    /** Called when the sheet is closed/discarded without submitting - a locally-captured photo
     *  that was never attached to an edit would otherwise leak on disk forever. */
    override fun onDiscard() {
        super.onDiscard()
        (photoAttachment as? PhotoAttachment.Pending)?.let { File(it.path).delete() }
    }

    private fun setVisibilityOfItems() {
        val itemCopy = items
        adapter.items = itemCopy as List<LongFormQuest>
    }

    companion object {
        private const val KARTAVIEW_URL_TAG = "ext:kartaview_url"
    }
}
