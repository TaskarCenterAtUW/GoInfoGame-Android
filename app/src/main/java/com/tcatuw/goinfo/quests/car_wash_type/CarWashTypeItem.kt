package com.tcatuw.goinfo.quests.car_wash_type

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.quests.car_wash_type.CarWashType.AUTOMATED
import com.tcatuw.goinfo.quests.car_wash_type.CarWashType.SELF_SERVICE
import com.tcatuw.goinfo.quests.car_wash_type.CarWashType.SERVICE
import com.tcatuw.goinfo.view.image_select.Item

fun CarWashType.asItem() = Item(this, iconResId, titleResId)

private val CarWashType.titleResId: Int get() = when (this) {
    AUTOMATED ->    R.string.quest_carWashType_automated
    SELF_SERVICE -> R.string.quest_carWashType_selfService
    SERVICE ->      R.string.quest_carWashType_service
}

private val CarWashType.iconResId: Int get() = when (this) {
    AUTOMATED ->    R.drawable.car_wash_automated
    SELF_SERVICE -> R.drawable.car_wash_self_service
    SERVICE ->      R.drawable.car_wash_service
}
