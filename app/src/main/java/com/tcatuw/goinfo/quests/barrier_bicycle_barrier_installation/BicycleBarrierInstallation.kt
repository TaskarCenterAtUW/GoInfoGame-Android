package com.tcatuw.goinfo.quests.barrier_bicycle_barrier_installation

sealed interface BicycleBarrierInstallationAnswer

enum class BicycleBarrierInstallation(val osmValue: String) : BicycleBarrierInstallationAnswer {
    FIXED("fixed"),
    OPENABLE("openable"),
    REMOVABLE("removable"),
}

object BarrierTypeIsNotBicycleBarrier : BicycleBarrierInstallationAnswer
