package de.westnordost.streetcomplete.quests.sidewalk_long_form

import android.content.res.Resources
import de.westnordost.streetcomplete.data.osm.edits.create_feature.FeaturePhoto
import de.westnordost.streetcomplete.data.osm.geometry.ElementPointGeometry
import de.westnordost.streetcomplete.data.osm.mapdata.LatLon
import de.westnordost.streetcomplete.osm.Tags
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.Elements
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.KARTAVIEW_URL_TAG
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.LongFormAdapter
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.LongFormSubmission
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.PhotoAttachment
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.PhotoState
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.UserInput
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.computeLongFormSubmission
import de.westnordost.streetcomplete.testutils.mock
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** What ALongForm.onClickOk would submit, driven through the adapter the same way the UI drives it. */
class LongFormSubmissionTest {

    private val photoUrl = "https://kartaview.org/photo/1"
    private val pendingPath = "/data/photo.jpg"

    /** Partially answered, with the photo follow-up ("other" obstruction) selected. */
    private val obstructionWithPhotoTags =
        mapOf("ext:obstruction" to "yes", "ext:obstruction:type" to "bollard;other")

    @BeforeTest fun setUp() {
        startKoin { modules(module { single<Resources> { mock() } }) }
    }

    @AfterTest fun tearDown() {
        stopKoin()
    }

    private fun LongFormAdapter<*>.submit(
        photo: PhotoState = PhotoState(),
        multiSelect: Boolean = false,
    ) = computeLongFormSubmission(givenItems, photo, recheckEnabled = !multiSelect)

    private fun LongFormSubmission.submittedIds() = items.map { it.questId }

    private fun LongFormSubmission.answerOf(questId: Int) = items.single { it.questId == questId }.userInput

    /** The tags the element ends up with once the submission is applied - the full round trip. */
    private fun LongFormSubmission.applyTo(tags: Map<String, String>): Map<String, String> {
        val builder = Tags(tags)
        AddGenericLong(Elements(questQuery = "ways", quests = sidewalkQuests()), 90)
            .applyAnswerTo(items, builder, ElementPointGeometry(LatLon(0.0, 0.0)), 0L)
        removeTagKeys.forEach { builder.remove(it) }
        return builder.toMap()
    }

    //region text entry regression

    @Test fun `text typed into a pre-filled form is submitted`() {
        val tags = mapOf("ext:surface" to "other", "ext:surface:description" to "cobbles")
        val adapter = openForm(tags)
        adapter.typeText(SURFACE_DESCRIPTION, "cobbles and tar")

        val submission = adapter.submit()
        assertEquals(listOf(SURFACE_DESCRIPTION), submission.submittedIds())
        assertEquals("cobbles and tar", submission.applyTo(tags)["ext:surface:description"])
    }

    @Test fun `text typed into a fully answered form is submitted on its own, not as a recheck`() {
        val adapter = openForm(FULLY_ANSWERED_TAGS)
        adapter.typeText(SURFACE_DESCRIPTION, "cobbles and tar")
        assertEquals(listOf(SURFACE_DESCRIPTION), adapter.submit().submittedIds())
    }

    @Test fun `text typed as the only answer on a new element is submitted`() {
        val adapter = openForm()
        adapter.tapChoice(SURFACE, "other")
        adapter.typeText(SURFACE_DESCRIPTION, "cobbles")

        val submission = adapter.submit()
        assertEquals(listOf(SURFACE, SURFACE_DESCRIPTION), submission.submittedIds())
        assertEquals(
            mapOf("ext:surface" to "other", "ext:surface:description" to "cobbles"),
            submission.applyTo(emptyMap())
        )
    }

    @Test fun `text typed after a dependency reveal shifted rows is submitted for the right question`() {
        val adapter = openForm()
        adapter.tapChoice(SURFACE, "other")
        adapter.typeNumber(WIDTH, "60")
        adapter.typeText(SURFACE_DESCRIPTION, "cobbles")

        val tags = adapter.submit().applyTo(emptyMap())
        assertEquals("60", tags["width"])
        assertEquals("cobbles", tags["ext:surface:description"])
    }

