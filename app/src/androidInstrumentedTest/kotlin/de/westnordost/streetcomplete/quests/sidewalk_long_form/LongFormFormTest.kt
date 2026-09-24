package de.westnordost.streetcomplete.quests.sidewalk_long_form

import android.app.Activity
import android.app.Instrumentation
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.IntentCompat
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import de.westnordost.streetcomplete.data.osm.edits.create_feature.FeaturePhoto
import de.westnordost.streetcomplete.data.osm.edits.create_feature.FeaturePhotosController
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.KARTAVIEW_URL_TAG
import org.hamcrest.Matchers.allOf
import java.io.File
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.fragment.app.commitNow
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isSelected
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.location.Location
import de.westnordost.streetcomplete.data.location.SurveyChecker
import de.westnordost.streetcomplete.data.osm.edits.update_tags.StringMapEntryModify
import de.westnordost.streetcomplete.data.osm.edits.update_tags.UpdateElementTagsAction
import de.westnordost.streetcomplete.data.osm.geometry.ElementPolylinesGeometry
import de.westnordost.streetcomplete.data.osm.mapdata.ElementType
import de.westnordost.streetcomplete.data.osm.mapdata.LatLon
import de.westnordost.streetcomplete.data.osm.mapdata.Way
import de.westnordost.streetcomplete.data.osm.osmquests.OsmQuest
import de.westnordost.streetcomplete.data.preferences.Preferences
import de.westnordost.streetcomplete.data.quest.OsmQuestKey
import de.westnordost.streetcomplete.data.quest.QuestKey
import de.westnordost.streetcomplete.data.quest.QuestType
import de.westnordost.streetcomplete.data.quest.QuestTypeRegistry
import de.westnordost.streetcomplete.data.visiblequests.HideQuestController
import de.westnordost.streetcomplete.quests.AbstractOsmQuestForm
import de.westnordost.streetcomplete.quests.AbstractQuestForm
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.Elements
import de.westnordost.streetcomplete.testutils.UiTestScreenshot
import de.westnordost.streetcomplete.util.ktx.nowAsEpochMilliseconds
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import kotlin.time.Duration.Companion.nanoseconds

/**
 * Drives the real long-form quest form (AddGenericLongForm: RecyclerView rows, text watchers,
 * tile selection, validation, submit) and asserts the tag edit it would queue for upload.
 *
 * The form is hosted by [LongFormTestHost] instead of the map, and its edits go to a
 * [RecordingEditsController] instead of the upload queue - same approach as the debug
 * "Show Quest Forms" screen. Pure submit/visibility logic is covered in depth by the unit tests
 * (androidUnitTest .../sidewalk_long_form); these tests are about the view wiring on top of it.
 */
@RunWith(AndroidJUnit4::class)
class LongFormFormTest {

    @get:Rule val testName = TestName()

    private val koin = GlobalContext.get()
    private val preferences: Preferences = koin.get()
    private val questTypeRegistry: QuestTypeRegistry = koin.get()
    private val featurePhotosController: FeaturePhotosController = koin.get()
    private val firstEditId = RecordingEditsController.FIRST_EDIT_ID + 1

    private val edits = RecordingEditsController()
    private lateinit var questType: AddGenericLong
    private lateinit var scenario: FragmentScenario<LongFormTestHost>
    private lateinit var originalTags: Map<String, String>

    private var previousQuestTypes: List<Pair<Int, QuestType>> = emptyList()
    private var wasLowBandwidth = false
    private var previousAnimationScales: List<String> = emptyList()

    private val start = LatLon(47.6, -122.3)
    private val end = LatLon(47.6005, -122.3)
    private val geometry = ElementPolylinesGeometry(listOf(listOf(start, end)), start)

