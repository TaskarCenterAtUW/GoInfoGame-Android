package com.tcatuw.goinfo.quests.place_name

import com.tcatuw.goinfo.osm.LocalizedName

sealed interface PlaceNameAnswer

data class PlaceName(val localizedNames: List<LocalizedName>) : PlaceNameAnswer
object NoPlaceNameSign : PlaceNameAnswer
