package de.westnordost.streetcomplete.quests.sidewalk_long_form

import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.PhotoAttachment.None
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.PhotoAttachment.Pending
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.PhotoAttachment.PendingRemoval
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.PhotoAttachment.Uploaded
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.PhotoState
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.PhotoTransition
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.afterOrphanedPhotoDiscarded
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.afterPhotoCaptured
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.afterPhotoDeleted
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.afterPhotoUndoRemoval
import kotlin.test.Test
import kotlin.test.assertEquals

class PhotoStateTest {

    private val url = "https://kartaview.org/photo/1"
    private val uploaded = PhotoState(Uploaded(url))
    private val markedForRemoval = PhotoState(PendingRemoval(url), existingPhotoRemoved = true)

    //region capture

    @Test fun `first capture`() {
        assertEquals(
            PhotoTransition(PhotoState(Pending("a.jpg", 45f))),
            PhotoState().afterPhotoCaptured("a.jpg", 45f)
        )
    }

    @Test fun `capture over an existing photo remembers what it replaces`() {
        assertEquals(
            PhotoState(Pending("a.jpg", 0f, replaces = Uploaded(url))),
            uploaded.afterPhotoCaptured("a.jpg", 0f).state
        )
    }

    @Test fun `capture over a photo marked for removal remembers the removal`() {
        assertEquals(
            Pending("a.jpg", 0f, replaces = PendingRemoval(url)),
            markedForRemoval.afterPhotoCaptured("a.jpg", 0f).state.attachment
        )
    }

    @Test fun `retake deletes the previous capture and keeps the original it replaces`() {
        val first = uploaded.afterPhotoCaptured("a.jpg", 0f).state
        val retake = first.afterPhotoCaptured("b.jpg", 10f)
        assertEquals("a.jpg", retake.fileToDelete)
        assertEquals(Pending("b.jpg", 10f, replaces = Uploaded(url)), retake.state.attachment)
    }

    //endregion

    //region delete / undo

    @Test fun `deleting a fresh capture drops it`() {
        val deleted = PhotoState(Pending("a.jpg")).afterPhotoDeleted()
        assertEquals(PhotoTransition(PhotoState(None), fileToDelete = "a.jpg"), deleted)
    }

    @Test fun `deleting a capture reverts to the existing photo it replaced`() {
        val deleted = uploaded.afterPhotoCaptured("a.jpg", 0f).state.afterPhotoDeleted()
        assertEquals(PhotoTransition(uploaded, fileToDelete = "a.jpg"), deleted)
    }

    @Test fun `deleting a capture reverts to the removal it replaced`() {
        val deleted = markedForRemoval.afterPhotoCaptured("a.jpg", 0f).state.afterPhotoDeleted()
        assertEquals(PhotoTransition(markedForRemoval, fileToDelete = "a.jpg"), deleted)
    }

    @Test fun `deleting an existing photo marks it for removal`() {
        assertEquals(PhotoTransition(markedForRemoval), uploaded.afterPhotoDeleted())
    }

    @Test fun `deleting when marked for removal or empty ends at none`() {
        assertEquals(None, markedForRemoval.afterPhotoDeleted().state.attachment)
        assertEquals(PhotoTransition(PhotoState()), PhotoState().afterPhotoDeleted())
    }

    @Test fun `undo restores a photo marked for removal`() {
        assertEquals(uploaded, markedForRemoval.afterPhotoUndoRemoval())
    }

    @Test fun `undo does nothing in any other state`() {
        listOf(PhotoState(), uploaded, PhotoState(Pending("a.jpg"))).forEach {
            assertEquals(it, it.afterPhotoUndoRemoval())
        }
    }

    @Test fun `remove, retake, cancel retake, undo lands back on the original photo`() {
        val end = uploaded
            .afterPhotoDeleted().state
            .afterPhotoCaptured("a.jpg", 0f).state
            .afterPhotoCaptured("b.jpg", 0f).state
            .afterPhotoDeleted().state
            .afterPhotoUndoRemoval()
        assertEquals(uploaded, end)
    }

    //endregion

    //region orphan discard

    @Test fun `discarding an orphaned fresh capture deletes it without removing any tag`() {
        assertEquals(
            PhotoTransition(PhotoState(), fileToDelete = "a.jpg"),
            PhotoState(Pending("a.jpg")).afterOrphanedPhotoDiscarded()
        )
    }

    @Test fun `discarding an orphaned existing photo removes its tag`() {
        assertEquals(PhotoState(None, existingPhotoRemoved = true), uploaded.afterOrphanedPhotoDiscarded().state)
        assertEquals(PhotoState(None, existingPhotoRemoved = true), markedForRemoval.afterOrphanedPhotoDiscarded().state)
    }

    @Test fun `discarding an orphaned replacement deletes it and removes the replaced photo's tag`() {
        val replacement = uploaded.afterPhotoCaptured("a.jpg", 0f).state
        assertEquals(
            PhotoTransition(PhotoState(None, existingPhotoRemoved = true), fileToDelete = "a.jpg"),
            replacement.afterOrphanedPhotoDiscarded()
        )
    }

    //endregion
}
