package com.tcatuw.goinfo.quests.place_name

import androidx.appcompat.app.AlertDialog
import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.databinding.QuestLocalizednameBinding
import com.tcatuw.goinfo.osm.LocalizedName
import com.tcatuw.goinfo.quests.AAddLocalizedNameForm
import com.tcatuw.goinfo.quests.AnswerItem

class AddPlaceNameForm : AAddLocalizedNameForm<PlaceNameAnswer>() {

    override val contentLayoutResId = R.layout.quest_localizedname
    private val binding by contentViewBinding(QuestLocalizednameBinding::bind)

    override val addLanguageButton get() = binding.addLanguageButton
    override val namesList get() = binding.namesList

    override val otherAnswers = listOf(
        AnswerItem(R.string.quest_placeName_no_name_answer) { confirmNoName() }
    )

    override fun onClickOk(names: List<LocalizedName>) {
        applyAnswer(PlaceName(names))
    }

    private fun confirmNoName() {
        val ctx = context ?: return
        AlertDialog.Builder(ctx)
            .setTitle(R.string.quest_generic_confirmation_title)
            .setPositiveButton(R.string.quest_generic_confirmation_yes) { _, _ -> applyAnswer(NoPlaceNameSign) }
            .setNegativeButton(R.string.quest_generic_confirmation_no, null)
            .show()
    }
}
