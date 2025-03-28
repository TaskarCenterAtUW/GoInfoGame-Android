package de.westnordost.streetcomplete.screens.main.bottom_sheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import de.westnordost.streetcomplete.databinding.FragmentYesNoBottomSheetBinding
import de.westnordost.streetcomplete.screens.main.edithistory.EditHistoryViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class MultiSelectOptionsFragment : Fragment() {

    private var _binding: FragmentYesNoBottomSheetBinding? = null
    private val binding get() = _binding!!
    private val multiSelectViewModel by viewModel<MultiSelectViewModel>(ownerProducer = { requireParentFragment() })

    var onYesClick: (() -> Unit)? = null
    var onNoClick: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentYesNoBottomSheetBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.yesButton.setOnClickListener {
            onYesClick?.invoke()
        }

        binding.noButton.setOnClickListener {
            onNoClick?.invoke()
        }

        multiSelectViewModel.dynamicText.observe(viewLifecycleOwner) { newText ->
            binding.title.text = newText
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

