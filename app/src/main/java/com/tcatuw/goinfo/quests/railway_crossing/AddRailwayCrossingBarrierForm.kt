package com.tcatuw.goinfo.quests.railway_crossing

import com.tcatuw.goinfo.quests.AImageListQuestForm
import com.tcatuw.goinfo.view.image_select.DisplayItem

class AddRailwayCrossingBarrierForm : AImageListQuestForm<RailwayCrossingBarrier, RailwayCrossingBarrier>() {

    override val items: List<DisplayItem<RailwayCrossingBarrier>> get() {
        val isPedestrian = element.tags["railway"] == "crossing"
        return RailwayCrossingBarrier.getSelectableValues(isPedestrian)
            .map { it.asItem(countryInfo.isLeftHandTraffic) }
    }

    override val itemsPerRow = 4

    override fun onClickOk(selectedItems: List<RailwayCrossingBarrier>) {
        applyAnswer(selectedItems.single())
    }
}
