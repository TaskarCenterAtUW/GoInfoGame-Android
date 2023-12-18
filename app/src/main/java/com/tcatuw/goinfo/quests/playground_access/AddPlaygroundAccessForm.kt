package com.tcatuw.goinfo.quests.playground_access

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.quests.AListQuestForm
import com.tcatuw.goinfo.quests.TextItem
import com.tcatuw.goinfo.quests.playground_access.PlaygroundAccess.CUSTOMERS
import com.tcatuw.goinfo.quests.playground_access.PlaygroundAccess.PRIVATE
import com.tcatuw.goinfo.quests.playground_access.PlaygroundAccess.YES

class AddPlaygroundAccessForm : AListQuestForm<PlaygroundAccess>() {

    override val items = listOf(
        TextItem(YES, R.string.quest_access_yes),
        TextItem(CUSTOMERS, R.string.quest_access_customers),
        TextItem(PRIVATE, R.string.quest_access_private),
    )
}
