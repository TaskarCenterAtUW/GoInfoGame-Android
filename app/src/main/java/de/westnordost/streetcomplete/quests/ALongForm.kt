package de.westnordost.streetcomplete.quests

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.databinding.QuestLongFormListBinding
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.LongFormAdapter
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.Quest

abstract class ALongForm<T> : AbstractOsmQuestForm<T>() {
    final override val contentLayoutResId = R.layout.quest_long_form_list
    private val binding by contentViewBinding(QuestLongFormListBinding::bind)
    protected lateinit var adapter: LongFormAdapter<T>

    var answerMap : MutableMap<Int, Pair<String, String>> = mutableMapOf()

    override val defaultExpanded = false

    protected abstract val items: T
    private var imageUrl : String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter =  LongFormAdapter()
    }

    override fun onClickOk() {
        val editedItems = adapter.givenItems.filter { it.visible  && it.userInput !=null}
        val tagList : MutableList<Pair<String, String>> = mutableListOf()
        if (imageUrl !=null){
            tagList.add(Pair("ext:kartaview_url", imageUrl!!))
        }

        if (editedItems.isEmpty()) {
            Toast.makeText(context, "No changes to submit. Please answer at least one question.", Toast.LENGTH_SHORT).show()
        }else{
            applyAnswer(editedItems as T, tagList)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(activity)
        }
        setVisibilityOfItems()
        binding.recyclerView.adapter = adapter
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
        binding.imageUrlTV.visibility = View.VISIBLE
        this.imageUrl = imageUrl
        binding.imageUrlTV.text = getSpannableText(imageUrl)
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
        adapter.items = itemCopy as List<Quest>
    }
}

data class LongFormItem<T>(val options: T, val title: String?, val description: String?, val questId : Int?,
    var visible : Boolean = true, var userInput : String? = null)