    @Test fun `retyping the same text as before is not a change`() {
        val tags = mapOf("ext:surface" to "other", "ext:surface:description" to "cobbles")
        val adapter = openForm(tags)
        adapter.typeText(SURFACE_DESCRIPTION, "cobble")
        adapter.typeText(SURFACE_DESCRIPTION, "cobbles")
        assertTrue(adapter.submit().isEmpty)
    }

    @Test fun `clearing a pre-filled text removes the tag`() {
        val tags = mapOf("ext:surface" to "other", "ext:surface:description" to "cobbles")
        val adapter = openForm(tags)
        adapter.typeText(SURFACE_DESCRIPTION, "")

        val submission = adapter.submit()
        assertEquals(listOf(SURFACE_DESCRIPTION), submission.submittedIds())
        assertFalse("ext:surface:description" in submission.applyTo(tags))
    }

    //endregion

    //region answering

    @Test fun `nothing answered on a new element is nothing to submit`() {
        assertTrue(openForm().submit().isEmpty)
    }

    @Test fun `only changed questions are submitted`() {
        val adapter = openForm(mapOf("ext:surface" to "asphalt", "width" to "60"))
        adapter.typeNumber(WIDTH, "72")
        adapter.tapChoice(OBSTRUCTION, "no")

        val submission = adapter.submit()
        assertEquals(listOf(WIDTH, OBSTRUCTION), submission.submittedIds())
        assertEquals(UserInput.Single("72"), submission.answerOf(WIDTH))
    }

    @Test fun `changing an answer and changing it back is not a change`() {
        val adapter = openForm(mapOf("ext:surface" to "asphalt"))
        adapter.tapChoice(SURFACE, "concrete")
        adapter.tapChoice(SURFACE, "asphalt")
        assertTrue(adapter.submit().isEmpty)
    }

    @Test fun `deselecting a pre-filled choice removes its tag`() {
        val tags = mapOf("ext:surface" to "asphalt")
        val adapter = openForm(tags)
        adapter.tapChoice(SURFACE, "asphalt")

        val submission = adapter.submit()
        assertEquals(listOf(SURFACE), submission.submittedIds())
        assertEquals(emptyMap(), submission.applyTo(tags))
    }

    @Test fun `reordering the same multiple choice answers is not a change`() {
        val adapter = openForm(mapOf("ext:obstruction" to "yes", "ext:obstruction:type" to "bollard;pole"))
        adapter.tapChoice(OBSTRUCTION_TYPE, "bollard")
        adapter.tapChoice(OBSTRUCTION_TYPE, "bollard") // now pole;bollard
        assertTrue(adapter.submit().isEmpty)
    }

    @Test fun `adding to a pre-filled multiple choice submits the full set`() {
        val tags = mapOf("ext:obstruction" to "yes", "ext:obstruction:type" to "bollard")
        val adapter = openForm(tags)
        adapter.tapChoice(OBSTRUCTION_TYPE, "pole")
        assertEquals("bollard;pole", adapter.submit().applyTo(tags)["ext:obstruction:type"])
    }

    //endregion

    //region dependent questions

    @Test fun `a pre-filled dependent question that becomes hidden is removed`() {
        val tags = mapOf("ext:surface" to "other", "ext:surface:description" to "cobbles")
        val adapter = openForm(tags)
        adapter.tapChoice(SURFACE, "asphalt")

        val submission = adapter.submit()
        assertEquals(listOf(SURFACE, SURFACE_DESCRIPTION), submission.submittedIds())
        assertEquals(mapOf("ext:surface" to "asphalt"), submission.applyTo(tags))
        // only the submitted copy is cleared - the live answer survives in case the user toggles back
        assertEquals(UserInput.Single("cobbles"), adapter.live(SURFACE_DESCRIPTION).userInput)
    }

