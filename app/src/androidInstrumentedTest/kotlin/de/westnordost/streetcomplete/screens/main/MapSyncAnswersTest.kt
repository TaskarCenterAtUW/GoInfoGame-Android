package de.westnordost.streetcomplete.screens.main

import android.os.SystemClock
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.quest.VisibleQuestsSource
import de.westnordost.streetcomplete.quests.sidewalk_long_form.inRowOf
import de.westnordost.streetcomplete.quests.sidewalk_long_form.scrollIntoView
import de.westnordost.streetcomplete.testutils.MockOsmServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/**
 * More long-form answers given on the map, uploaded to (mock) OSM: a text answer, removing the
 * photo from the last visit, and the recheck of an already fully answered sidewalk once its
 * last edit is older than the workspace's recency period (90 days here) - and not before.
 * See [MapSyncTestBase] for the setup.
 */
@RunWith(AndroidJUnit4::class)
class MapSyncAnswersTest : MapSyncTestBase() {

    @Test
    fun textAnswer_isUploaded() {
        openWorkspaceAndSidewalkQuest()

        typeInto(NOTES_Q, "Cracked slab near the curb")
        submit()

        val way = awaitUploads(1).single().modifiedWays.single()
        assertEquals(SIDEWALK.tags + ("note" to "Cracked slab near the curb"), way.tags)
    }

    @Test
    fun removingThePhotoFromTheLastVisit_removesItsTagOnOsm() {
        val tags = SIDEWALK.tags + mapOf("obstacle" to "other", "ext:kartaview_url" to OLD_PHOTO_URL)
        MockOsmServer.put(SIDEWALK.copy(tags = tags))
        openWorkspaceAndSidewalkQuest()

        expandSheet()
        onView(inRowOf(OBSTRUCTION_Q, R.id.photo_title)).perform(scrollIntoView()).check(matches(withText("Photo from last visit")))
        onView(inRowOf(OBSTRUCTION_Q, R.id.photo_delete)).perform(scrollIntoView(), click())
        onView(inRowOf(OBSTRUCTION_Q, R.id.photo_title)).check(matches(withText("Photo will be removed")))
        submit()

        val way = awaitUploads(1).single().modifiedWays.single()
        assertEquals(tags - "ext:kartaview_url", way.tags)
    }

    // last edited long ago (the mock's default: 2025-01-01) - due for a recheck
    @Test
    fun fullyAnsweredSidewalk_dueForRecheck_resubmitsEveryAnswerAsIs() {
        MockOsmServer.put(SIDEWALK.copy(tags = FULLY_ANSWERED))
        openWorkspaceAndSidewalkQuest()

        submit()

        val way = awaitUploads(1).single().modifiedWays.single()
        assertEquals(FULLY_ANSWERED, way.tags)
        assertEquals(1, way.version)
    }

    @Test
    fun fullyAnsweredSidewalk_editedRecently_isNotAskedAgain() {
        MockOsmServer.put(SIDEWALK.copy(tags = FULLY_ANSWERED, timestamp = Instant.now().toString()))
        openWorkspace()
        waitUntil("map data downloaded") { MockOsmServer.snapshot().requests.any { it == "GET map" } }

        SystemClock.sleep(5_000) // quests are created right after the download
        assertNull(questFor(koin.get<VisibleQuestsSource>(), SIDEWALK.id))
        assertFalse(MockOsmServer.snapshot().requests.any { it.startsWith("PUT changeset") })
    }

    private companion object {
        const val OLD_PHOTO_URL = "https://storage.kartaview.org/files/photo/lth/from-last-visit.jpg"
        val FULLY_ANSWERED = SIDEWALK.tags + mapOf(
            "surface" to "asphalt", "width" to "60", "obstacle" to "bollard", "note" to "all good",
        )
    }
}
