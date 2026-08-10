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
        val editedItems =
            adapter.givenItems.filter {
                it.visible && it.userInput != null && !it.userInput!!.isEmpty() &&
                    !it.userInput.contentEquals(it.seededAnswer)
            }
        val tagList: MutableList<Pair<String, String>> = mutableListOf()
        if (imageUrls.isNotEmpty()) {
            val urls = imageUrls.joinToString(",")
            tagList.add(Pair("ext:kartaview_url", urls))
        }

        if (editedItems.isEmpty()) {
            Toast.makeText(
                context,
                "No changes to submit. Please answer at least one question.",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            applyAnswer(editedItems as T, tagList)
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
