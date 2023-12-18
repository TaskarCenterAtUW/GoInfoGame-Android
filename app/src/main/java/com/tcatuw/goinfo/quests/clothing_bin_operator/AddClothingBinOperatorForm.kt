package com.tcatuw.goinfo.quests.clothing_bin_operator

import com.tcatuw.goinfo.quests.ANameWithSuggestionsForm

class AddClothingBinOperatorForm : ANameWithSuggestionsForm<String>() {

    override val suggestions: List<String>? get() = countryInfo.clothesContainerOperators

    override fun onClickOk() {
        applyAnswer(name!!)
    }
}
