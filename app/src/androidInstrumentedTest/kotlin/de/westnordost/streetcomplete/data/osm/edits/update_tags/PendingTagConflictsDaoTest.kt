package de.westnordost.streetcomplete.data.osm.edits.update_tags

import androidx.test.platform.app.InstrumentationRegistry
import com.russhwolf.settings.SharedPreferencesSettings
import de.westnordost.streetcomplete.data.AllEditTypes
import de.westnordost.streetcomplete.data.ApplicationDbTestCase
import de.westnordost.streetcomplete.data.osm.mapdata.ElementType
import de.westnordost.streetcomplete.data.osm.mapdata.LatLon
import de.westnordost.streetcomplete.data.osm.osmquests.TestQuestType
import de.westnordost.streetcomplete.data.preferences.Preferences
import de.westnordost.streetcomplete.data.quest.QuestTypeRegistry
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Pending tag conflicts (RESOLVE mode) must survive the app being killed mid-decision, and are
 *  scoped to the workspace that is currently open. */
class PendingTagConflictsDaoTest : ApplicationDbTestCase() {

    private val questType = TestQuestType()
    private lateinit var preferences: Preferences
    private lateinit var dao: PendingTagConflictsDao

    @BeforeTest fun createDao() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        preferences = Preferences(SharedPreferencesSettings(context.getSharedPreferences(PREFS, 0)))
        preferences.workspaceId = 7
        dao = PendingTagConflictsDao(database, AllEditTypes(mutableListOf(QuestTypeRegistry(listOf(1 to questType)))), preferences)
    }

    @AfterTest fun clearPrefs() {
        InstrumentationRegistry.getInstrumentation().targetContext.deleteSharedPreferences(PREFS)
    }

    private fun conflict(
        editId: Long = 1,
        key: String = "surface",
        mine: String? = "concrete",
        theirs: String? = "gravel",
        timestamp: Long = 100,
    ) = PendingTagConflict(
        id = 0, editId = editId, elementType = ElementType.WAY, elementId = 42,
        tagKey = key, mineValue = mine, theirsValueAtDetection = theirs,
        editType = questType, source = "survey", position = LatLon(47.6, -122.3),
        createdTimestamp = timestamp, workspaceId = 7,
    )

    @Test fun addAndGet_roundTripsEveryField() {
        val added = dao.add(conflict())
        assertTrue(added.id > 0)
        assertEquals(listOf(added), dao.getAll())
    }

    @Test fun nullValues_areKept() {
        // mine = null: the user deleted the tag; theirs = null: the server has no such tag
        val added = dao.add(conflict(mine = null, theirs = null))
        assertEquals(listOf(added), dao.getAll())
    }

    @Test fun getAll_isOldestFirst() {
        dao.add(conflict(key = "b", timestamp = 300))
        dao.add(conflict(key = "a", timestamp = 100))
        dao.add(conflict(key = "c", timestamp = 200))
        assertEquals(listOf("a", "c", "b"), dao.getAll().map { it.tagKey })
    }

    @Test fun delete() {
        val a = dao.add(conflict(key = "a"))
        val b = dao.add(conflict(key = "b"))
        assertTrue(dao.delete(a.id))
        assertFalse(dao.delete(a.id))
        assertEquals(listOf(b), dao.getAll())
        assertEquals(1, dao.getCount())
    }

    @Test fun onlyTheOpenWorkspacesConflictsAreVisible() {
        dao.add(conflict(key = "in-7"))
        preferences.workspaceId = 8
        dao.add(conflict(key = "in-8").copy(workspaceId = 8))

        assertEquals(listOf("in-8"), dao.getAll().map { it.tagKey })
        assertEquals(1, dao.getCount())

        preferences.workspaceId = 7
        val inSeven = dao.getAll().single()
        assertEquals("in-7", inSeven.tagKey)
        // and can't be deleted from another workspace
        preferences.workspaceId = 8
        assertFalse(dao.delete(inSeven.id))
    }

    private companion object {
        const val PREFS = "PendingTagConflictsDaoTest"
    }
}