    @Test fun `hiding and re-showing a dependent question before submitting keeps it`() {
        val adapter = openForm(mapOf("ext:surface" to "other", "ext:surface:description" to "cobbles"))
        adapter.tapChoice(SURFACE, "asphalt")
        adapter.tapChoice(SURFACE, "other")
        assertTrue(adapter.submit().isEmpty)
    }

    @Test fun `a choice made before its question was hidden and re-shown is not submitted`() {
        val adapter = openForm()
        adapter.tapChoice(OBSTRUCTION, "yes")
        adapter.tapChoice(OBSTRUCTION_TYPE, "bollard")
        adapter.tapChoice(OBSTRUCTION, "no")
        adapter.tapChoice(OBSTRUCTION, "yes")

        val submission = adapter.submit()
        assertEquals(listOf(OBSTRUCTION), submission.submittedIds())
        assertEquals(mapOf("ext:obstruction" to "yes"), submission.applyTo(emptyMap()))
    }

    @Test fun `a new choice on a re-shown question replaces the old one instead of merging`() {
        val adapter = openForm()
        adapter.tapChoice(OBSTRUCTION, "yes")
        adapter.tapChoice(OBSTRUCTION_TYPE, "bollard")
        adapter.tapChoice(OBSTRUCTION, "no")
        adapter.tapChoice(OBSTRUCTION, "yes")
        adapter.tapChoice(OBSTRUCTION_TYPE, "pole")
        assertEquals("pole", adapter.submit().applyTo(emptyMap())["ext:obstruction:type"])
    }

    @Test fun `a pre-filled choice whose question was hidden and re-shown is removed unless re-selected`() {
        val tags = mapOf("ext:obstruction" to "yes", "ext:obstruction:type" to "bollard")
        val adapter = openForm(tags)
        adapter.tapChoice(OBSTRUCTION, "no")
        adapter.tapChoice(OBSTRUCTION, "yes")
        // what the user sees (no tile selected) is what gets submitted
        assertEquals(mapOf("ext:obstruction" to "yes"), adapter.submit().applyTo(tags))

        adapter.tapChoice(OBSTRUCTION_TYPE, "bollard")
        assertTrue(adapter.submit().isEmpty)
    }

    @Test fun `text typed into a dependent question that is then hidden is not submitted`() {
        val adapter = openForm()
        adapter.tapChoice(SURFACE, "other")
        adapter.typeText(SURFACE_DESCRIPTION, "cobbles")
        adapter.tapChoice(SURFACE, "asphalt")
        assertEquals(listOf(SURFACE), adapter.submit().submittedIds())
    }

    //endregion

    //region recheck

    @Test fun `untouched fully answered form resubmits every answer to confirm it`() {
        val adapter = openForm(FULLY_ANSWERED_TAGS)
        val submission = adapter.submit()
        assertEquals(listOf(SURFACE, SURFACE_DESCRIPTION, WIDTH, OBSTRUCTION, OBSTRUCTION_TYPE), submission.submittedIds())
        assertEquals(FULLY_ANSWERED_TAGS, submission.applyTo(FULLY_ANSWERED_TAGS))
    }

    @Test fun `untouched partially answered form is nothing to submit`() {
        assertTrue(openForm(FULLY_ANSWERED_TAGS - "width").submit().isEmpty)
    }

    @Test fun `untouched fully answered form is nothing to submit in multi-select`() {
        assertTrue(openForm(FULLY_ANSWERED_TAGS).submit(multiSelect = true).isEmpty)
    }

    @Test fun `hidden questions do not count against a recheck being fully answered`() {
        val tags = mapOf("ext:surface" to "asphalt", "width" to "60", "ext:obstruction" to "no")
        assertEquals(listOf(SURFACE, WIDTH, OBSTRUCTION), openForm(tags).submit().submittedIds())
    }

    //endregion

    //region photo

    @Test fun `a new photo alone is submitted together with only its own question`() {
        val adapter = openForm(obstructionWithPhotoTags)
        val submission = adapter.submit(PhotoState(PhotoAttachment.Pending(pendingPath, 90f)))
        assertEquals(listOf(OBSTRUCTION_TYPE), submission.submittedIds())
        assertEquals(FeaturePhoto(pendingPath, 90f), submission.photo)
        assertNull(submission.photoTransition)
    }

