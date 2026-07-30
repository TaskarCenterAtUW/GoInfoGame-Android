package de.westnordost.streetcomplete.screens.main.bottom_sheet

import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.core.widget.doAfterTextChanged
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.quests.note_discussion.AttachPhotoFragment
import de.westnordost.streetcomplete.util.ktx.nonBlankTextOrNull
import de.westnordost.streetcomplete.util.ktx.popIn
import de.westnordost.streetcomplete.util.ktx.popOut

/** Abstract base class for a bottom sheet that lets the user create a note */
abstract class AbstractCreateNoteFragment : AbstractBottomSheetFragment() {

    protected abstract val noteInput: EditText
    protected abstract val okButtonContainer: View
    protected abstract val okButton: View

    private val attachPhotoFragment: AttachPhotoFragment?
        get() = childFragmentManager.findFragmentById(R.id.attachPhotoFragment) as AttachPhotoFragment?

    protected val noteText get() = noteInput.nonBlankTextOrNull

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        noteInput.doAfterTextChanged { updateOkButtonEnablement() }
        okButton.setOnClickListener { onClickOk() }

        updateOkButtonEnablement()
    }

    protected fun onClickOk() {
        onComposedNote(noteText!!, attachPhotoFragment?.imagePaths.orEmpty())
    }

    override fun onDiscard() {
        attachPhotoFragment?.deleteImages()
    }

    override fun isRejectingClose() =
        noteText != null || attachPhotoFragment?.imagePaths?.isNotEmpty() == true

    // open so a subclass can drive its own submit button instead of the shared floating round
    // tick (okButtonContainer) - see CreateNoteFragment, which uses a full-width button instead
    // to match Add Feature / Long Form
    protected open fun updateOkButtonEnablement() {
        if (noteText != null) {
            okButtonContainer.popIn()
        } else {
            okButtonContainer.popOut()
        }
    }

    protected abstract fun onComposedNote(text: String, imagePaths: List<String>)
}
