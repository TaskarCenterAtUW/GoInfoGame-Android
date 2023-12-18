package com.tcatuw.goinfo.quests.fire_hydrant_diameter
import com.tcatuw.goinfo.quests.fire_hydrant_diameter.FireHydrantDiameterMeasurementUnit.INCH
import com.tcatuw.goinfo.quests.fire_hydrant_diameter.FireHydrantDiameterMeasurementUnit.MILLIMETER

sealed interface FireHydrantDiameterAnswer

object NoFireHydrantDiameterSign : FireHydrantDiameterAnswer
data class FireHydrantDiameter(val value: Int, val unit: FireHydrantDiameterMeasurementUnit) : FireHydrantDiameterAnswer {
    fun toOsmValue() = value.toString() + when (unit) {
        MILLIMETER -> ""
        INCH -> "\""
    }
}

enum class FireHydrantDiameterMeasurementUnit { MILLIMETER, INCH }
