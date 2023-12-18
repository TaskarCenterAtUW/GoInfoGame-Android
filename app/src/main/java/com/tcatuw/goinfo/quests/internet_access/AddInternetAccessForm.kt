package com.tcatuw.goinfo.quests.internet_access

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.quests.AListQuestForm
import com.tcatuw.goinfo.quests.TextItem
import com.tcatuw.goinfo.quests.internet_access.InternetAccess.NO
import com.tcatuw.goinfo.quests.internet_access.InternetAccess.TERMINAL
import com.tcatuw.goinfo.quests.internet_access.InternetAccess.WIFI
import com.tcatuw.goinfo.quests.internet_access.InternetAccess.WIRED

class AddInternetAccessForm : AListQuestForm<InternetAccess>() {

    override val items = listOf(
        TextItem(WIFI, R.string.quest_internet_access_wlan),
        TextItem(NO, R.string.quest_internet_access_no),
        TextItem(TERMINAL, R.string.quest_internet_access_terminal),
        TextItem(WIRED, R.string.quest_internet_access_wired),
    )
}
