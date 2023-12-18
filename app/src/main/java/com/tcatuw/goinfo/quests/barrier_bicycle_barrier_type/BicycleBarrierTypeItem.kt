package com.tcatuw.goinfo.quests.barrier_bicycle_barrier_type

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.quests.barrier_bicycle_barrier_type.BicycleBarrierType.DIAGONAL
import com.tcatuw.goinfo.quests.barrier_bicycle_barrier_type.BicycleBarrierType.DOUBLE
import com.tcatuw.goinfo.quests.barrier_bicycle_barrier_type.BicycleBarrierType.SINGLE
import com.tcatuw.goinfo.quests.barrier_bicycle_barrier_type.BicycleBarrierType.TILTED
import com.tcatuw.goinfo.quests.barrier_bicycle_barrier_type.BicycleBarrierType.TRIPLE
import com.tcatuw.goinfo.view.image_select.Item

fun BicycleBarrierType.asItem() = Item(this, iconResId, titleResId)

private val BicycleBarrierType.titleResId: Int get() = when (this) {
    SINGLE ->   R.string.quest_barrier_bicycle_type_single
    DOUBLE ->   R.string.quest_barrier_bicycle_type_double
    TRIPLE ->   R.string.quest_barrier_bicycle_type_multiple
    DIAGONAL -> R.string.quest_barrier_bicycle_type_diagonal
    TILTED ->   R.string.quest_barrier_bicycle_type_tilted
}

private val BicycleBarrierType.iconResId: Int get() = when (this) {
    SINGLE ->   R.drawable.barrier_bicycle_barrier_single
    DOUBLE ->   R.drawable.barrier_bicycle_barrier_double
    TRIPLE ->   R.drawable.barrier_bicycle_barrier_triple
    DIAGONAL -> R.drawable.barrier_bicycle_barrier_diagonal
    TILTED ->   R.drawable.barrier_bicycle_barrier_tilted
}