    @Test fun `a new photo rides along with other changes`() {
        val adapter = openForm(obstructionWithPhotoTags)
        adapter.typeNumber(WIDTH, "60")
        val submission = adapter.submit(PhotoState(PhotoAttachment.Pending(pendingPath)))
        assertEquals(listOf(WIDTH), submission.submittedIds())
        assertEquals(FeaturePhoto(pendingPath, 0f), submission.photo)
    }

    @Test fun `a new photo replacing an existing one does not remove the tag up front`() {
        val adapter = openForm(obstructionWithPhotoTags)
        val photo = PhotoState(PhotoAttachment.Pending(pendingPath, replaces = PhotoAttachment.Uploaded(photoUrl)))
        assertEquals(emptyList(), adapter.submit(photo).removeTagKeys)
    }

    @Test fun `a new photo alone is nothing to submit in multi-select`() {
        val adapter = openForm(obstructionWithPhotoTags)
        assertTrue(adapter.submit(PhotoState(PhotoAttachment.Pending(pendingPath)), multiSelect = true).isEmpty)
    }

    @Test fun `removing an existing photo alone is submitted as a tag removal`() {
        val adapter = openForm(obstructionWithPhotoTags)
        val submission = adapter.submit(PhotoState(PhotoAttachment.PendingRemoval(photoUrl), existingPhotoRemoved = true))
        assertFalse(submission.isEmpty)
        assertEquals(emptyList(), submission.items)
        assertEquals(listOf(KARTAVIEW_URL_TAG), submission.removeTagKeys)
        assertNull(submission.photo)
    }

    @Test fun `an unchanged existing photo is not touched`() {
        val adapter = openForm(obstructionWithPhotoTags)
        val submission = adapter.submit(PhotoState(PhotoAttachment.Uploaded(photoUrl)))
        assertTrue(submission.isEmpty)
        assertNull(submission.photoTransition)
    }

    @Test fun `a new photo whose choice was deselected is discarded`() {
        val adapter = openForm(obstructionWithPhotoTags)
        adapter.tapChoice(OBSTRUCTION_TYPE, "other")

        val submission = adapter.submit(PhotoState(PhotoAttachment.Pending(pendingPath)))
        assertNull(submission.photo)
        assertEquals(emptyList(), submission.removeTagKeys)
        assertEquals(pendingPath, submission.photoTransition!!.fileToDelete)
        assertEquals(PhotoState(), submission.photoTransition!!.state)
        assertEquals(listOf(OBSTRUCTION_TYPE), submission.submittedIds())
    }

    @Test fun `an existing photo whose choice was deselected is removed`() {
        val adapter = openForm(obstructionWithPhotoTags)
        adapter.tapChoice(OBSTRUCTION_TYPE, "other")
        val submission = adapter.submit(PhotoState(PhotoAttachment.Uploaded(photoUrl)))
        assertEquals(listOf(KARTAVIEW_URL_TAG), submission.removeTagKeys)
        assertNull(submission.photoTransition!!.fileToDelete)
    }

    @Test fun `a replacement photo whose choice was hidden removes both`() {
        val adapter = openForm(obstructionWithPhotoTags)
        adapter.tapChoice(OBSTRUCTION, "no") // hides the whole obstruction type question
        val photo = PhotoState(PhotoAttachment.Pending(pendingPath, replaces = PhotoAttachment.Uploaded(photoUrl)))

        val submission = adapter.submit(photo)
        assertEquals(pendingPath, submission.photoTransition!!.fileToDelete)
        assertEquals(listOf(KARTAVIEW_URL_TAG), submission.removeTagKeys)
        val tags = submission.applyTo(obstructionWithPhotoTags + (KARTAVIEW_URL_TAG to photoUrl))
        assertEquals(mapOf("ext:obstruction" to "no"), tags)
    }

    //endregion
}
