package de.westnordost.streetcomplete.data.osm.edits.create_feature

object FeaturePhotosTable {
    const val NAME = "osm_element_edit_photos"

    object Columns {
        const val ID = "id"
        const val EDIT_ID = "edit_id"
        const val PHOTO_PATHS = "photo_paths"
        /** JSON list of Floats, index-aligned with PHOTO_PATHS - the compass bearing the device
         *  was facing at capture time for each photo, or absent/null on rows written before this
         *  column existed (treated as all-zero bearings for those - see FeaturePhotosDao.get). */
        const val PHOTO_BEARINGS = "photo_bearings"
        const val WORKSPACE_ID = "workspace_id"
    }

    const val CREATE = """
        CREATE TABLE $NAME (
            ${Columns.ID} INTEGER PRIMARY KEY AUTOINCREMENT,
            ${Columns.EDIT_ID} int NOT NULL,
            ${Columns.PHOTO_PATHS} text NOT NULL,
            ${Columns.PHOTO_BEARINGS} text,
            ${Columns.WORKSPACE_ID} int NOT NULL
        );
    """
}
