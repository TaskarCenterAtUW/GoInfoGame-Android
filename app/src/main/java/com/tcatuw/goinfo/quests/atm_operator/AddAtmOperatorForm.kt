package com.tcatuw.goinfo.quests.atm_operator

import com.tcatuw.goinfo.quests.ANameWithSuggestionsForm

class AddAtmOperatorForm : ANameWithSuggestionsForm<String>() {

    override val suggestions: List<String>? get() = countryInfo.atmOperators

    override fun onClickOk() {
        applyAnswer(name!!)
    }
}
