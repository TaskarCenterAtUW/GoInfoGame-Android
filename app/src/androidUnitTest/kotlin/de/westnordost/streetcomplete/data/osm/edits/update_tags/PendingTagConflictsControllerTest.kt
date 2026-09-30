package de.westnordost.streetcomplete.data.osm.edits.update_tags

import de.westnordost.streetcomplete.data.osm.edits.ElementEdit
import de.westnordost.streetcomplete.data.osm.edits.ElementEditsController
import de.westnordost.streetcomplete.data.osm.edits.ElementEditsSource
import de.westnordost.streetcomplete.data.osm.mapdata.ElementType
import de.westnordost.streetcomplete.data.quest.TestQuestTypeA
import de.westnordost.streetcomplete.testutils.any
import de.westnordost.streetcomplete.testutils.argumentCaptor
import de.westnordost.streetcomplete.testutils.capture
import de.westnordost.streetcomplete.testutils.edit
import de.westnordost.streetcomplete.testutils.mock
import de.westnordost.streetcomplete.testutils.node
import de.westnordost.streetcomplete.testutils.on
import de.westnordost.streetcomplete.testutils.p
import kotlinx.coroutines.runBlocking
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * RESOLVE mode: an edit whose tags collided with a concurrent remote edit is held back
 * (blocked) with one [PendingTagConflict] per colliding key; the user decides per tag, each
 * decision is folded into the blocked edit, and the edit is unblocked once all are decided.
 */
class PendingTagConflictsControllerTest {

    private lateinit var dao: PendingTagConflictsDao
    private lateinit var editsController: ElementEditsController
    private lateinit var editsListener: ElementEditsSource.Listener
    private lateinit var controller: PendingTagConflictsController
    private lateinit var listener: PendingTagConflictsController.Listener

    /** what the (mocked) dao holds */
    private val stored = mutableListOf<PendingTagConflict>()
    private var nextId = 1L

    /** what the (mocked) edits controller holds */
    private val edits = mutableMapOf<Long, ElementEdit>()

    @BeforeTest fun setUp() {
        dao = mock()
        on(dao.add(any())).thenAnswer { invocation ->
            val conflict = invocation.getArgument<PendingTagConflict>(0).copy(id = nextId++)
            stored.add(conflict)
            conflict
        }
        on(dao.getAll()).thenAnswer { stored.toList() }
        on(dao.delete(anyLong())).thenAnswer { invocation ->
            val id = invocation.getArgument<Long>(0)
            stored.removeIf { it.id == id }
        }

        editsController = mock()
        on(editsController.get(anyLong())).thenAnswer { edits[it.getArgument<Long>(0)] }
        doAnswer { edits[it.getArgument<ElementEdit>(0).id] = it.getArgument(0); null }
            .`when`(editsController).updateAction(any())
        doAnswer {
            val edit = it.getArgument<ElementEdit>(0)
            edits[edit.id] = edit.copy(isBlockedOnConflict = false)
            null
        }.`when`(editsController).markUnblocked(any())

        controller = PendingTagConflictsController(dao, editsController)
        val editsListenerCaptor = argumentCaptor<ElementEditsSource.Listener>()
        verify(editsController).addListener(capture(editsListenerCaptor))
        editsListener = editsListenerCaptor.value

        listener = mock()
        controller.addListener(listener)
    }

    /** A blocked edit (id [editId]) that changed surface asphalt -> concrete and width 50 -> 60,
     *  while someone else set surface=gravel and width=55 on the server. */
    private fun givenBlockedEdit(editId: Long = 1L): ElementEdit {
        val action = UpdateElementTagsAction(
            node(id = 10, tags = mapOf("surface" to "asphalt", "width" to "50")),
            StringMapChanges(listOf(
                StringMapEntryModify("surface", "asphalt", "concrete"),
                StringMapEntryModify("width", "50", "60"),
                StringMapEntryAdd("lit", "yes"),
            ))
        )
        val edit = edit(id = editId, action = action).copy(isBlockedOnConflict = true)
        edits[editId] = edit
        return edit
    }

    private fun conflict(editId: Long, key: String, mine: String?, theirs: String?) = PendingTagConflict(
        id = 0, editId = editId, elementType = ElementType.NODE, elementId = 10,
        tagKey = key, mineValue = mine, theirsValueAtDetection = theirs,
        editType = TestQuestTypeA(), source = "survey", position = p(),
        createdTimestamp = 0, workspaceId = 7,
    )

    private fun changesOf(editId: Long): Set<StringMapEntryChange> =
        (edits.getValue(editId).action as UpdateElementTagsAction).changes.changes.toSet()

    //region adding and querying

    @Test fun `added conflict is stored and listeners are told`() {
        controller.add(conflict(1, "surface", "concrete", "gravel"))
        val added = stored.single()
        assertEquals(1L, added.id)
        verify(listener).onAdded(added)
    }

    @Test fun `count is per blocked edit, not per conflicting tag`() {
        controller.add(conflict(1, "surface", "concrete", "gravel"))
        controller.add(conflict(1, "width", "60", "55"))
        controller.add(conflict(2, "surface", "concrete", "gravel"))
        assertEquals(2, controller.getCount())
    }

