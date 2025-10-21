package de.westnordost.streetcomplete.quests

import de.westnordost.countryboundaries.CountryBoundaries
import de.westnordost.osmfeatures.Feature
import de.westnordost.osmfeatures.FeatureDictionary
import de.westnordost.streetcomplete.data.meta.CountryInfo
import de.westnordost.streetcomplete.data.meta.CountryInfos
import de.westnordost.streetcomplete.data.meta.getByLocation
import de.westnordost.streetcomplete.data.osm.mapdata.Element
import de.westnordost.streetcomplete.data.osm.mapdata.LatLon
import de.westnordost.streetcomplete.data.quest.QuestTypeRegistry
import de.westnordost.streetcomplete.quests.note_discussion.OsmNoteQuestType
import de.westnordost.streetcomplete.screens.measure.ArSupportChecker
import de.westnordost.streetcomplete.util.ktx.getFeature
import org.koin.core.qualifier.named
import org.koin.dsl.module

val questsModule = module {
    single {
        questTypeRegistry(
            get(),
            { location ->
                val countryInfos = get<CountryInfos>()
                val countryBoundaries = get<Lazy<CountryBoundaries>>(named("CountryBoundariesLazy")).value
                countryInfos.getByLocation(countryBoundaries, location.longitude, location.latitude)
            },
            { element ->
                get<Lazy<FeatureDictionary>>(named("FeatureDictionaryLazy")).value.getFeature(element)
            }
        )
    }
}

fun questTypeRegistry(
    arSupportChecker: ArSupportChecker,
    getCountryInfoByLocation: (LatLon) -> CountryInfo,
    getFeature: (Element) -> Feature?,
) = QuestTypeRegistry(listOf(

    /*
        The quest types are primarily sorted by how easy they can be solved:
        1. quests that are solvable from a distance or while passing by (fast)
        2. quests that require to be right in front of it (e.g. because it is small, you need to
          look for it or read text)
        3. quests that require some exploration or walking around to check (e.g. walking down the
          whole road to find the cycleway is the same along the whole way)
        4. quests that require to go inside, i.e. deviate from your walking route by a lot just
          to solve the quest
        5. quests that come in heaps (are spammy) come last: e.g. building type etc.

        The ordering within this primary sort order shall be whatever is faster so solve first:

        a. Yes/No quests, easy selections first,
        b. number and text inputs later,
        c. complex inputs (opening hours, ...) last. Quests that e.g. often require the way to be
          split up first are in effect also slow to answer

        The order can be watered down somewhat if it means that quests that usually apply to the
        same elements are in direct succession because we want to avoid that users are half-done
        answering all the quests for one element and then can't solve the last anymore because it
        is visually obscured by another quest.

        Finally, importance of the quest can still play a factor, but only secondarily.

        ---

        Each quest is assigned an ordinal. This is used for serialization and is thus never changed,
        even if the quest's order is changed or new quests are added somewhere in the middle. Each new
        quest always gets a new sequential ordinal.
     */

    /* always first: notes - they mark a mistake in the data so potentially every quest for that
    element is based on wrong data while the note is not resolved */
    0 to OsmNoteQuestType,
))
