package de.westnordost.streetcomplete.screens.main

import android.os.SystemClock
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.westnordost.streetcomplete.testutils.MockOsmServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * RESOLVE mode (the conflict dialog) beyond a single changed tag: several conflicting tags at
 * once, a conflict with the user's removal of a tag, a third edit landing while the dialog is
 * open, and a held conflict surviving the app being closed or the dialog being postponed.
 * See [MapSyncTestBase] for the setup.
 */
@RunWith(AndroidJUnit4::class)
class MapSyncConflictEdgeCasesTest : MapSyncTestBase() {

    @Before
    fun resolveMode() {
        overrideConflicts = "false"
    }

    @Test
    fun severalConflictingTags_areDecidedPerTag_inOneDialog() {
        openWorkspaceAndSidewalkQuest()
        MockOsmServer.editWayConcurrently(SIDEWALK.id, mapOf("surface" to "gravel", "width" to "55"))

        answerConcreteAndWidth60()

        awaitConflictDialog()
        assertTrue(isShown(SURFACE_Q))
        assertTrue(isShown(WIDTH_Q))
        assertTrue(isShown("Your answer: concrete"))
        assertTrue(isShown("Existing value: gravel"))
        assertTrue(isShown("Your answer: 60"))
        // keep "concrete" (preselected), but take the other edit's width
        composeTestRule.onNode(hasText("Existing value: 55")).performClick()
        composeTestRule.onNodeWithText("Confirm").performClick()

        val way = awaitUploads(1).single().modifiedWays.single()
        assertEquals(SIDEWALK.tags + mapOf("surface" to "concrete", "width" to "55"), way.tags)
        awaitChangesetsClosed()
    }

    @Test
    fun removingATagThatSomeoneElseChanged_asksToo_andKeepingTheRemovalRemovesIt() {
        openWorkspaceAndSidewalkQuest()
        MockOsmServer.editWayConcurrently(SIDEWALK.id, mapOf("surface" to "gravel"))

        // the surface was pre-filled from the sidewalk's tags - deselecting it removes the tag
        tapTile(SURFACE_Q, "Asphalt")
        submit()

        awaitConflictDialog()
        assertTrue(isShown("Your answer: (removed)"))
        assertTrue(isShown("Existing value: gravel"))
        composeTestRule.onNodeWithText("Confirm").performClick()

        val way = awaitUploads(1).single().modifiedWays.single()
        assertEquals(SIDEWALK.tags - "surface", way.tags)
    }

    @Test
    fun anotherEditWhileTheDialogIsOpen_asksAgain_withTheNewValue() {
        openWorkspaceAndSidewalkQuest()
        MockOsmServer.editWayConcurrently(SIDEWALK.id, mapOf("surface" to "gravel"))
        answerConcreteAndWidth60()
        awaitConflictDialog()

        // a third value lands before the user confirms
        MockOsmServer.editWayConcurrently(SIDEWALK.id, mapOf("surface" to "paving_stones"))
        composeTestRule.onNodeWithText("Confirm").performClick()

        waitUntil("asked again about the new value") { isShown("Existing value: paving_stones") }
        assertEquals(emptyList<MockOsmServer.Upload>(), MockOsmServer.snapshot().uploads)
        composeTestRule.onNodeWithText("Confirm").performClick()

        val way = awaitUploads(1).single().modifiedWays.single()
        assertEquals(3, way.version)
        assertEquals(SIDEWALK.tags + mapOf("surface" to "concrete", "width" to "60"), way.tags)
    }

    @Test
    fun postponedDialog_comesBack_whenTheUserTapsUpload() {
        openWorkspaceAndSidewalkQuest()
        MockOsmServer.editWayConcurrently(SIDEWALK.id, mapOf("surface" to "gravel"))
        answerConcreteAndWidth60()
        awaitConflictDialog()

        composeTestRule.onNodeWithText("Cancel").performClick()
        waitUntil("dialog postponed") { !isShown(CONFLICT_DIALOG_TITLE) }

        tapUploadButton()
        awaitConflictDialog()
        composeTestRule.onNodeWithText("Confirm").performClick()
        awaitUploads(1)
    }

    @Test
    fun heldConflict_isAskedAgain_afterTheAppWasClosed() {
        openWorkspaceAndSidewalkQuest()
        MockOsmServer.editWayConcurrently(SIDEWALK.id, mapOf("surface" to "gravel"))
        answerConcreteAndWidth60()
        awaitConflictDialog()

        // closed without deciding
        scenario.close()
        finishAllActivities()
        SystemClock.sleep(1_000)

        openWorkspace()
        awaitConflictDialog()
        assertTrue(isShown("Existing value: gravel"))
        composeTestRule.onNodeWithText("Confirm").performClick()

        val way = awaitUploads(1).single().modifiedWays.single()
        assertEquals(SIDEWALK.tags + mapOf("surface" to "concrete", "width" to "60"), way.tags)
    }
}
