package de.westnordost.streetcomplete.screens.main

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.screens.workspaces.WorkSpaceActivity
import de.westnordost.streetcomplete.testutils.MockOsmServer
import de.westnordost.streetcomplete.testutils.MockOsmServer.Failure
import de.westnordost.streetcomplete.testutils.MockOsmServer.MockNode
import io.ktor.http.HttpStatusCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * What the user sees when syncing with OSM goes wrong - and that their answer isn't lost where it
 * shouldn't be: connection/server/authorization errors on upload and download, answering while
 * offline and uploading once back online (with the toolbar's upload button), and answers that
 * can't be applied anymore because the element was deleted or reshaped meanwhile.
 * See [MapSyncTestBase] for the setup.
 */
@RunWith(AndroidJUnit4::class)
class MapSyncErrorsTest : MapSyncTestBase() {

    //region upload

    @Test
    fun answeringOffline_showsConnectionError_keepsTheAnswer_andUploadsItOnceBackOnline() {
        openWorkspaceAndSidewalkQuest()
        MockOsmServer.failure = { Failure.Offline }

        answerConcreteAndWidth60()

        awaitToast(R.string.upload_server_error)
        assertEquals(emptyList<MockOsmServer.Upload>(), MockOsmServer.snapshot().uploads)
        assertEquals(1, unsyncedEditsCount())

        // back online - the user taps the toolbar's upload button
        MockOsmServer.failure = null
        tapUploadButton()

        val way = awaitUploads(1).single().modifiedWays.single()
        assertEquals(SIDEWALK.tags + mapOf("surface" to "concrete", "width" to "60"), way.tags)
        waitUntil("edit synced") { unsyncedEditsCount() == 0 }
        awaitChangesetsClosed()
    }

    @Test
    fun serverErrorOnUpload_showsConnectionError_closesTheChangeset_andKeepsTheAnswer() {
        openWorkspaceAndSidewalkQuest()
        MockOsmServer.failure = { if (it.endsWith("/upload")) Failure.Status(HttpStatusCode.InternalServerError) else null }

        answerConcreteAndWidth60()

        awaitToast(R.string.upload_server_error)
        assertEquals(emptyList<MockOsmServer.Upload>(), MockOsmServer.snapshot().uploads)
        assertEquals(1, unsyncedEditsCount())
        // the changeset opened for the failed attempt isn't left open
        awaitChangesetsClosed()

        MockOsmServer.failure = null
        tapUploadButton()
        awaitUploads(1)
    }

    // same as a 401 whose token refresh fails: logged out and taken to the login screen with the
    // Session Expired alert (it used to only toast and leave the user on the map, logged out)
    @Test
    fun uploadNotAuthorized_logsOut_toTheLoginScreen_butKeepsTheAnswer() {
        openWorkspaceAndSidewalkQuest()
        MockOsmServer.failure = { if (it.endsWith("/upload")) Failure.Status(HttpStatusCode.Forbidden) else null }

        answerConcreteAndWidth60()

        assertSessionExpiredOnTheLoginScreen()
        assertEquals(emptyList<MockOsmServer.Upload>(), MockOsmServer.snapshot().uploads)
        assertEquals(1, unsyncedEditsCount())
    }

    @Test
    fun uploadRejectedForAnotherReason_offersToSendAnErrorReport() {
        openWorkspaceAndSidewalkQuest()
        MockOsmServer.failure = { if (it.endsWith("/upload")) Failure.Status(HttpStatusCode.BadRequest) else null }

        answerConcreteAndWidth60()

        awaitText(UPLOAD_ERROR_TITLE)
        assertEquals(1, unsyncedEditsCount())
    }

    //endregion

    //region download

    @Test
    fun downloadingOffline_showsConnectionError() {
        MockOsmServer.failure = { Failure.Offline }
        openWorkspace()

        awaitToast(R.string.download_server_error)
        assertTrue(preferences.workspaceLogin)
    }

    @Test
    fun serverErrorOnDownload_showsConnectionError() {
        MockOsmServer.failure = { if (it == "GET map") Failure.Status(HttpStatusCode.ServiceUnavailable) else null }
        openWorkspace()

        awaitToast(R.string.download_server_error)
        assertTrue(preferences.workspaceLogin)
    }

    @Test
    fun downloadNotAuthorized_logsOut_toTheLoginScreen() {
        MockOsmServer.failure = { if (it == "GET map") Failure.Status(HttpStatusCode.Forbidden) else null }
        openWorkspace()

        assertSessionExpiredOnTheLoginScreen()
    }

    //endregion

    //region answers that can't be applied anymore

    @Test
    fun elementDeletedMeanwhile_answerIsDiscarded_andTheUserIsTold() {
        openWorkspaceAndSidewalkQuest()
        MockOsmServer.deleteWay(SIDEWALK.id)

        answerConcreteAndWidth60()

        awaitText(DISCARDED_TITLE)
        assertTrue(isShown("Reason: Element deleted"))
        composeTestRule.onNodeWithText("OK").performClick()
        waitUntil("notice closed") { !isShown(DISCARDED_TITLE) }

        assertEquals(emptyList<MockOsmServer.Upload>(), MockOsmServer.snapshot().uploads)
        assertEquals(0, unsyncedEditsCount())
        awaitChangesetsClosed()
    }

    @Test
    fun elementReshapedMeanwhile_answerIsDiscarded_andTheUserIsTold() {
        openWorkspaceAndSidewalkQuest()
        // someone extended the sidewalk - the answer may not apply to the new part
        MockOsmServer.put(MockNode(3, 47.65530, -122.30280))
        MockOsmServer.changeWayNodes(SIDEWALK.id, listOf(1, 2, 3))

        answerConcreteAndWidth60()

        awaitText(DISCARDED_TITLE)
        assertTrue(isShown("Reason: Element geometry changed substantially"))
        composeTestRule.onNodeWithText("OK").performClick()
        waitUntil("notice closed") { !isShown(DISCARDED_TITLE) }
        assertEquals(emptyList<MockOsmServer.Upload>(), MockOsmServer.snapshot().uploads)
    }

    //endregion

    private fun assertSessionExpiredOnTheLoginScreen() {
        awaitActivity(WorkSpaceActivity::class.java)
        awaitText("Session Expired")
        assertFalse(preferences.workspaceLogin)
        composeTestRule.onNodeWithText("Close").performClick()
        awaitText("Email")
    }

    private companion object {
        const val DISCARDED_TITLE = "Answer could not be saved"
        const val UPLOAD_ERROR_TITLE = "Upload error"
    }
}
