package com.tcatuw.goinfo.quests.hairdresser

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.quests.AListQuestForm
import com.tcatuw.goinfo.quests.TextItem
import com.tcatuw.goinfo.quests.hairdresser.HairdresserCustomers.MALE_AND_FEMALE
import com.tcatuw.goinfo.quests.hairdresser.HairdresserCustomers.NOT_SIGNED
import com.tcatuw.goinfo.quests.hairdresser.HairdresserCustomers.ONLY_FEMALE
import com.tcatuw.goinfo.quests.hairdresser.HairdresserCustomers.ONLY_MALE

class AddHairdresserCustomersForm : AListQuestForm<HairdresserCustomers>() {
    override val items = listOf(
        TextItem(MALE_AND_FEMALE, R.string.quest_hairdresser_male_and_female),
        TextItem(ONLY_FEMALE, R.string.quest_hairdresser_female_only),
        TextItem(ONLY_MALE, R.string.quest_hairdresser_male_only),
        TextItem(NOT_SIGNED, R.string.quest_hairdresser_not_signed),
    )
}
