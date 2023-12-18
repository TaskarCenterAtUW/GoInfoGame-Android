package com.tcatuw.goinfo.quests.barrier_bicycle_barrier_installation

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.quests.barrier_bicycle_barrier_installation.BicycleBarrierInstallation.FIXED
import com.tcatuw.goinfo.quests.barrier_bicycle_barrier_installation.BicycleBarrierInstallation.OPENABLE
import com.tcatuw.goinfo.quests.barrier_bicycle_barrier_installation.BicycleBarrierInstallation.REMOVABLE
import com.tcatuw.goinfo.view.image_select.Item

fun BicycleBarrierInstallation.asItem() = Item(this, iconResId, titleResId)

private val BicycleBarrierInstallation.titleResId: Int get() = when (this) {
    FIXED ->     R.string.quest_barrier_bicycle_installation_fixed
    OPENABLE ->  R.string.quest_barrier_bicycle_installation_openable
    REMOVABLE -> R.string.quest_barrier_bicycle_installation_removable
}

private val BicycleBarrierInstallation.iconResId: Int get() = when (this) {
    FIXED ->     R.drawable.barrier_bicycle_installation_fixed
    OPENABLE ->  R.drawable.barrier_bicycle_installation_openable
    REMOVABLE -> R.drawable.barrier_bicycle_installation_removable
}
