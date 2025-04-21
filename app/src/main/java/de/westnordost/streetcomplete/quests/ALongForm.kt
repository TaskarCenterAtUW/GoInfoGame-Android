package de.westnordost.streetcomplete.quests

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.quest.Quest
import de.westnordost.streetcomplete.databinding.QuestLongFormListBinding
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.LongFormAdapter
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.LongFormQuest

abstract class ALongForm<T> : AbstractOsmQuestForm<T>() {
    final override val contentLayoutResId = R.layout.quest_long_form_list
    private val binding by contentViewBinding(QuestLongFormListBinding::bind)
    protected lateinit var adapter: LongFormAdapter<T>

    var answerMap : MutableMap<Int, Pair<String, String>> = mutableMapOf()

    override val defaultExpanded = false

    protected abstract val items: T
    protected abstract val multiselectItems : List<Quest>

    private var imageUrls : MutableList<String> = mutableListOf()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter =  LongFormAdapter {setCameraIntent()}
    }

    override fun onClickOk() {
        val editedItems = adapter.givenItems.filter { it.visible  && it.userInput !=null}
        val tagList : MutableList<Pair<String, String>> = mutableListOf()
        if (imageUrls.isNotEmpty()){
            val urls = imageUrls.joinToString(",")
            tagList.add(Pair("ext:kartaview_url", urls))
        }

        if (editedItems.isEmpty()) {
            Toast.makeText(context, "No changes to submit. Please answer at least one question.", Toast.LENGTH_SHORT).show()
        }else{
            applyAnswer(editedItems as T, tagList)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun setupRecyclerViewTouchListener(recyclerView: RecyclerView, editTextId: Int) {
        recyclerView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_MOVE) {
                val currentFocus = recyclerView.findFocus() // Use RecyclerView's context to find focus

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
        }
        setVisibilityOfItems()
        binding.recyclerView.adapter = adapter
        setupRecyclerViewTouchListener(binding.recyclerView,R.id.editText)
        binding.submitButton.setOnClickListener {
            onClickOk()
        }
        binding.cameraIntentTV.setOnClickListener {
            setCameraIntent()
            //hide
        }
        binding.hideTV.setOnClickListener {
            hideQuest()
        }

        binding.composeNoteTV.setOnClickListener {
            composeNote()
        }

    }

    override fun onImageUrlReceived(imageUrl: String) {
//        binding.imageUrlTV.visibility = View.VISIBLE
        this.imageUrls.add(imageUrl)
//        binding.imageUrlTV.text = getSpannableText(imageUrl)
    }

    private fun getSpannableText(imageUrl: String): SpannableString {
        val text = "Image path : Click here"
        val spannableString = SpannableString(text)
        // Find start and end index of "Click here"
        val startIndex = text.indexOf("Click here")
        val endIndex = startIndex + "Click here".length
        // Make "Click here" clickable
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                // Intent to open a link in browser
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(imageUrl))
                startActivity(intent)
            }
        }
        spannableString.setSpan(clickableSpan, startIndex,endIndex , Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        // Color "and this part is colored"
        // Get color from resources
        val color = resources.getColor(R.color.primary)
        val colorSpan = ForegroundColorSpan(color)
        spannableString.setSpan(colorSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        // Enable clicking on the TextView
        binding.imageUrlTV.movementMethod = LinkMovementMethod.getInstance()
        return spannableString
    }

    private fun setVisibilityOfItems() {
        val itemCopy = items
        adapter.items = (itemCopy as List<LongFormQuest>).apply {
            this.forEach {
                it.selectedIndex = null
            }
        }
    }
}
