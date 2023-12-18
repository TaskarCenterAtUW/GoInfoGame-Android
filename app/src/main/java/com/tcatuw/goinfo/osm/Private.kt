package com.tcatuw.goinfo.osm

import com.tcatuw.goinfo.data.elementfilter.toElementFilterExpression
import com.tcatuw.goinfo.data.osm.mapdata.Element

private val isPrivateOnFootFilter by lazy { """
    nodes, ways, relations with
      access ~ private|no
      and (!foot or foot ~ private|no)
""".toElementFilterExpression() }

fun isPrivateOnFoot(element: Element): Boolean = isPrivateOnFootFilter.matches(element)