    @Before
    fun setUp() {
        questType = AddGenericLong(
            Elements(elementType = QUEST_TYPE_NAME, questQuery = "ways with highway=footway", quests = sidewalkQuests()),
            recencyPeriodInDays = 90
        )
        previousQuestTypes = questTypeRegistry.ordinalsAndEntries.toList()
        questTypeRegistry.addItem(listOf(0 to questType))

        // at the element, so submitting doesn't stop at the "are you really there?" dialog
        koin.get<SurveyChecker>().addRecentLocation(
            Location(start, accuracy = 1000f, elapsedDuration = SystemClock.elapsedRealtimeNanos().nanoseconds)
        )
        // question/choice images are remote URLs - not what these tests are about
        wasLowBandwidth = preferences.isLowBandwidthModeEnabled
        preferences.isLowBandwidthModeEnabled = true
        // RecyclerView item animations (e.g. a tile's selection cross-fade) briefly keep two views
        // of the same item around, which Espresso doesn't wait for. CI's emulator runs with
        // animations off already; this makes local runs behave the same.
        previousAnimationScales = ANIMATION_SCALES.map { shell("settings get global $it").trim() }
        ANIMATION_SCALES.forEach { shell("settings put global $it 0") }
        Intents.init()
    }

    @After
    fun tearDown() {
        // before scenario.close() - see UiTestScreenshot's kdoc
        UiTestScreenshot.capture("${javaClass.simpleName}.${testName.methodName}")
        if (::scenario.isInitialized) scenario.close()
        Intents.release()
        // edit ids are fake (RecordingEditsController) but photos attached to them are real rows
        featurePhotosController.markUploaded(firstEditId)
        preferences.isLowBandwidthModeEnabled = wasLowBandwidth
        if (previousQuestTypes.isNotEmpty()) questTypeRegistry.addItem(previousQuestTypes)
        ANIMATION_SCALES.zip(previousAnimationScales).forEach { (setting, value) ->
            shell(if (value == "null") "settings delete global $setting" else "settings put global $setting $value")
        }
    }

    //region rendering and pre-fill

    @Test
    fun dependentQuestionsAreHiddenOnOpen() {
        open()
        onView(questionRow(SURFACE_Q)).check(matches(isDisplayed()))
        onView(questionRow(DESCRIPTION_Q)).check(doesNotExist())
        onView(questionRow(OBSTRUCTION_TYPE_Q)).check(doesNotExist())
    }

    @Test
    fun existingAnswersArePreFilled() {
        open(FULLY_ANSWERED_TAGS)
        assertTileSelected(SURFACE_Q, "Other")
        assertTileNotSelected(SURFACE_Q, "Asphalt")
        onView(fieldOf(DESCRIPTION_Q)).perform(scrollIntoView()).check(matches(withText("cobbles")))
        onView(fieldOf(WIDTH_Q)).perform(scrollIntoView()).check(matches(withText("60")))
        assertTileSelected(OBSTRUCTION_Q, "Yes")
        assertTileSelected(OBSTRUCTION_TYPE_Q, "Bollard")
        assertTileSelected(OBSTRUCTION_TYPE_Q, "Utility pole")
        assertTileNotSelected(OBSTRUCTION_TYPE_Q, OTHER_OBSTRUCTION)
    }

    @Test
    fun multiSelectDoesNotPreFill() {
        open(FULLY_ANSWERED_TAGS, multiSelect = true)
        assertTileNotSelected(SURFACE_Q, "Other")
        onView(fieldOf(WIDTH_Q)).perform(scrollIntoView()).check(matches(withText("")))
        onView(questionRow(DESCRIPTION_Q)).check(doesNotExist())
    }

    //endregion

    //region text entry (regression: typed text was never submitted)

    @Test
    fun textTypedIntoPreFilledFormIsSubmitted() {
        open(FULLY_ANSWERED_TAGS)
        typeInto(DESCRIPTION_Q, "cobbles and tar")
        submit()

        assertEquals(
            originalTags + ("ext:surface:description" to "cobbles and tar"),
            edits.resultingTags(originalTags)
        )
    }

    @Test
    fun textTypedOnNewElementIsSubmitted() {
        open()
        tap(SURFACE_Q, "Other")
        typeInto(DESCRIPTION_Q, "cobbles")
        submit()

        assertEquals(
            originalTags + mapOf("ext:surface" to "other", "ext:surface:description" to "cobbles"),
            edits.resultingTags(originalTags)
        )
    }

    @Test
    fun textSurvivesTheQuestionBeingHiddenAndShownAgain() {
        open()
        tap(SURFACE_Q, "Other")
        typeInto(DESCRIPTION_Q, "cobbles")
        tap(SURFACE_Q, "Asphalt")
        onView(questionRow(DESCRIPTION_Q)).check(doesNotExist())
        tap(SURFACE_Q, "Other")

        onView(fieldOf(DESCRIPTION_Q)).perform(scrollIntoView()).check(matches(withText("cobbles")))
        submit()
        assertEquals("cobbles", edits.resultingTags(originalTags)["ext:surface:description"])
    }

