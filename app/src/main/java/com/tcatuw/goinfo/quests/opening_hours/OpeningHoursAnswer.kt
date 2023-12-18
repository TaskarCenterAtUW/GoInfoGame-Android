package com.tcatuw.goinfo.quests.opening_hours

import com.tcatuw.goinfo.osm.opening_hours.parser.OpeningHoursRuleList

sealed interface OpeningHoursAnswer

data class RegularOpeningHours(val hours: OpeningHoursRuleList) : OpeningHoursAnswer
object AlwaysOpen : OpeningHoursAnswer
data class DescribeOpeningHours(val text: String) : OpeningHoursAnswer
object NoOpeningHoursSign : OpeningHoursAnswer
