package de.westnordost.streetcomplete.data.workspace

object WorkSpaceTable {
    const val NAME = "work_spaces"

    object Columns {
        const val ID = "id"
        const val TITLE = "title"
        const val QUESTS = "quests"
        const val TYPE = "type"
        const val EXTERNAL_APP_ACCESS = "externalAppAccess"
        // from WorkspaceDetailsResponse.overrideConflicts (nullable there; null/missing means
        // RESOLVE mode). 0 = RESOLVE (default; shows the per-tag conflict-resolution dialog), 1 =
        // OVERRIDE (auto-prefer the app's own value on a tag conflict, no dialog). Only ever
        // written by WorkspaceDao.updateOverrideConflicts, called when workspace details are
        // fetched - not by put()'s list sync, since the workspace list endpoint doesn't return
        // this field.
        const val OVERRIDE_CONFLICTS = "overrideConflicts"
    }

    const val CREATE = """
        CREATE TABLE $NAME (
            ${Columns.ID} int PRIMARY KEY,
            ${Columns.TITLE} varchar(255) NOT NULL,
            ${Columns.QUESTS}  text,
            ${Columns.TYPE} varchar(255) NOT NULL,
            ${Columns.EXTERNAL_APP_ACCESS} int NOT NULL,
            ${Columns.OVERRIDE_CONFLICTS} int NOT NULL DEFAULT 0
        );
    """
}
