package de.westnordost.streetcomplete.screens.main

import android.os.SystemClock
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.osm.edits.ElementEditsController
import de.westnordost.streetcomplete.data.osm.edits.create_feature.FeaturePhotosController
import de.westnordost.streetcomplete.data.osm.mapdata.LatLon
import de.westnordost.streetcomplete.data.upload.UploadProgressSource
import de.westnordost.streetcomplete.testutils.MockKartaView
import de.westnordost.streetcomplete.testutils.MockOsmServer
import de.westnordost.streetcomplete.testutils.MockOsmServer.MockNode
import io.ktor.http.HttpStatusCode
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * One long-form answer for several sidewalks at once, the way a user does it on the map:
 * long-press a sidewalk's pin, tap another one, confirm, answer (with a photo) - each sidewalk
 * gets its own edit, and its own copy of the photo (one shared file would be deleted by whichever
 * edit uploads first, losing the photo for the others). See [MapSyncTestBase] for the setup.
 */
@RunWith(AndroidJUnit4::class)
class MapSyncMultiSelectTest : MapSyncTestBase() {

    // a second sidewalk ~24 m north of the first
    override val otherElements = listOf(
        MockNode(3, 47.65552, -122.30380),
        MockNode(4, 47.65552, -122.30320),
        SIDEWALK.copy(id = SECOND_SIDEWALK_ID, nodeIds = listOf(3, 4)),
    )

    @Before
    fun stubCamera() {
        Intents.init()
    }

    @After
    fun releaseCamera() {
        Intents.release()
    }

    @Test
    fun answeringTwoSidewalksAtOnce_withAPhoto_uploadsBoth_eachWithItsOwnPhoto() {
        val activity = openWorkspace()
        waitUntil("map data downloaded") { MockOsmServer.snapshot().requests.any { it == "GET map" } }
        val first = awaitQuestFor(SIDEWALK.id)
        val second = awaitQuestFor(SECOND_SIDEWALK_ID)

        // select both on the map
        zoomTo(activity, LatLon((first.position.latitude + second.position.latitude) / 2, first.position.longitude))
        val (x1, y1) = pinOnScreen(activity, first)
        press(x1, y1, long = true)
        waitUntil("multi-select bar shown") { isShown(R.id.yesButton) }
        val (x2, y2) = pinOnScreen(activity, second)
        press(x2, y2, long = false)
        SystemClock.sleep(500)
        onView(withId(R.id.yesButton)).perform(click())
        waitUntil("long form opened") { isDisplayed(SURFACE_Q) }

        // KartaView down at first, so the photos stay around to be looked at
        MockKartaView.failure = MockKartaView.Failure.Status(HttpStatusCode.ServiceUnavailable)
        tapTile(SURFACE_Q, "Concrete")
        tapTile(OBSTRUCTION_Q, "Other obstruction")
        capturePhoto()
        submit()

        val uploads: UploadProgressSource = koin.get()
        waitUntil("photo upload attempted") { MockKartaView.requests.isNotEmpty() && !uploads.isUploadInProgress }
        val edits = koin.get<ElementEditsController>().getAllUnsynced()
        assertEquals("one edit per selected sidewalk", 2, edits.size)
        val photos = edits.map { koin.get<FeaturePhotosController>().get(it.id).single() }
        assertEquals("each edit owns its own photo file", 2, photos.map { it.path }.toSet().size)
        assertTrue(photos.all { File(it.path).exists() })

        // KartaView back - both go up, each carrying its photo's URL
        MockKartaView.failure = null
        tapUploadButton()
        waitUntil("both sidewalks uploaded") {
            MockOsmServer.snapshot().uploads.flatMap { it.modifiedWays }.map { it.id }.toSet() == setOf(SIDEWALK.id, SECOND_SIDEWALK_ID)
        }
        for (way in MockOsmServer.snapshot().uploads.flatMap { it.modifiedWays }) {
            assertEquals(
                "way ${way.id}",
                SIDEWALK.tags + mapOf("surface" to "concrete", "obstacle" to "other", "ext:kartaview_url" to MockKartaView.PHOTO_URL),
                way.tags
            )
        }
        waitUntil("nothing left queued") { unsyncedEditsCount() == 0 }
        awaitChangesetsClosed()
    }

    private fun isShown(viewId: Int): Boolean = try {
        onView(withId(viewId)).check(matches(isDisplayed()))
        true
    } catch (e: Throwable) {
        false
    }

    private companion object {
        const val SECOND_SIDEWALK_ID = 101L
    }
}
