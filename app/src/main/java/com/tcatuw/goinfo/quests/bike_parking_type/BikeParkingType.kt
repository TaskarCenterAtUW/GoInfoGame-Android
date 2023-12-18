package com.tcatuw.goinfo.quests.bike_parking_type

enum class BikeParkingType(val osmValue: String) {
    STANDS("stands"),
    WALL_LOOPS("wall_loops"),
    SHED("shed"),
    LOCKERS("lockers"),
    BUILDING("building"),
    HANDLEBAR_HOLDER("handlebar_holder"),
    TWO_TIER("two-tier")
}
