package com.tcatuw.goinfo.quests.defibrillator

import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.databinding.QuestLocationBinding
import com.tcatuw.goinfo.quests.AbstractOsmQuestForm
import com.tcatuw.goinfo.util.ktx.nonBlankTextOrNull

class AddDefibrillatorLocationForm : AbstractOsmQuestForm<String>() {

    override val contentLayoutResId = R.layout.quest_location
    private val binding by contentViewBinding(QuestLocationBinding::bind)

    private val location get() = binding.locationInput.nonBlankTextOrNull

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.locationInput.doAfterTextChanged { checkIsFormComplete() }
    }

    override fun onClickOk() {
        applyAnswer(location!!)
    }

    override fun isFormComplete() = location != null
}
