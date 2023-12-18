package com.tcatuw.goinfo.quests.address

import com.tcatuw.goinfo.osm.address.AddressNumber

sealed interface HouseNumberAnswer

data class AddressNumberOrName(val number: AddressNumber?, val name: String?) : HouseNumberAnswer
object WrongBuildingType : HouseNumberAnswer
