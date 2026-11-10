package de.westnordost.streetcomplete.data.osmnotes.edits

object NoteEditsTable {
    const val NAME = "osm_note_edits"

    object Columns {
        const val ID = "id"
        const val NOTE_ID = "note_id"
        const val LATITUDE = "latitude"
        const val LONGITUDE = "longitude"
        const val CREATED_TIMESTAMP = "created"
        const val IS_SYNCED = "synced"
        const val TYPE = "type"
        const val TEXT = "text"
        const val IMAGE_PATHS = "image_paths"
        const val IMAGES_NEED_ACTIVATION = "images_need_activation"
        /** JSON list of nullable Floats, index-aligned with IMAGE_PATHS - the bearing of each
         *  photo, or null on rows written before this column existed */
        const val IMAGE_BEARINGS = "image_bearings"
        /** JSON list of nullable Longs, index-aligned with IMAGE_PATHS - when each photo was taken
         *  (epoch ms), or null on rows written before this column existed */
        const val IMAGE_CAPTURE_TIMES = "image_capture_times"
        const val TRACK = "track"
        const val WORKSPACE_ID = "workspace_id"
    }

    const val CREATE = """
        CREATE TABLE $NAME (
            ${Columns.ID} INTEGER PRIMARY KEY AUTOINCREMENT,
            ${Columns.NOTE_ID} int NOT NULL,
            ${Columns.LATITUDE} double NOT NULL,
            ${Columns.LONGITUDE} double NOT NULL,
            ${Columns.CREATED_TIMESTAMP} int NOT NULL,
            ${Columns.IS_SYNCED} int NOT NULL,
            ${Columns.TEXT} text,
            ${Columns.IMAGE_PATHS} text NOT NULL,
            ${Columns.IMAGES_NEED_ACTIVATION} int NOT NULL,
            ${Columns.IMAGE_BEARINGS} text,
            ${Columns.IMAGE_CAPTURE_TIMES} text,
            ${Columns.TRACK} text NOT NULL,
            ${Columns.TYPE} varchar(255),
            ${Columns.WORKSPACE_ID} int NOT NULL
        );
    """

    const val SPATIAL_INDEX_CREATE = """
        CREATE INDEX osm_note_edits_spatial_index ON $NAME (
            ${Columns.LATITUDE},
            ${Columns.LONGITUDE}
        );
    """

    const val NOTE_ID_INDEX_CREATE = """
        CREATE INDEX osm_note_edits_note_index ON $NAME (${Columns.NOTE_ID});
    """
}
