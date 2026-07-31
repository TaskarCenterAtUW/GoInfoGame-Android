package de.westnordost.streetcomplete.screens.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import de.westnordost.streetcomplete.databinding.DialogQuestCheckBinding

/** Small modal bottom sheet shown while re-checking a gig quest's element against the server right
 *  before opening its answer form (see MainActivity.showQuestDetails) - starts in a loading state,
 *  then either gets dismissed (element still needs an answer) or switched via
 *  [showAlreadyAnswered] to tell the user someone else already answered it. */
class QuestCheckBottomSheet : BottomSheetDialogFragment() {

    private var _binding: DialogQuestCheckBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // not dismissable while the check is still in flight - nothing actionable to cancel yet,
        // and the quest form will just pop up a moment later regardless once the check resolves
        isCancelable = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = DialogQuestCheckBinding.inflate(inflater, container, false)
        return binding.root
    }

    fun showAlreadyAnswered(onOk: () -> Unit) {
        isCancelable = true
        _binding?.apply {
            loadingGroup.isGone = true
            answeredGroup.isVisible = true
            okButton.setOnClickListener { onOk() }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
