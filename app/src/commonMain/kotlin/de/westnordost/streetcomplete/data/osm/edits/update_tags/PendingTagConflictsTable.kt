package de.westnordost.streetcomplete.data.osm.edits.update_tags

object PendingTagConflictsTable {
    const val NAME = "osm_pending_tag_conflicts"

    object Columns {
        const val ID = "id"
        const val EDIT_ID = "edit_id"
        const val ELEMENT_TYPE = "element_type"
        const val ELEMENT_ID = "element_id"
        const val TAG_KEY = "tag_key"
        const val MINE_VALUE = "mine_value"
        const val THEIRS_VALUE = "theirs_value"
        const val QUEST_TYPE = "quest_type"
        const val SOURCE = "source"
        const val LATITUDE = "latitude"
        const val LONGITUDE = "longitude"
        const val CREATED_TIMESTAMP = "created"
        const val WORKSPACE_ID = "workspace_id"
    }

    const val CREATE = """
        CREATE TABLE $NAME (
            ${Columns.ID} INTEGER PRIMARY KEY AUTOINCREMENT,
            ${Columns.EDIT_ID} int NOT NULL,
            ${Columns.ELEMENT_TYPE} varchar(255) NOT NULL,
            ${Columns.ELEMENT_ID} int NOT NULL,
            ${Columns.TAG_KEY} varchar(255) NOT NULL,
            ${Columns.MINE_VALUE} varchar(255),
            ${Columns.THEIRS_VALUE} varchar(255),
            ${Columns.QUEST_TYPE} varchar(255) NOT NULL,
            ${Columns.SOURCE} varchar(255) NOT NULL,
            ${Columns.LATITUDE} double NOT NULL,
            ${Columns.LONGITUDE} double NOT NULL,
            ${Columns.CREATED_TIMESTAMP} int NOT NULL,
            ${Columns.WORKSPACE_ID} int NOT NULL
        );
    """
}
