package com.tcatuw.goinfo.overlays

import de.westnordost.countryboundaries.CountryBoundaries
import de.westnordost.osmfeatures.Feature
import de.westnordost.osmfeatures.FeatureDictionary
import com.tcatuw.goinfo.data.meta.CountryInfo
import com.tcatuw.goinfo.data.meta.CountryInfos
import com.tcatuw.goinfo.data.meta.getByLocation
import com.tcatuw.goinfo.data.osm.mapdata.LatLon
import com.tcatuw.goinfo.data.overlays.OverlayRegistry
import com.tcatuw.goinfo.overlays.address.AddressOverlay
import com.tcatuw.goinfo.overlays.cycleway.CyclewayOverlay
import com.tcatuw.goinfo.overlays.shops.ShopsOverlay
import com.tcatuw.goinfo.overlays.sidewalk.SidewalkOverlay
import com.tcatuw.goinfo.overlays.street_parking.StreetParkingOverlay
import com.tcatuw.goinfo.overlays.surface.SurfaceOverlay
import com.tcatuw.goinfo.overlays.way_lit.WayLitOverlay
import com.tcatuw.goinfo.util.ktx.getFeature
import com.tcatuw.goinfo.util.ktx.getIds
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.util.concurrent.FutureTask

/* Each overlay is assigned an ordinal. This is used for serialization and is thus never changed,
*  even if the order of overlays is changed.  */
val overlaysModule = module {
    single {
        overlaysRegistry(
            { location ->
                val countryInfos = get<CountryInfos>()
                val countryBoundaries = get<FutureTask<CountryBoundaries>>(named("CountryBoundariesFuture")).get()
                countryInfos.getByLocation(countryBoundaries, location.longitude, location.latitude)
            },
            { location ->
                val countryBoundaries = get<FutureTask<CountryBoundaries>>(named("CountryBoundariesFuture")).get()
                countryBoundaries.getIds(location).firstOrNull()
            },
            { tags ->
                get<FutureTask<FeatureDictionary>>(named("FeatureDictionaryFuture"))
                .get().getFeature(tags)
            }
        )
    }
}

fun overlaysRegistry(
    getCountryInfoByLocation: (location: LatLon) -> CountryInfo,
    getCountryCodeByLocation: (location: LatLon) -> String?,
    getFeature: (tags: Map<String, String>) -> Feature?,
) = OverlayRegistry(listOf(

    0 to WayLitOverlay(),
    6 to SurfaceOverlay(),
    1 to SidewalkOverlay(),
    5 to CyclewayOverlay(getCountryInfoByLocation),
    2 to StreetParkingOverlay(),
    3 to AddressOverlay(getCountryCodeByLocation),
    4 to ShopsOverlay(getFeature),
))
