package de.westnordost.streetcomplete.data.osm.edits.create_feature

object FeaturePhotosTable {
    const val NAME = "osm_element_edit_photos"

    object Columns {
        const val ID = "id"
        const val EDIT_ID = "edit_id"
        const val PHOTO_PATHS = "photo_paths"
        /** JSON list of nullable Floats, index-aligned with PHOTO_PATHS - the compass bearing the
         *  camera was facing at capture time for each photo (null: unknown), or absent/null on
         *  rows written before this column existed (unknown bearings - see FeaturePhotosDao.get). */
        const val PHOTO_BEARINGS = "photo_bearings"
        /** JSON list of nullable Strings, index-aligned with PHOTO_PATHS - the long-form question
         *  each photo was taken for, or absent/null on rows written before this column existed */
        const val PHOTO_QUESTIONS = "photo_questions"
        /** How many times uploading this edit's photo(s) has failed in a row - see
         *  ElementEditsUploader.uploadPendingPhotos and StuckPhotoUploadNotice. Reset to 0 once
         *  the user picks "keep trying" on the resulting notice. */
        const val UPLOAD_ATTEMPTS = "upload_attempts"
        const val WORKSPACE_ID = "workspace_id"
    }

    const val CREATE = """
        CREATE TABLE $NAME (
            ${Columns.ID} INTEGER PRIMARY KEY AUTOINCREMENT,
            ${Columns.EDIT_ID} int NOT NULL,
            ${Columns.PHOTO_PATHS} text NOT NULL,
            ${Columns.PHOTO_BEARINGS} text,
            ${Columns.PHOTO_QUESTIONS} text,
            ${Columns.UPLOAD_ATTEMPTS} int NOT NULL DEFAULT 0,
            ${Columns.WORKSPACE_ID} int NOT NULL
        );
    """
}
