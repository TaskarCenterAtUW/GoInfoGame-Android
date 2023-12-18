package com.tcatuw.goinfo.screens.main.controls

import android.os.Bundle
import android.view.View
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.messages.Message
import com.tcatuw.goinfo.data.messages.MessagesSource
import com.tcatuw.goinfo.util.ktx.popIn
import com.tcatuw.goinfo.util.ktx.popOut
import com.tcatuw.goinfo.util.ktx.viewLifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

/** Handles showing a button with a little counter that shows how many unread messages there are */
class MessagesButtonFragment : Fragment(R.layout.fragment_messages_button) {

    private val messagesSource: MessagesSource by inject()

    interface Listener {
        fun onClickShowMessage(message: Message)
    }
    private val listener: Listener? get() = parentFragment as? Listener ?: activity as? Listener

    private val messagesButton get() = view as MessagesButton

    private var messagesSourceUpdateListener = object : MessagesSource.UpdateListener {
        override fun onNumberOfMessagesUpdated(numberOfMessages: Int) {
            viewLifecycleScope.launch { updateButtonStateAnimated(numberOfMessages) }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        messagesButton.setOnClickListener { viewLifecycleScope.launch { onClickButton() } }
    }

    override fun onStart() {
        super.onStart()
        messagesSource.addListener(messagesSourceUpdateListener)
        viewLifecycleScope.launch { initializeButtonState() }
    }

    override fun onStop() {
        super.onStop()
        messagesSource.removeListener(messagesSourceUpdateListener)
    }

    private suspend fun initializeButtonState() {
        val numberOfMessages = withContext(Dispatchers.IO) { messagesSource.getNumberOfMessages() }
        messagesButton.messagesCount = numberOfMessages
        messagesButton.isGone = numberOfMessages <= 0
    }

    private fun updateButtonStateAnimated(numberOfMessages: Int) {
        messagesButton.messagesCount = numberOfMessages
        if (messagesButton.isVisible && numberOfMessages == 0) {
            messagesButton.popOut()
        } else if (!messagesButton.isVisible && numberOfMessages > 0) {
            messagesButton.popIn()
        }
    }

    private suspend fun onClickButton() {
        val message = withContext(Dispatchers.IO) { messagesSource.popNextMessage() }
        if (message != null) {
            listener?.onClickShowMessage(message)
        }
    }
}