    @Test
    fun eachFieldWritesToItsOwnQuestionAfterRowsShift() {
        open()
        tap(SURFACE_Q, "Other") // inserts the description row above width
        typeInto(WIDTH_Q, "60")
        typeInto(DESCRIPTION_Q, "cobbles")
        typeInto(WIDTH_Q, "72")
        submit()

        val tags = edits.resultingTags(originalTags)
        assertEquals("72", tags["width"])
        assertEquals("cobbles", tags["ext:surface:description"])
    }

    //endregion

    //region validation

    @Test
    fun outOfRangeNumberBlocksSubmitUntilFixed() {
        open()
        typeInto(WIDTH_Q, "999")
        onView(inRowOf(WIDTH_Q, R.id.input)).check(matches(hasInputError("Value should be less than 240")))
        onView(withId(R.id.submitButton)).perform(scrollIntoView()).check(matches(hasAlpha(0.5f)))
        onView(withId(R.id.submitButton)).perform(click())
        assertNoEdit()

        typeInto(WIDTH_Q, "60")
        onView(inRowOf(WIDTH_Q, R.id.input)).check(matches(hasInputError(null)))
        submit()
        assertEquals("60", edits.resultingTags(originalTags)["width"])
    }

    //endregion

    //region choices and dependencies

    @Test
    fun exclusiveChoiceSetsAndDeselectingRemovesTheTag() {
        open(mapOf("ext:surface" to "asphalt"))
        tap(SURFACE_Q, "Concrete")
        assertTileSelected(SURFACE_Q, "Concrete")
        assertTileNotSelected(SURFACE_Q, "Asphalt")
        tap(SURFACE_Q, "Concrete")
        submit()
        assertFalse("ext:surface" in edits.resultingTags(originalTags))
    }

    @Test
    fun multipleChoiceAnswersAreJoined() {
        open()
        tap(OBSTRUCTION_Q, "Yes")
        tap(OBSTRUCTION_TYPE_Q, "Bollard")
        tap(OBSTRUCTION_TYPE_Q, "Utility pole")
        submit()
        val tags = edits.resultingTags(originalTags)
        assertEquals("yes", tags["ext:obstruction"])
        assertEquals("bollard;pole", tags["ext:obstruction:type"])
    }

    @Test
    fun reShownChoiceQuestionStartsBlankAndSubmitsNothingForIt() {
        open()
        tap(OBSTRUCTION_Q, "Yes")
        tap(OBSTRUCTION_TYPE_Q, "Bollard")
        tap(OBSTRUCTION_Q, "No")
        tap(OBSTRUCTION_Q, "Yes")
        assertTileNotSelected(OBSTRUCTION_TYPE_Q, "Bollard")
        submit()
        assertEquals(originalTags + ("ext:obstruction" to "yes"), edits.resultingTags(originalTags))
    }

    @Test
    fun preFilledDependentQuestionThatGetsHiddenIsRemoved() {
        open(mapOf("ext:surface" to "other", "ext:surface:description" to "cobbles"))
        tap(SURFACE_Q, "Asphalt")
        submit()
        val tags = edits.resultingTags(originalTags)
        assertEquals("asphalt", tags["ext:surface"])
        assertFalse("ext:surface:description" in tags)
    }

    //endregion

    //region submit outcomes

    @Test
    fun untouchedPartiallyAnsweredFormSubmitsNothing() {
        open(mapOf("ext:surface" to "asphalt"))
        onView(withId(R.id.submitButton)).perform(scrollIntoView(), click())
        assertNoEdit()
        onView(questionRow(SURFACE_Q)).check(matches(isDisplayed()))
    }

    @Test
    fun untouchedFullyAnsweredFormConfirmsEveryAnswer() {
        open(FULLY_ANSWERED_TAGS)
        submit()
        val changes = (edits.actions.single() as UpdateElementTagsAction).changes.changes
        assertEquals(
            FULLY_ANSWERED_TAGS.map { (k, v) -> StringMapEntryModify(k, v, v) }.toSet(),
            changes
        )
    }

