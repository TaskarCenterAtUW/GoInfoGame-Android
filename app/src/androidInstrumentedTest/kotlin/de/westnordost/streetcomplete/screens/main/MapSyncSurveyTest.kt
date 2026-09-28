package de.westnordost.streetcomplete.screens.main

import androidx.test.ext.junit.runners.AndroidJUnit4
import de.westnordost.streetcomplete.data.osm.mapdata.BoundingBox
import de.westnordost.streetcomplete.data.osm.mapdata.LatLon
import de.westnordost.streetcomplete.testutils.MockOsmServer.MockNode
import de.westnordost.streetcomplete.testutils.MockOsmServer.MockWay
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Answering a quest for an element the user isn't near (further than the app's survey distance,
 * 80 m + GPS accuracy). Its own area ~1 km east of the other map tests, so no recent location of
 * theirs counts as having been there. See [MapSyncTestBase] for the setup.
 */
@RunWith(AndroidJUnit4::class)
class MapSyncSurveyTest : MapSyncTestBase() {

    // 120 m north of the sidewalk
    override val userPosition = LatLon(47.65638, -122.29020)
    override val sidewalkNodes = listOf(
        MockNode(11, 47.65530, -122.29050),
        MockNode(12, 47.65530, -122.28990),
    )
    override val sidewalk = SIDEWALK.copy(id = 200, nodeIds = listOf(11, 12))
    override val area = BoundingBox(47.654, -122.292, 47.658, -122.288)

    // CURRENT BEHAVIOR, flagged to the user 2026-09-28: the "Are you sure you checked this on-site?"
    // dialog (view/ConfirmIsSurvey.kt) never shows - its dontShowAgain defaults to true (upstream
    // StreetComplete: false) - so an answer given from afar is accepted and uploaded as a survey.
    @Test
    fun answeringFromAfar_isAcceptedWithoutAskingIfTheUserWasThere() {
        openWorkspaceAndSidewalkQuest()

        answerConcreteAndWidth60()

        val way = awaitUploads(1).single().modifiedWays.single()
        assertEquals(sidewalk.tags + mapOf("surface" to "concrete", "width" to "60"), way.tags)
    }
}
