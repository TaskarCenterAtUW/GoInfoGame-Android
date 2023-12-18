package com.tcatuw.goinfo.quests.smoking

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.quests.AListQuestForm
import com.tcatuw.goinfo.quests.TextItem
import com.tcatuw.goinfo.quests.smoking.SmokingAllowed.NO
import com.tcatuw.goinfo.quests.smoking.SmokingAllowed.OUTSIDE
import com.tcatuw.goinfo.quests.smoking.SmokingAllowed.SEPARATED
import com.tcatuw.goinfo.quests.smoking.SmokingAllowed.YES

class SmokingAllowedForm : AListQuestForm<SmokingAllowed>() {

    override val items: List<TextItem<SmokingAllowed>> get() {
        val tags = element.tags
        val isAlreadyOutdoor =
            tags["leisure"] == "outdoor_seating" || tags["amenity"] == "biergarten" ||
            (tags["outdoor_seating"] == "yes" && tags["indoor_seating"] == "no")
        val noOutdoorSmoking =
            tags["outdoor_seating"] == "no" &&
            tags["amenity"] != "nightclub" && tags["amenity"] != "stripclub" && tags["amenity"] != "pub"
            /* nightclubs etc. might have outside smoking areas even when there is no seating outside */

        return listOfNotNull(
            TextItem(NO, R.string.quest_smoking_no),
            if (isAlreadyOutdoor || noOutdoorSmoking) null else TextItem(OUTSIDE, R.string.quest_smoking_outside),
            TextItem(SEPARATED, R.string.quest_smoking_separated),
            TextItem(YES, R.string.quest_smoking_yes),
        )
    }
}
