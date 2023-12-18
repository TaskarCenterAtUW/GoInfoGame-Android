package com.tcatuw.goinfo.data.osmnotes.edits

import com.tcatuw.goinfo.data.osm.mapdata.ElementIdUpdate
import com.tcatuw.goinfo.data.osm.mapdata.ElementType
import com.tcatuw.goinfo.testutils.any
import com.tcatuw.goinfo.testutils.eq
import com.tcatuw.goinfo.testutils.mock
import com.tcatuw.goinfo.testutils.note
import com.tcatuw.goinfo.testutils.noteEdit
import com.tcatuw.goinfo.testutils.on
import com.tcatuw.goinfo.testutils.p
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import kotlin.test.BeforeTest
import kotlin.test.Test

class NoteEditsControllerTest {

    private lateinit var ctrl: NoteEditsController
    private lateinit var db: NoteEditsDao
    private lateinit var listener: NoteEditsSource.Listener

    @BeforeTest fun setUp() {
        db = mock()
        on(db.delete(anyLong())).thenReturn(true)
        on(db.markSynced(anyLong())).thenReturn(true)

        listener = mock()
        ctrl = NoteEditsController(db)
        ctrl.addListener(listener)
    }

    @Test fun add() {
        ctrl.add(1L, NoteEditAction.COMMENT, p(1.0, 1.0))

        verify(db).add(any())
        verify(listener).onAddedEdit(any())
    }

    @Test fun syncFailed() {
        val edit = noteEdit(noteId = 1)
        ctrl.markSyncFailed(edit)

        verify(db).delete(1)
        verify(listener).onDeletedEdits(eq(listOf(edit)))
    }

    @Test fun synced() {
        val edit = noteEdit(id = 3, noteId = 1)
        val note = note(1)

        ctrl.markSynced(edit, note)

        verify(db).markSynced(3)
        verify(db, never()).updateNoteId(anyLong(), anyLong())
        verify(listener).onSyncedEdit(edit)
    }

    @Test fun `synced with new id`() {
        val edit = noteEdit(id = 3, noteId = -100)
        val note = note(123)

        ctrl.markSynced(edit, note)

        verify(db).markSynced(3)
        verify(db).updateNoteId(-100L, 123L)
        verify(listener).onSyncedEdit(edit)
    }

    @Test fun `update element ids`() {
        ctrl.updateElementIds(listOf(
            ElementIdUpdate(ElementType.NODE, -9, 1234),
            ElementIdUpdate(ElementType.WAY, 4, 999),
            ElementIdUpdate(ElementType.RELATION, 8, 234),
        ))

        verify(db).replaceTextInUnsynced("osm.org/node/-9 ", "osm.org/node/1234 ")
        verify(db).replaceTextInUnsynced("osm.org/way/4 ", "osm.org/way/999 ")
        verify(db).replaceTextInUnsynced("osm.org/relation/8 ", "osm.org/relation/234 ")
    }
}
