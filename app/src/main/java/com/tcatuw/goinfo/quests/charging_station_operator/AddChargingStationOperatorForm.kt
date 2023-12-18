package com.tcatuw.goinfo.quests.charging_station_operator

import com.tcatuw.goinfo.quests.ANameWithSuggestionsForm

class AddChargingStationOperatorForm : ANameWithSuggestionsForm<String>() {

    override val suggestions: List<String>? get() = countryInfo.chargingStationOperators

    override fun onClickOk() {
        applyAnswer(name!!)
    }
}
