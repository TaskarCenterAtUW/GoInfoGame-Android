package de.westnordost.streetcomplete.overlays

import de.westnordost.countryboundaries.CountryBoundaries
import de.westnordost.osmfeatures.Feature
import de.westnordost.osmfeatures.FeatureDictionary
import de.westnordost.streetcomplete.data.meta.CountryInfo
import de.westnordost.streetcomplete.data.meta.CountryInfos
import de.westnordost.streetcomplete.data.meta.getByLocation
import de.westnordost.streetcomplete.data.osm.mapdata.Element
import de.westnordost.streetcomplete.data.osm.mapdata.LatLon
import de.westnordost.streetcomplete.data.overlays.OverlayRegistry
import de.westnordost.streetcomplete.overlays.things.ThingsOverlay
import de.westnordost.streetcomplete.util.ktx.getFeature
import de.westnordost.streetcomplete.util.ktx.getIds
import org.koin.core.qualifier.named
import org.koin.dsl.module

/* Each overlay is assigned an ordinal. This is used for serialization and is thus never changed,
*  even if the order of overlays is changed.  */
val overlaysModule = module {
    single {
        overlaysRegistry(
            { location ->
                val countryInfos = get<CountryInfos>()
                val countryBoundaries = get<Lazy<CountryBoundaries>>(named("CountryBoundariesLazy")).value
                countryInfos.getByLocation(countryBoundaries, location.longitude, location.latitude)
            },
            { location ->
                val countryBoundaries = get<Lazy<CountryBoundaries>>(named("CountryBoundariesLazy")).value
                countryBoundaries.getIds(location).firstOrNull()
            },
            { element ->
                get<Lazy<FeatureDictionary>>(named("FeatureDictionaryLazy")).value.getFeature(element)
            }
        )
    }
}

fun overlaysRegistry(
    getCountryInfoByLocation: (LatLon) -> CountryInfo,
    getCountryCodeByLocation: (LatLon) -> String?,
    getFeature: (Element) -> Feature?,
) = OverlayRegistry(listOf(

    0 to ThingsOverlay(getFeature)
))
