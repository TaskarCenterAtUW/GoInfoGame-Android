package com.tcatuw.goinfo.quests.construction

import com.tcatuw.goinfo.osm.SURVEY_MARK_KEY
import com.tcatuw.goinfo.osm.Tags

fun removeTagsDescribingConstruction(tags: Tags) {
    tags.remove("construction")
    tags.remove("source:construction")
    tags.remove("opening_date")
    tags.remove("source:opening_date")
    tags.remove(SURVEY_MARK_KEY)
    tags.remove("source:$SURVEY_MARK_KEY")
}