    @Test fun `oldest group holds every conflicting tag of the oldest blocked edit`() {
        controller.add(conflict(1, "surface", "concrete", "gravel"))
        controller.add(conflict(2, "surface", "concrete", "gravel"))
        controller.add(conflict(1, "width", "60", "55"))

        assertEquals(listOf("surface", "width"), controller.getOldestGroup().map { it.tagKey })
        assertTrue(controller.getOldestGroup().all { it.editId == 1L })
        assertEquals("surface", controller.getOldest()?.tagKey)
    }

    @Test fun `nothing pending`() {
        assertEquals(0, controller.getCount())
        assertEquals(emptyList(), controller.getOldestGroup())
    }

    //endregion

    //region resolving

    @Test fun `keep mine re-bases the change on the server's value`() = runBlocking {
        givenBlockedEdit()
        controller.add(conflict(1, "surface", "concrete", "gravel"))

        controller.resolveKeepMine(stored.single())

        // uploads as gravel -> concrete, so the re-upload's conflict check doesn't flag it again
        assertTrue(StringMapEntryModify("surface", "gravel", "concrete") in changesOf(1))
        assertTrue(StringMapEntryModify("width", "50", "60") in changesOf(1))
        assertTrue(StringMapEntryAdd("lit", "yes") in changesOf(1))
        assertTrue(stored.isEmpty())
    }

    @Test fun `keep theirs drops only that tag from the edit`() = runBlocking {
        givenBlockedEdit()
        controller.add(conflict(1, "surface", "concrete", "gravel"))

        controller.resolveKeepTheirs(stored.single())

        assertEquals(
            setOf(StringMapEntryModify("width", "50", "60"), StringMapEntryAdd("lit", "yes")),
            changesOf(1)
        )
    }

    @Test fun `the edit is unblocked once its last conflict is resolved`() = runBlocking {
        givenBlockedEdit()
        controller.add(conflict(1, "surface", "concrete", "gravel"))
        controller.add(conflict(1, "width", "60", "55"))

        controller.resolveKeepMine(stored.first { it.tagKey == "surface" })
        verify(editsController, never()).markUnblocked(any())

        controller.resolveKeepTheirs(stored.single { it.tagKey == "width" })
        verify(editsController).markUnblocked(any())
        assertEquals(false, edits.getValue(1).isBlockedOnConflict)
        assertEquals(
            setOf(StringMapEntryModify("surface", "gravel", "concrete"), StringMapEntryAdd("lit", "yes")),
            changesOf(1)
        )
    }

    @Test fun `resolving one edit's conflicts doesn't unblock another edit`() = runBlocking {
        givenBlockedEdit(1)
        givenBlockedEdit(2)
        controller.add(conflict(1, "surface", "concrete", "gravel"))
        controller.add(conflict(2, "surface", "concrete", "gravel"))

        controller.resolveKeepMine(stored.first { it.editId == 1L })

        assertEquals(false, edits.getValue(1).isBlockedOnConflict)
        assertEquals(true, edits.getValue(2).isBlockedOnConflict)
        assertEquals(1, controller.getCount())
    }

    @Test fun `resolving tells the listeners`() = runBlocking {
        givenBlockedEdit()
        controller.add(conflict(1, "surface", "concrete", "gravel"))
        val conflict = stored.single()

        controller.resolveKeepTheirs(conflict)
        verify(listener).onRemoved(conflict)
    }

    @Test fun `resolving a conflict whose edit is gone just removes the conflict`() = runBlocking {
        controller.add(conflict(99, "surface", "concrete", "gravel"))
        controller.resolveKeepMine(stored.single())
        assertTrue(stored.isEmpty())
        verify(editsController, never()).updateAction(any())
    }

    @Test fun `undoing a blocked edit removes its conflicts`() {
        controller.add(conflict(1, "surface", "concrete", "gravel"))
        controller.add(conflict(1, "width", "60", "55"))
        controller.add(conflict(2, "surface", "concrete", "gravel"))

        editsListener.onDeletedEdits(listOf(givenBlockedEdit(1)))

        assertEquals(listOf(2L), stored.map { it.editId })
    }

    //endregion

    //region re-basing a change (shared with OVERRIDE mode's auto-resolve in ElementEditUploader)

    @Test fun `rebuiltAgainst keeps the intended end value`() {
        val modify = StringMapEntryModify("surface", "asphalt", "concrete")
        assertEquals(StringMapEntryModify("surface", "gravel", "concrete"), modify.rebuiltAgainst("gravel"))
        // someone deleted the tag meanwhile -> add it back
        assertEquals(StringMapEntryAdd("surface", "concrete"), modify.rebuiltAgainst(null))

        val delete = StringMapEntryDelete("surface", "asphalt")
        assertEquals(StringMapEntryDelete("surface", "gravel"), delete.rebuiltAgainst("gravel"))
        assertEquals(delete, delete.rebuiltAgainst(null))

        val add = StringMapEntryAdd("surface", "concrete")
        assertEquals(StringMapEntryModify("surface", "gravel", "concrete"), add.rebuiltAgainst("gravel"))
    }

    @Test fun `mineValue is the value the user answered, null for a deletion`() {
        assertEquals("concrete", StringMapEntryModify("surface", "asphalt", "concrete").mineValue())
        assertEquals("concrete", StringMapEntryAdd("surface", "concrete").mineValue())
        assertEquals(null, StringMapEntryDelete("surface", "asphalt").mineValue())
    }

    //endregion
}
