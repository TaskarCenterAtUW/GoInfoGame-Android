package com.tcatuw.goinfo.quests.bridge_structure

import com.tcatuw.goinfo.quests.AImageListQuestForm

class AddBridgeStructureForm : AImageListQuestForm<BridgeStructure, BridgeStructure>() {

    override val items = BridgeStructure.values().map { it.asItem() }
    override val itemsPerRow = 2

    override fun onClickOk(selectedItems: List<BridgeStructure>) {
        applyAnswer(selectedItems.first())
    }
}
