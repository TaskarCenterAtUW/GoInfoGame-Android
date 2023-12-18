package com.tcatuw.goinfo.quests.powerpoles_material

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.quests.powerpoles_material.PowerPolesMaterial.CONCRETE
import com.tcatuw.goinfo.quests.powerpoles_material.PowerPolesMaterial.STEEL
import com.tcatuw.goinfo.quests.powerpoles_material.PowerPolesMaterial.WOOD
import com.tcatuw.goinfo.view.image_select.Item

fun PowerPolesMaterial.asItem() = Item(this, iconResId, titleResId)

private val PowerPolesMaterial.titleResId: Int get() = when (this) {
    WOOD ->     R.string.quest_powerPolesMaterial_wood
    STEEL ->    R.string.quest_powerPolesMaterial_metal
    CONCRETE -> R.string.quest_powerPolesMaterial_concrete
}

private val PowerPolesMaterial.iconResId: Int get() = when (this) {
    WOOD ->     R.drawable.power_pole_wood
    STEEL ->    R.drawable.power_pole_steel
    CONCRETE -> R.drawable.power_pole_concrete
}
