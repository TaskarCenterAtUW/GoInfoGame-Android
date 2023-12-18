package com.tcatuw.goinfo.quests.orchard_produce

import com.tcatuw.goinfo.quests.AImageListQuestForm

class AddOrchardProduceForm : AImageListQuestForm<OrchardProduce, List<OrchardProduce>>() {

    private val producesMap = OrchardProduce.values()
        .map { it.asItem() }
        .associateBy { it.value!!.osmValue }

    // only include what is given for that country
    override val items get() = countryInfo.orchardProduces.mapNotNull { producesMap[it] }

    override val itemsPerRow = 3
    override val maxSelectableItems = -1

    override fun onClickOk(selectedItems: List<OrchardProduce>) {
        applyAnswer(selectedItems)
    }
}
