package com.tcatuw.goinfo.quests.bike_parking_type

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.quests.bike_parking_type.BikeParkingType.*
import com.tcatuw.goinfo.view.image_select.Item

fun BikeParkingType.asItem() = Item(this, iconResId, titleResId)

private val BikeParkingType.titleResId: Int get() = when (this) {
    STANDS ->           R.string.quest_bicycle_parking_type_stand
    WALL_LOOPS ->       R.string.quest_bicycle_parking_type_wheelbender
    SHED ->             R.string.quest_bicycle_parking_type_shed
    LOCKERS ->          R.string.quest_bicycle_parking_type_locker
    BUILDING ->         R.string.quest_bicycle_parking_type_building
    HANDLEBAR_HOLDER -> R.string.quest_bicycle_parking_type_handlebarholder
    TWO_TIER ->         R.string.quest_bicycle_parking_type_two_tier
}

private val BikeParkingType.iconResId: Int get() = when (this) {
    STANDS ->           R.drawable.bicycle_parking_type_stand
    WALL_LOOPS ->       R.drawable.bicycle_parking_type_wheelbenders
    SHED ->             R.drawable.bicycle_parking_type_shed
    LOCKERS ->          R.drawable.bicycle_parking_type_lockers
    BUILDING ->         R.drawable.bicycle_parking_type_building
    HANDLEBAR_HOLDER -> R.drawable.bicycle_parking_type_handlebarholder
    TWO_TIER ->         R.drawable.bicycle_parking_type_two_tier
}
