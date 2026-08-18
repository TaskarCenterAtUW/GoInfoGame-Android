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
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.contentEquals
import de.westnordost.streetcomplete.util.ktx.toast
import kotlinx.coroutines.launch

abstract class ALongForm<T> : AbstractOsmQuestForm<T>() {
    final override val contentLayoutResId = R.layout.quest_long_form_list
    private val binding by contentViewBinding(QuestLongFormListBinding::bind)
    protected lateinit var adapter: LongFormAdapter<T>

    override val defaultExpanded = false

    protected abstract val items: T

    private var imageUrls: MutableList<String> = mutableListOf()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = LongFormAdapter { setCameraIntent() }
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
        val tagList: MutableList<Pair<String, String>> = mutableListOf()
        if (imageUrls.isNotEmpty()) {
            val urls = imageUrls.joinToString(",")
            tagList.add(Pair("ext:kartaview_url", urls))
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
        val isFullyAnswered = adapter.givenItems.none { it.visible && it.seededAnswer == null }
        val submittedItems = editedItems.ifEmpty {
            if (isFullyAnswered) adapter.givenItems.filter { it.visible && it.seededAnswer != null }
            else emptyList()
        }

        if (submittedItems.isEmpty()) {
            Toast.makeText(
                context,
                "No changes to submit. Please answer at least one question.",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            applyAnswer(submittedItems as T, tagList)
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

    override fun onImageUrlReceived(imageUrl: String) {
        this.imageUrls.add(imageUrl)
    }

    private fun setVisibilityOfItems() {
        val itemCopy = items
        adapter.items = itemCopy as List<LongFormQuest>
    }
}
