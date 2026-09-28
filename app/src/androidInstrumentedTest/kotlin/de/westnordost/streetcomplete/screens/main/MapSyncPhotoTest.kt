package de.westnordost.streetcomplete.screens.main

import android.app.Activity
import android.app.Instrumentation
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.content.IntentCompat
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.Database
import de.westnordost.streetcomplete.data.osm.edits.create_feature.FeaturePhotosTable
import de.westnordost.streetcomplete.data.upload.UploadProgressSource
import de.westnordost.streetcomplete.quests.sidewalk_long_form.inRowOf
import de.westnordost.streetcomplete.quests.sidewalk_long_form.scrollIntoView
import de.westnordost.streetcomplete.testutils.MockKartaView
import de.westnordost.streetcomplete.testutils.MockOsmServer
import io.ktor.http.HttpStatusCode
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A long-form answer with a photo, when the photo can't be uploaded to KartaView: after three
 * failed upload runs the user is told and can remove the photo (the answer then uploads without
 * it) or keep trying. KartaView is [MockKartaView], hooked into the app's own client - no photo
 * ever reaches the real service. See [MapSyncTestBase] for the rest of the setup.
 */
@RunWith(AndroidJUnit4::class)
class MapSyncPhotoTest : MapSyncTestBase() {

    @Before
    fun stubCamera() {
        Intents.init()
    }

    @After
    fun releaseCamera() {
        Intents.release()
    }

    // the normal case: KartaView takes the photo, and the answer goes up carrying its public URL
    @Test
    fun photoUploads_andTheAnswerCarriesItsUrl() {
        MockKartaView.failure = null
        openWorkspaceAndSidewalkQuest()
        answerWithPhoto()

        val way = awaitUploads(1).single().modifiedWays.single()
        assertEquals(
            SIDEWALK.tags + mapOf("surface" to "concrete", "obstacle" to "other", "ext:kartaview_url" to MockKartaView.PHOTO_URL),
            way.tags
        )
        // uploaded the way the real API expects it, before the answer
        assertEquals(
            listOf("POST /1.0/sequence/", "POST /1.0/photo/", "POST /1.0/sequence/finished-uploading/", "GET /2.0/photo/mock-photo-1"),
            MockKartaView.requests.toList()
        )
        // the photo is done with: its record (and file) are gone, nothing is left queued
        waitUntil("answer synced") { unsyncedEditsCount() == 0 }
        assertEquals(0, koin.get<Database>().query(FeaturePhotosTable.NAME) { 1 }.size)
        assertFalse(isShown(STUCK_PHOTO_TITLE))
        awaitChangesetsClosed()
    }

    @Test
    fun photoKeepsFailingToUpload_userIsTold_andCanRemoveIt_thenTheAnswerUploads() {
        MockKartaView.failure = MockKartaView.Failure.Status(HttpStatusCode.ServiceUnavailable)
        openWorkspaceAndSidewalkQuest()
        answerWithPhoto()

        failThreeUploadRuns()

        awaitText(STUCK_PHOTO_TITLE)
        composeTestRule.onNodeWithText("Remove photo").performClick()

        val way = awaitUploads(1).single().modifiedWays.single()
        assertEquals(SIDEWALK.tags + mapOf("surface" to "concrete", "obstacle" to "other"), way.tags)
        assertFalse(way.tags.keys.any { it.startsWith("ext:kartaview") })
    }

    @Test
    fun keepTrying_closesTheNotice_andTheAnswerStaysQueued() {
        MockKartaView.failure = MockKartaView.Failure.Status(HttpStatusCode.ServiceUnavailable)
        openWorkspaceAndSidewalkQuest()
        answerWithPhoto()

        failThreeUploadRuns()

        awaitText(STUCK_PHOTO_TITLE)
        composeTestRule.onNodeWithText("Keep trying").performClick()
        waitUntil("notice closed") { !isShown(STUCK_PHOTO_TITLE) }

        assertEquals(emptyList<MockOsmServer.Upload>(), MockOsmServer.snapshot().uploads)
        assertEquals(1, unsyncedEditsCount())
    }

    // KartaView not answering at all (no connection, DNS, timeout) must be handled like it
    // answering with an error: the photo's answer is held, the others aren't, and after three
    // runs the user is told
    @Test
    fun kartaViewUnreachable_isHandledLikeAKartaViewError() {
        MockKartaView.failure = MockKartaView.Failure.Unreachable
        openWorkspaceAndSidewalkQuest()
        answerWithPhoto()

        failThreeUploadRuns()

        awaitText(STUCK_PHOTO_TITLE)
        assertFalse(isShown(UPLOAD_ERROR_TITLE))
    }

    //region helpers

    private fun answerWithPhoto() {
        tapTile(SURFACE_Q, "Concrete")
        tapTile(OBSTRUCTION_Q, "Other obstruction")
        capturePhoto()
        submit()
    }

    /** The upload after submitting, then two more with the toolbar's upload button. */
    private fun failThreeUploadRuns() {
        val uploads: UploadProgressSource = koin.get()
        for (run in 1..3) {
            if (run > 1) tapUploadButton()
            waitUntil("photo upload attempt $run") {
                MockKartaView.requests.size >= run && !uploads.isUploadInProgress
            }
        }
        assertTrue(MockOsmServer.snapshot().uploads.isEmpty())
    }

    /** The camera is stubbed to "take" a small real JPEG (the form reads its EXIF). */
    private fun capturePhoto() {
        intending(hasAction(MediaStore.ACTION_IMAGE_CAPTURE)).respondWithFunction { intent ->
            val uri = IntentCompat.getParcelableExtra(intent, MediaStore.EXTRA_OUTPUT, Uri::class.java)!!
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            context.contentResolver.openOutputStream(uri)!!.use {
                Bitmap.createBitmap(64, 48, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.JPEG, 90, it)
            }
            Instrumentation.ActivityResult(Activity.RESULT_OK, null)
        }
        onView(inRowOf(OBSTRUCTION_Q, R.id.choice_follow_up)).perform(scrollIntoView(), click())
        // the camera result comes back asynchronously - submitting before the photo card shows
        // would submit the answer without its photo
        waitUntil("photo attached") {
            try {
                onView(inRowOf(OBSTRUCTION_Q, R.id.photo_title)).perform(scrollIntoView()).check(matches(withText("Photo attached")))
                true
            } catch (e: Throwable) {
                false
            }
        }
    }

    //endregion

    private companion object {
        const val STUCK_PHOTO_TITLE = "Photo upload failed"
        const val UPLOAD_ERROR_TITLE = "Upload error"
    }
}
