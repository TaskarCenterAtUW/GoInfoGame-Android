package de.westnordost.streetcomplete.data.logs

import de.westnordost.streetcomplete.data.ApplicationDbTestCase
import de.westnordost.streetcomplete.data.DatabaseInitializer
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class HttpLogsMigrationTest : ApplicationDbTestCase() {
    private lateinit var dao: LogsDao

    @BeforeTest fun createDao() {
        dao = LogsDao(database)
    }

    @Test fun upgrading_to_29_deletes_stored_http_logs_only() {
        listOf("OsmClient", "KtorClient", "Download", "QuestAutoSyncer").forEach { tag ->
            dao.add(LogMessage(LogLevel.DEBUG, tag, "message of $tag", null, 1))
        }

        DatabaseInitializer.onUpgrade(database, 28, 29)

        assertEquals(listOf("Download", "QuestAutoSyncer"), dao.getAll().map { it.tag }.sorted())
    }

    @Test fun upgrading_from_before_29_to_later_deletes_stored_http_logs() {
        dao.add(LogMessage(LogLevel.DEBUG, "OsmClient", "<osm>...</osm>", null, 1))

        DatabaseInitializer.onUpgrade(database, 28, 30)

        assertEquals(emptyList(), dao.getAll())
    }
}
