package com.tcatuw.goinfo.quests.postbox_collection_times

import com.tcatuw.goinfo.osm.opening_hours.parser.OpeningHoursRuleList

sealed interface CollectionTimesAnswer

data class CollectionTimes(val times: OpeningHoursRuleList) : CollectionTimesAnswer
object NoCollectionTimesSign : CollectionTimesAnswer
