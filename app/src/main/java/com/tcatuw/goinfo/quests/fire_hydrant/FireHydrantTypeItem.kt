package com.tcatuw.goinfo.quests.fire_hydrant

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.quests.fire_hydrant.FireHydrantType.PILLAR
import com.tcatuw.goinfo.quests.fire_hydrant.FireHydrantType.PIPE
import com.tcatuw.goinfo.quests.fire_hydrant.FireHydrantType.UNDERGROUND
import com.tcatuw.goinfo.quests.fire_hydrant.FireHydrantType.WALL
import com.tcatuw.goinfo.view.image_select.Item

fun FireHydrantType.asItem() = Item(this, iconResId, titleResId)

private val FireHydrantType.titleResId: Int get() = when (this) {
    PILLAR ->      R.string.quest_fireHydrant_type_pillar
    UNDERGROUND -> R.string.quest_fireHydrant_type_underground
    WALL ->        R.string.quest_fireHydrant_type_wall
    PIPE ->        R.string.quest_fireHydrant_type_pipe
}

private val FireHydrantType.iconResId: Int get() = when (this) {
    PILLAR ->      R.drawable.fire_hydrant_pillar
    UNDERGROUND -> R.drawable.fire_hydrant_underground
    WALL ->        R.drawable.fire_hydrant_wall
    PIPE ->        R.drawable.fire_hydrant_pipe
}
