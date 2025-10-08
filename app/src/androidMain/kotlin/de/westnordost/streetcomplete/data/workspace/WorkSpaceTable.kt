package de.westnordost.streetcomplete.data.workspace

object WorkSpaceTable {
    const val NAME = "work_spaces"

    object Columns {
        const val ID = "id"
        const val TITLE = "title"
        const val QUESTS = "quests"
        const val TYPE = "type"
        const val EXTERNAL_APP_ACCESS = "externalAppAccess"
    }

    const val CREATE = """
        CREATE TABLE $NAME (
            ${Columns.ID} int PRIMARY KEY,
            ${Columns.TITLE} varchar(255) NOT NULL,
            ${Columns.QUESTS}  text,
            ${Columns.TYPE} varchar(255) NOT NULL,
            ${Columns.EXTERNAL_APP_ACCESS} int NOT NULL
        );
    """
}
