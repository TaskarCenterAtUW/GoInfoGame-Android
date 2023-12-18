package com.tcatuw.goinfo.quests.parking_access

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.quests.AListQuestForm
import com.tcatuw.goinfo.quests.TextItem
import com.tcatuw.goinfo.quests.parking_access.ParkingAccess.CUSTOMERS
import com.tcatuw.goinfo.quests.parking_access.ParkingAccess.PRIVATE
import com.tcatuw.goinfo.quests.parking_access.ParkingAccess.YES

class AddParkingAccessForm : AListQuestForm<ParkingAccess>() {

    override val items = listOf(
        TextItem(YES, R.string.quest_access_yes),
        TextItem(CUSTOMERS, R.string.quest_access_customers),
        TextItem(PRIVATE, R.string.quest_access_private),
    )
}