    @Test
    fun closeButtonClosesWithoutEditing() {
        open(mapOf("ext:surface" to "asphalt"))
        tap(SURFACE_Q, "Concrete")
        onView(withId(R.id.close_button)).perform(click())
        scenario.onFragment { assertTrue(it.closed) }
        assertNoEdit()
    }

    //endregion

    //region photo

    @Test
    fun photoPromptAppearsAndCaptureShowsTheCardUntilDeleted() {
        open(mapOf("ext:obstruction" to "yes"))
        onView(inRowOf(OBSTRUCTION_TYPE_Q, R.id.choice_follow_up)).check(matches(not(isDisplayed())))
        tap(OBSTRUCTION_TYPE_Q, OTHER_OBSTRUCTION)
        onView(inRowOf(OBSTRUCTION_TYPE_Q, R.id.choice_follow_up))
            .perform(scrollIntoView())
            .check(matches(allOf(isDisplayed(), withText(PHOTO_FOLLOW_UP))))

        capturePhoto()
        onView(inRowOf(OBSTRUCTION_TYPE_Q, R.id.photo_title)).perform(scrollIntoView()).check(matches(withText("Photo attached")))
        onView(inRowOf(OBSTRUCTION_TYPE_Q, R.id.choice_follow_up)).check(matches(not(isDisplayed())))

        onView(inRowOf(OBSTRUCTION_TYPE_Q, R.id.photo_delete)).perform(scrollIntoView(), click())
        onView(inRowOf(OBSTRUCTION_TYPE_Q, R.id.photo_card)).check(matches(not(isDisplayed())))
        onView(inRowOf(OBSTRUCTION_TYPE_Q, R.id.choice_follow_up)).check(matches(isDisplayed()))
    }

    @Test
    fun capturedPhotoIsAttachedToTheSubmittedEdit() {
        open(mapOf("ext:obstruction" to "yes", "ext:obstruction:type" to "bollard;other"))
        capturePhoto()
        submit()

        // a photo alone rides along on its own question's (unchanged) tag
        val changes = (edits.actions.single() as UpdateElementTagsAction).changes.changes
        assertEquals(setOf(StringMapEntryModify("ext:obstruction:type", "bollard;other", "bollard;other")), changes)
        val photos = waitForPhotos(firstEditId)
        assertEquals(1, photos.size)
        assertTrue(File(photos.single().path).exists())
    }

    @Test
    fun existingPhotoCanBeMarkedForRemovalUndoneAndRemoved() {
        open(mapOf("ext:obstruction" to "yes", "ext:obstruction:type" to "other", KARTAVIEW_URL_TAG to PHOTO_URL))
        onView(inRowOf(OBSTRUCTION_TYPE_Q, R.id.photo_title)).perform(scrollIntoView()).check(matches(withText("Photo from last visit")))

        onView(inRowOf(OBSTRUCTION_TYPE_Q, R.id.photo_delete)).perform(scrollIntoView(), click())
        onView(inRowOf(OBSTRUCTION_TYPE_Q, R.id.photo_title)).check(matches(withText("Photo will be removed")))
        onView(inRowOf(OBSTRUCTION_TYPE_Q, R.id.photo_undo)).perform(scrollIntoView(), click())
        onView(inRowOf(OBSTRUCTION_TYPE_Q, R.id.photo_title)).check(matches(withText("Photo from last visit")))

        onView(inRowOf(OBSTRUCTION_TYPE_Q, R.id.photo_delete)).perform(scrollIntoView(), click())
        submit()
        assertFalse(KARTAVIEW_URL_TAG in edits.resultingTags(originalTags))
    }

    @Test
    fun capturedPhotoIsDiscardedWhenItsChoiceIsDeselected() {
        open(mapOf("ext:obstruction" to "yes", "ext:obstruction:type" to "bollard;other"))
        capturePhoto()
        tap(OBSTRUCTION_TYPE_Q, OTHER_OBSTRUCTION)
        onView(inRowOf(OBSTRUCTION_TYPE_Q, R.id.photo_card)).check(matches(not(isDisplayed())))
        submit()

        assertEquals("bollard", edits.resultingTags(originalTags)["ext:obstruction:type"])
        Thread.sleep(NO_EDIT_GRACE_MS)
        assertTrue(featurePhotosController.get(firstEditId).isEmpty())
    }

    //endregion

    //region helpers

