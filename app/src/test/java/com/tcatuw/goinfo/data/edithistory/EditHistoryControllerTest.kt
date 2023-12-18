package com.tcatuw.goinfo.data.edithistory

import com.tcatuw.goinfo.data.osm.edits.ElementEditsController
import com.tcatuw.goinfo.data.osm.edits.ElementEditsSource
import com.tcatuw.goinfo.data.osm.mapdata.ElementType
import com.tcatuw.goinfo.data.osm.osmquests.OsmQuestController
import com.tcatuw.goinfo.data.osmnotes.edits.NoteEditsController
import com.tcatuw.goinfo.data.osmnotes.edits.NoteEditsSource
import com.tcatuw.goinfo.data.osmnotes.notequests.OsmNoteQuestController
import com.tcatuw.goinfo.data.quest.OsmQuestKey
import com.tcatuw.goinfo.data.quest.TestQuestTypeA
import com.tcatuw.goinfo.testutils.any
import com.tcatuw.goinfo.testutils.edit
import com.tcatuw.goinfo.testutils.eq
import com.tcatuw.goinfo.testutils.mock
import com.tcatuw.goinfo.testutils.noteEdit
import com.tcatuw.goinfo.testutils.noteQuestHidden
import com.tcatuw.goinfo.testutils.on
import com.tcatuw.goinfo.testutils.questHidden
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.verify
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class EditHistoryControllerTest {

    private lateinit var elementEditsController: ElementEditsController
    private lateinit var noteEditsController: NoteEditsController
    private lateinit var osmQuestController: OsmQuestController
    private lateinit var osmNoteQuestController: OsmNoteQuestController
    private lateinit var listener: EditHistorySource.Listener
    private lateinit var ctrl: EditHistoryController

    private lateinit var elementEditsListener: ElementEditsSource.Listener
    private lateinit var noteEditsListener: NoteEditsSource.Listener
    private lateinit var hideNoteQuestsListener: OsmNoteQuestController.HideOsmNoteQuestListener
    private lateinit var hideQuestsListener: OsmQuestController.HideOsmQuestListener

    @BeforeTest fun setUp() {
        elementEditsController = mock()
        noteEditsController = mock()
        osmQuestController = mock()
        osmNoteQuestController = mock()
        listener = mock()

        elementEditsListener = mock()
        noteEditsListener = mock()
        hideNoteQuestsListener = mock()
        hideQuestsListener = mock()

        on(elementEditsController.addListener(any())).then { invocation ->
            elementEditsListener = invocation.getArgument(0)
            Unit
        }
        on(noteEditsController.addListener(any())).then { invocation ->
            noteEditsListener = invocation.getArgument(0)
            Unit
        }
        on(osmNoteQuestController.addHideQuestsListener(any())).then { invocation ->
            hideNoteQuestsListener = invocation.getArgument(0)
            Unit
        }
        on(osmQuestController.addHideQuestsListener(any())).then { invocation ->
            hideQuestsListener = invocation.getArgument(0)
            Unit
        }

        ctrl = EditHistoryController(elementEditsController, noteEditsController, osmNoteQuestController, osmQuestController)
        ctrl.addListener(listener)
    }

    @Test fun getAll() {
        val edit1 = edit(timestamp = 10L)
        val edit2 = noteEdit(timestamp = 20L)
        val edit3 = edit(timestamp = 50L)
        val edit4 = noteEdit(timestamp = 80L)
        val edit5 = questHidden(timestamp = 100L)
        val edit6 = noteQuestHidden(timestamp = 120L)

        on(elementEditsController.getAll()).thenReturn(listOf(edit1, edit3))
        on(noteEditsController.getAll()).thenReturn(listOf(edit2, edit4))
        on(osmQuestController.getAllHiddenNewerThan(anyLong())).thenReturn(listOf(edit5))
        on(osmNoteQuestController.getAllHiddenNewerThan(anyLong())).thenReturn(listOf(edit6))

        assertEquals(
            listOf(edit6, edit5, edit4, edit3, edit2, edit1),
            ctrl.getAll()
        )
    }

    @Test fun `undo element edit`() {
        val e = edit()
        ctrl.undo(e)
        verify(elementEditsController).undo(e)
    }

    @Test fun `undo note edit`() {
        val e = noteEdit()
        ctrl.undo(e)
        verify(noteEditsController).undo(e)
    }

    @Test fun `undo hid quest`() {
        val e = questHidden(ElementType.NODE, 1L, TestQuestTypeA())
        ctrl.undo(e)
        verify(osmQuestController).unhide(OsmQuestKey(ElementType.NODE, 1L, "TestQuestTypeA"))
    }

    @Test fun `undo hid note quest`() {
        val e = noteQuestHidden()
        ctrl.undo(e)
        verify(osmNoteQuestController).unhide(e.note.id)
    }

    @Test fun `relays added element edit`() {
        val e = edit()
        elementEditsListener.onAddedEdit(e)
        verify(listener).onAdded(e)
    }

    @Test fun `relays removed element edit`() {
        val e = edit()
        elementEditsListener.onDeletedEdits(listOf(e))
        verify(listener).onDeleted(eq(listOf(e)))
    }

    @Test fun `relays synced element edit`() {
        val e = edit()
        elementEditsListener.onSyncedEdit(e)
        verify(listener).onSynced(e)
    }

    @Test fun `relays added note edit`() {
        val e = noteEdit()
        noteEditsListener.onAddedEdit(e)
        verify(listener).onAdded(e)
    }

    @Test fun `relays removed note edit`() {
        val e = noteEdit()
        noteEditsListener.onDeletedEdits(listOf(e))
        verify(listener).onDeleted(eq(listOf(e)))
    }

    @Test fun `relays synced note edit`() {
        val e = noteEdit()
        noteEditsListener.onSyncedEdit(e)
        verify(listener).onSynced(e)
    }

    @Test fun `relays hid quest`() {
        val e = questHidden()
        hideQuestsListener.onHid(e)
        verify(listener).onAdded(e)
    }

    @Test fun `relays unhid quest`() {
        val e = questHidden()
        hideQuestsListener.onUnhid(e)
        verify(listener).onDeleted(eq(listOf(e)))
    }

    @Test fun `relays unhid all quests`() {
        hideQuestsListener.onUnhidAll()
        verify(listener).onInvalidated()
    }

    @Test fun `relays hid note quest`() {
        val e = noteQuestHidden()
        hideNoteQuestsListener.onHid(e)
        verify(listener).onAdded(e)
    }

    @Test fun `relays unhid note quest`() {
        val e = noteQuestHidden()
        hideNoteQuestsListener.onUnhid(e)
        verify(listener).onDeleted(eq(listOf(e)))
    }

    @Test fun `relays unhid all note quests`() {
        hideNoteQuestsListener.onUnhidAll()
        verify(listener).onInvalidated()
    }
}
