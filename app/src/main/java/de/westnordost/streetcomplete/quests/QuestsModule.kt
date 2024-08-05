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
import de.westnordost.streetcomplete.quests.kerb_height.AddKerbHeight
import de.westnordost.streetcomplete.quests.road_name.RoadNameSuggestionsSource
import de.westnordost.streetcomplete.screens.measure.ArSupportChecker
import de.westnordost.streetcomplete.util.ktx.getFeature
import org.koin.core.qualifier.named
import org.koin.dsl.module

val questsModule = module {
    factory { RoadNameSuggestionsSource(get()) }

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
) = QuestTypeRegistry(
    listOf(

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
        //GIG quests
        //1 to AddCrossing(),
        //8 to AddCrossingIsland(),
        // 10 to AddCrossingMarkings(),
        // 31 to AddSidewalkSurface(),
        // 27 to AddTactilePavingSteps(),
        // 32 to AddStepsIncline(),
        // 14 to AddTactilePavingCrosswalk(),
        19 to AddKerbHeight(),


        // 0 to OsmNoteQuestType,
        //
        // /* ↓ 1. solvable from a distance or while passing by -----------------------------------  */
        //
        // // bus stop quests
        // 1 to AddBusStopShelter(), // used by at least OsmAnd
        // 2 to AddBenchStatusOnBusStop(), // can be seen from across the street
        // 3 to AddBinStatusOnBusStop(), // can be seen from across the street
        // 4 to AddTactilePavingBusStop(), // requires you to be very close to it
        // 5 to AddBusStopName(), // requires text input
        // 6 to AddBusStopRef(), // requires text input
        // 7 to AddBusStopLit(), // at least during day requires to stand in it to see if there is a light in the shelter
        //
        // 8 to AddRailwayCrossingBarrier(), // useful for routing
        //
        // 9 to AddCarWashType(),
        //
        // 10 to AddBenchBackrest(),
        // 11 to AddAmenityCover(getFeature),
        //
        // 12 to AddBridgeStructure(),
        //
        // 13 to MarkCompletedBuildingConstruction(), // unlocks AddBuildingType which unlocks address and building detail quests
        //
        // // sport pitches
        // 14 to AddSport(),
        // 15 to AddPitchSurface(),
        // 16 to AddPitchLit(),
        //
        // // parking
        // 17 to AddParkingType(),
        // 18 to AddParkingAccess(), // used by OSM Carto, mapy.cz, OSMand, Sputnik etc
        // 19 to AddParkingFee(), // used by OsmAnd
        //
        // 20 to AddTrafficCalmingType(),
        //
        // // steps
        // 21 to AddHandrail(), // for accessibility of pedestrian routing, can be gathered when walking past
        // 22 to AddStepsRamp(),
        // 23 to AddStepsIncline(), // can be gathered while walking perpendicular to the way e.g. the other side of the road or when running/cycling past, confuses some people, so not as high as it theoretically should be
        // 158 to AddTactilePavingSteps(), // need to check top and bottom
        //
        // 24 to AddBicycleIncline(),
        //
        // 25 to AddMemorialType(), // sometimes a bit hard to decide between the different types (something something sculpture)
        //
        // 26 to AddReligionToPlaceOfWorship(), // icons on maps are different - OSM Carto, mapy.cz, OsmAnd, Sputnik etc
        // 27 to AddReligionToWaysideShrine(),
        //
        // 28 to AddPowerPolesMaterial(),
        //
        // 29 to AddIsBuildingUnderground(), // should be before AddHousenumber to avoid asking for underground buildings
        //
        // // motorcycle parking
        // 30 to AddMotorcycleParkingCover(),
        // 31 to AddMotorcycleParkingCapacity(), // counting + number input required but usually well visible
        //
        // // air pump, may require some checking within a garage forecourt
        // 32 to AddAirCompressor(),
        //
        // // recycling containers
        // 33 to AddRecyclingType(),
        // 34 to DetermineRecyclingGlass(), // because most recycling:glass=yes is a tagging mistake
        // 35 to AddRecyclingContainerMaterials(),
        //
        // // kerbs
        // 36 to AddKerbHeight(),
        // /*
        //     AddKerbHeight is deliberately before AddTactilePavingKerb:
        //     - Also should be visible while waiting to cross
        //     - Some people are not interpreting flush or lowered kerb as a kerb on their own,
        //       and would be confused about asking about tactile status on kerb without kerb
        //       but with this quest first they are OK with such interpretation
        //  */
        // 37 to AddTactilePavingKerb(), // Paving can be completed while waiting to cross
        //
        // // crossing quests: A little later because they are not all solvable from a distance
        // 38 to AddCrossing(),
        // 164 to AddCrossingSignals(),
        // 39 to AddCrossingIsland(), // can be done at a glance
        // 163 to AddCrossingMarkings(),
        // 41 to AddTactilePavingCrosswalk(),
        // 159 to AddCrossingKerbHeight(),
        // 42 to AddTrafficSignalsSound(), // Sound needs to be done as or after you're crossing
        // 43 to AddTrafficSignalsButton(),
        // 44 to AddTrafficSignalsVibration(),
        //
        // /* ↓ 2.solvable when right in front of it ----------------------------------------------- */
        // 45 to AddInformationToTourism(), // OSM Carto
        //
        // 46 to AddPoliceType(),
        //
        // 47 to AddPlaygroundAccess(),
        //
        // /* pulled up in priority to be before CheckExistence because this is basically the check
        //    whether the postbox is still there in countries in which it is enabled */
        // 48 to AddPostboxCollectionTimes(),
        // 49 to CheckExistence(getFeature),
        // 155 to AddGritBinSeasonal(),
        //
        // 50 to AddBoardType(),
        //
        // 51 to AddBarrierType(), // basically any more detailed rendering and routing: OSM Carto, mapy.cz, OSMand for start
        // 52 to AddBarrierOnPath(),
        // 53 to AddBarrierOnRoad(),
        // 54 to AddBicycleBarrierType(),
        // 55 to AddBicycleBarrierInstallation(),
        // 56 to AddStileType(),
        // 57 to AddStepCountStile(), // here to keep stile quest together - this quest will appear in low quest density anyway
        //
        // 58 to AddBollardType(), // useful for first responders
        //
        // 82 to AddSeating(), // easily visible from outside, but only seasonally
        //
        // 59 to AddSelfServiceLaundry(),
        //
        // 60 to AddGeneralFee(),
        //
        // 61 to AddDrinkingWater(), // used by AnyFinder
        // 62 to AddDrinkingWaterType(),
        //
        // 63 to AddCameraType(),
        //
        // 64 to AddFireHydrantType(),
        // 65 to AddFireHydrantPosition(),
        // 66 to AddFireHydrantDiameter(),
        // 67 to AddFireHydrantRef(),
        //
        // 160 to AddBbqFuel(),
        // /* ↓ 2.solvable when right in front of it but takes longer to input --------------------- */
        //
        // // bike parking/rental: would be higher up if not for bike parking/rental capacity which is usually not solvable when moving past
        // 68 to AddBikeParkingCover(), // used by OsmAnd in the object description
        // 69 to AddBikeRentalType(), // generally less overlap of possible types/fewer choices/simpler to answer
        // 70 to AddBikeParkingType(), // used by OsmAnd
        // 71 to AddBikeParkingAccess(),
        // 72 to AddBikeParkingFee(),
        // 73 to AddBikeRentalCapacity(), // less ambiguous than bike parking
        // 74 to AddBikeParkingCapacity(), // used by cycle map layer on osm.org, OsmAnd
        //
        // 167 to AddParcelLockerBrand(),
        // 168 to AddParcelLockerPickup(),
        // 169 to AddParcelLockerMailIn(),
        //
        // // address: usually only visible when just in front + sometimes requires to take "other answer"
        // 75 to AddHousenumber(),
        // 76 to AddAddressStreet(),
        //
        // // shops: text input / opening hours input take longer than other quests
        // 157 to AddHairdresserCustomers(), // almost always marked on sign outside
        // 78 to SpecifyShopType(), // above add place name as some brand presets will set the name too
        // 79 to CheckShopType(),
        // 80 to AddPlaceName(getFeature),
        // 77 to CheckOpeningHoursSigned(getFeature),
        // 81 to AddOpeningHours(getFeature),
        // 83 to AddBicyclePump(), // visible from the outside, but only during opening hours
        //
        // 84 to AddAtmOperator(),
        // 85 to AddAtmCashIn(),
        //
        // 86 to AddClothingBinOperator(),
        //
        // 87 to AddChargingStationCapacity(),
        // 88 to AddChargingStationOperator(),
        //
        // // postboxes (collection times are further up, see comment)
        // 89 to AddPostboxRoyalCypher(), // can be glanced across the road (if postbox facing the right way)
        // 90 to AddPostboxRef(), // requires text input and to be very close to the collection plate
        //
        // 91 to AddAccessPointRef(), // requires text input and to be very close to the collection plate
        //
        // 92 to AddWheelchairAccessOutside(),
        //
        // // road but information is visible usually at the beginning of the marked stretch of way
        // 93 to AddMaxWeight(), // used by OSRM and other routing engines
        // 94 to AddMaxHeight(), // OSRM and other routing engines
        // 95 to AddMaxPhysicalHeight(arSupportChecker), // same as above, best if it appears right after (if enabled)
        // 96 to AddRoadName(),
        // 97 to AddOneway(),
        //
        // 99 to AddEntrance(),
        // 100 to AddEntranceReference(),
        //
        // 166 to AddMopedAccess(),
        //
        // /* ↓ 3.quests that may need some exploration / walking around --------------------------- */
        //
        // // ferry: usually visible from looking at the boat, but not always...
        // 101 to AddFerryAccessPedestrian(),
        // 102 to AddFerryAccessMotorVehicle(),
        //
        // 103 to AddProhibitedForPedestrians(), // need to understand the pedestrian situation
        //
        // 104 to MarkCompletedHighwayConstruction(), // need to look the whole way
        //
        // 105 to AddSummitCross(), // summit markings are not necessarily directly at the peak, need to look around
        // 106 to AddSummitRegister(), // register is harder to find than cross
        //
        // 165 to AddTreeLeafType(), // may need to get close in trickier cases
        // 107 to AddForestLeafType(), // need to walk around in the highlighted section
        //
        // 108 to AddOrchardProduce(), // difficult to find out if the orchard does not carry fruits right now
        //
        // 109 to AddLevel(), // requires to search for the place on several levels (or at least find a mall map)
        //
        // 110 to AddAirConditioning(), // often visible from the outside across the street, if not, visible/feelable inside
        //
        // 111 to AddSmoking(), // often marked on the entrance, if not, visible/smellable inside
        //
        // /* ↓ 4.quests that may need to go inside ------------------------------------------------ */
        //
        // 112 to AddWheelchairAccessPublicTransport(), // need to look out for lifts etc, maybe even enter the station
        //
        // 113 to AddIsAmenityIndoor(getFeature), // need to go inside in case it is inside (or gone)
        // 161 to AddDefibrillatorLocation(), // need to go inside in case it is inside (or gone)
        //
        // // inside camping sites
        // 114 to AddCampType(),
        // 115 to AddCampDrinkingWater(),
        // 116 to AddCampShower(),
        // 117 to AddCampPower(),
        // 162 to AddSanitaryDumpStation(),
        //
        // // toilets
        // 118 to AddToiletAvailability(), // OSM Carto, shown in OsmAnd descriptions
        // 119 to AddToiletsFee(), // used by OsmAnd in the object description
        // 120 to AddBabyChangingTable(), // used by OsmAnd in the object description
        // 121 to AddWheelchairAccessToiletsPart(),
        // 122 to AddWheelchairAccessToilets(), // used by wheelmap, OsmAnd, Organic Maps
        //
        // // shop
        // 123 to AddBikeRepairAvailability(),
        // 124 to AddSecondHandBicycleAvailability(),
        // 125 to AddVegetarian(), // menus are often posted externally
        // 126 to AddVegan(),
        // 127 to AddHalal(), // there are ~ 100 times more Muslims than Jews
        // 128 to AddKosher(),
        // 129 to AddWheelchairAccessBusiness(), // used by wheelmap, OsmAnd, Organic Maps
        // 130 to AddInternetAccess(), // used by OsmAnd
        // 131 to AddAcceptsCards(), // this will often involve going inside and near the till
        // 132 to AddAcceptsCash(),
        //
        // 133 to AddFuelSelfService(),
        // 156 to CheckShopExistence(getFeature), // after opening hours and similar so they will be preferred if enabled

        /* ↓ 5.quests that are very numerous ---------------------------------------------------- */

        // roads
        //134 to AddLongFormSidewalk(""), // for any pedestrian routers, needs minimal thinking
        // 135 to AddRoadSurface(), // used by BRouter, OsmAnd, OSRM, graphhopper, HOT map style... - sometimes requires way to be split
        // 136 to AddTracktype(), // widely used in map rendering - OSM Carto, OsmAnd...
        // 137 to AddCycleway(getCountryInfoByLocation), // for any cyclist routers (and cyclist maps)
        // 138 to AddLanes(), // abstreet, certainly most routing engines - often requires way to be split
        //
        // // disabled completely because definition is too fuzzy/broad to be useful and easy to answer,
        // // see https://community.openstreetmap.org/t/shoulder-tag-is-confusing/5185
        // // 139 to AddShoulder(), // needs minimal thinking
        //
        //140 to AddRoadWidth(arSupportChecker),
        // 141 to AddRoadSmoothness(),
        // 142 to AddPathSmoothness(),
        //
        // // footways
        // 143 to AddPathSurface(), // used by OSM Carto, BRouter, OsmAnd, OSRM, graphhopper...
        // 144 to AddCyclewaySegregation(), // Cyclosm, Valhalla, Bike Citizens Bicycle Navigation...
        // 145 to AddFootwayPartSurface(),
        // 146 to AddCyclewayPartSurface(),
        // 147 to AddSidewalkSurface(),
        // 148 to AddCyclewayWidth(arSupportChecker), // should be after cycleway segregation
        //
        // /* should best be after road surface because it excludes unpaved roads, also, need to search
        //  * for the sign which is one reason why it is disabled by default */
        // 149 to AddMaxSpeed(),
        //
        // // buildings
        // 150 to AddBuildingType(),
        // 151 to AddBuildingLevels(),
        // 152 to AddRoofShape(getCountryInfoByLocation),
        //
        // 153 to AddStepCount(), // can only be gathered when walking along this way, also needs the most effort and least useful
        //
        // /* at the very last because it can be difficult to ascertain during day. used by OsmAnd if "Street lighting" is enabled. (Configure map, Map rendering, Details) */
        // 154 to AddWayLit(),
    )
)