    private fun open(tags: Map<String, String> = emptyMap(), multiSelect: Boolean = false) {
        originalTags = mapOf("highway" to "footway", "footway" to "sidewalk") + tags
        val element = Way(1L, listOf(1L, 2L), originalTags, 1, nowAsEpochMilliseconds())
        val questKey = OsmQuestKey(ElementType.WAY, element.id, questType.name)

        val form = questType.createForm()
        form.requireArguments().putAll(AbstractQuestForm.createArguments(questKey, questType, geometry, 0.0, 0.0))
        form.requireArguments().putAll(AbstractOsmQuestForm.createArguments(element))
        form.addElementEditsController = edits
        form.hideQuestController = object : HideQuestController {
            override fun hide(key: QuestKey) {}
        }

        scenario = launchFragmentInContainer<LongFormTestHost>(themeResId = R.style.AppTheme)
        scenario.onFragment { host ->
            if (multiSelect) {
                host.mutableMultiSelectQuests.add(OsmQuest(questType, ElementType.WAY, 2L, geometry))
            }
            host.childFragmentManager.commitNow { replace(host.containerId, form) }
            form.expand()
        }
    }

    private fun tap(question: String, choice: String) {
        onView(tileOf(question, choice)).perform(scrollIntoView(), click())
    }

    private fun typeInto(question: String, text: String) {
        onView(fieldOf(question)).perform(scrollIntoView(), replaceText(text), closeSoftKeyboard())
    }

    private fun assertTileSelected(question: String, choice: String) {
        onView(tileOf(question, choice)).perform(scrollIntoView()).check(matches(isSelected()))
    }

    private fun assertTileNotSelected(question: String, choice: String) {
        onView(tileOf(question, choice)).perform(scrollIntoView()).check(matches(not(isSelected())))
    }

    /** Taps Submit and waits for the edit - applyAnswer runs in a coroutine Espresso can't see. */
    private fun submit() {
        onView(withId(R.id.submitButton)).perform(scrollIntoView(), click())
        val deadline = SystemClock.uptimeMillis() + EDIT_TIMEOUT_MS
        while (edits.actions.isEmpty() && SystemClock.uptimeMillis() < deadline) {
            Thread.sleep(50)
        }
        assertEquals("expected exactly one recorded edit", 1, edits.actions.size)
    }

    private fun assertNoEdit() {
        Thread.sleep(NO_EDIT_GRACE_MS)
        assertTrue("expected no edit, got ${edits.actions}", edits.actions.isEmpty())
        scenario.onFragment { assertEquals(0, it.editedCount) }
    }

    /** Taps the photo prompt (or retake) with the camera stubbed to "take" a small real JPEG -
     *  onTookPhoto reads its EXIF and rescales it, so an empty file wouldn't do. */
    private fun capturePhoto() {
        intending(hasAction(MediaStore.ACTION_IMAGE_CAPTURE)).respondWithFunction { intent ->
            val uri = IntentCompat.getParcelableExtra(intent, MediaStore.EXTRA_OUTPUT, Uri::class.java)!!
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            context.contentResolver.openOutputStream(uri)!!.use {
                Bitmap.createBitmap(64, 48, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.JPEG, 90, it)
            }
            Instrumentation.ActivityResult(Activity.RESULT_OK, null)
        }
        onView(inRowOf(OBSTRUCTION_TYPE_Q, R.id.choice_follow_up)).perform(scrollIntoView(), click())
    }

    private fun waitForPhotos(editId: Long): List<FeaturePhoto> {
        val deadline = SystemClock.uptimeMillis() + EDIT_TIMEOUT_MS
        var photos = featurePhotosController.get(editId)
        while (photos.isEmpty() && SystemClock.uptimeMillis() < deadline) {
            Thread.sleep(50)
            photos = featurePhotosController.get(editId)
        }
        return photos
    }

    private fun shell(command: String): String {
        val pfd = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes().decodeToString() }
    }

    //endregion

    private companion object {
        val ANIMATION_SCALES = listOf("animator_duration_scale", "transition_animation_scale", "window_animation_scale")
        const val QUEST_TYPE_NAME = "LongFormUiTestSidewalks"
        const val PHOTO_URL = "https://kartaview.org/details/1/2/track-info"
        const val EDIT_TIMEOUT_MS = 5_000L
        const val NO_EDIT_GRACE_MS = 1_000L
    }
}
