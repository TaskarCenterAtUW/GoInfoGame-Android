package com.tcatuw.goinfo.quests.access_point_ref

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.databinding.QuestRefBinding
import com.tcatuw.goinfo.quests.AbstractOsmQuestForm
import com.tcatuw.goinfo.quests.AnswerItem
import com.tcatuw.goinfo.util.ktx.nonBlankTextOrNull

class AddAccessPointRefForm : AbstractOsmQuestForm<AccessPointRefAnswer>() {

    override val contentLayoutResId = R.layout.quest_ref
    private val binding by contentViewBinding(QuestRefBinding::bind)

    override val otherAnswers get() = listOfNotNull(
        AnswerItem(R.string.quest_ref_answer_noRef) { confirmNoRef() },
        AnswerItem(R.string.quest_accessPointRef_answer_assembly_point) { confirmAssemblyPoint() }
    )

    private val ref get() = binding.refInput.nonBlankTextOrNull

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.refInput.doAfterTextChanged { checkIsFormComplete() }
    }

    override fun onClickOk() {
        applyAnswer(AccessPointRef(ref!!))
    }

    private fun confirmNoRef() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.quest_generic_confirmation_title)
            .setPositiveButton(R.string.quest_generic_confirmation_yes) { _, _ -> applyAnswer(NoVisibleAccessPointRef) }
            .setNegativeButton(R.string.quest_generic_confirmation_no, null)
            .show()
    }

    private fun confirmAssemblyPoint() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.quest_generic_confirmation_title)
            .setView(R.layout.dialog_confirm_assembly_point)
            .setPositiveButton(R.string.quest_generic_confirmation_yes) { _, _ -> applyAnswer(IsAssemblyPointAnswer) }
            .setNegativeButton(R.string.quest_generic_confirmation_no, null)
            .show()
    }

    override fun isFormComplete() = ref != null
}
