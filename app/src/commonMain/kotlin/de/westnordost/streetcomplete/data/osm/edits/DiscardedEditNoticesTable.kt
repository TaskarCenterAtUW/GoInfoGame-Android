package de.westnordost.streetcomplete.data.osm.edits

object DiscardedEditNoticesTable {
    const val NAME = "osm_discarded_edit_notices"

    object Columns {
        const val ID = "id"
        const val QUEST_TYPE = "quest_type"
        const val ELEMENT_TYPE = "element_type"
        const val ELEMENT_ID = "element_id"
        const val REASON = "reason"
        const val LATITUDE = "latitude"
        const val LONGITUDE = "longitude"
        const val CREATED_TIMESTAMP = "created"
        const val WORKSPACE_ID = "workspace_id"
    }

    const val CREATE = """
        CREATE TABLE $NAME (
            ${Columns.ID} INTEGER PRIMARY KEY AUTOINCREMENT,
            ${Columns.QUEST_TYPE} varchar(255) NOT NULL,
            ${Columns.ELEMENT_TYPE} varchar(255),
            ${Columns.ELEMENT_ID} int,
            ${Columns.REASON} varchar(255) NOT NULL,
            ${Columns.LATITUDE} double NOT NULL,
            ${Columns.LONGITUDE} double NOT NULL,
            ${Columns.CREATED_TIMESTAMP} int NOT NULL,
            ${Columns.WORKSPACE_ID} int NOT NULL
        );
    """
}
