package com.tcatuw.goinfo.util.ktx

import de.westnordost.countryboundaries.CountryBoundaries
import com.tcatuw.goinfo.data.osm.mapdata.BoundingBox
import com.tcatuw.goinfo.data.osm.mapdata.LatLon
import com.tcatuw.goinfo.data.quest.AllCountries
import com.tcatuw.goinfo.data.quest.AllCountriesExcept
import com.tcatuw.goinfo.data.quest.Countries
import com.tcatuw.goinfo.data.quest.NoCountriesExcept

/** Whether the given position is in any of the given countries */
fun CountryBoundaries.isInAny(pos: LatLon, countries: Countries) = when (countries) {
    is AllCountries -> true
    is AllCountriesExcept -> !isInAny(pos, countries.exceptions)
    is NoCountriesExcept -> isInAny(pos, countries.exceptions)
}

/** Whether the given bounding box at least intersects with the given countries */
fun CountryBoundaries.intersects(bbox: BoundingBox, countries: Countries) = when (countries) {
    is AllCountries -> true
    is AllCountriesExcept -> !getContainingIds(bbox).containsAny(countries.exceptions)
    is NoCountriesExcept -> getIntersectingIds(bbox).containsAny(countries.exceptions)
}

fun CountryBoundaries.getContainingIds(bounds: BoundingBox): Set<String> = getContainingIds(
    bounds.min.longitude, bounds.min.latitude, bounds.max.longitude, bounds.max.latitude
)

fun CountryBoundaries.getIntersectingIds(bounds: BoundingBox): Set<String> = getIntersectingIds(
    bounds.min.longitude, bounds.min.latitude, bounds.max.longitude, bounds.max.latitude
)

fun CountryBoundaries.isInAny(pos: LatLon, ids: Collection<String>) = isInAny(
    pos.longitude, pos.latitude, ids
)

fun CountryBoundaries.getIds(pos: LatLon): MutableList<String> = getIds(pos.longitude, pos.latitude)
