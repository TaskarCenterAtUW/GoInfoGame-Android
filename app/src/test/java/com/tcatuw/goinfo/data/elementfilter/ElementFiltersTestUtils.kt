package com.tcatuw.goinfo.data.elementfilter

import com.tcatuw.goinfo.data.elementfilter.filters.ElementFilter
import com.tcatuw.goinfo.testutils.node
import com.tcatuw.goinfo.util.ktx.minusInSystemTimeZone
import com.tcatuw.goinfo.util.ktx.now
import com.tcatuw.goinfo.util.ktx.toEpochMilli
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

/** Returns the date x days in the past */
fun dateDaysAgo(daysAgo: Float): LocalDate =
    LocalDateTime.now().minusInSystemTimeZone((daysAgo * 24).toLong(), DateTimeUnit.HOUR).date

fun ElementFilter.matches(tags: Map<String, String>, date: LocalDate? = null): Boolean =
    matches(node(tags = tags, timestamp = date?.toEpochMilli()))
