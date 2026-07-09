package de.westnordost.streetcomplete.quests

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.hardware.SensorManager
import android.location.Location
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.PopupMenu
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.getSystemService
import androidx.core.os.bundleOf
import androidx.core.view.children
import com.google.android.material.snackbar.Snackbar
import de.westnordost.osmfeatures.Feature
import de.westnordost.osmfeatures.FeatureDictionary
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.karta_view.KartaViewApiClient
import de.westnordost.streetcomplete.data.location.SurveyChecker
import de.westnordost.streetcomplete.data.osm.edits.AddElementEditsController
import de.westnordost.streetcomplete.data.osm.edits.ElementEditAction
import de.westnordost.streetcomplete.data.osm.edits.ElementEditType
import de.westnordost.streetcomplete.data.osm.edits.ElementEditsController
import de.westnordost.streetcomplete.data.osm.edits.MapDataWithEditsSource
import de.westnordost.streetcomplete.data.osm.edits.delete.DeletePoiNodeAction
import de.westnordost.streetcomplete.data.osm.edits.update_tags.StringMapChanges
import de.westnordost.streetcomplete.data.osm.edits.update_tags.StringMapChangesBuilder
import de.westnordost.streetcomplete.data.osm.edits.update_tags.UpdateElementTagsAction
import de.westnordost.streetcomplete.data.osm.geometry.ElementGeometry
import de.westnordost.streetcomplete.data.osm.geometry.ElementPolylinesGeometry
import de.westnordost.streetcomplete.data.osm.mapdata.Element
import de.westnordost.streetcomplete.data.osm.mapdata.ElementType
import de.westnordost.streetcomplete.data.osm.mapdata.LatLon
import de.westnordost.streetcomplete.data.osm.mapdata.Node
import de.westnordost.streetcomplete.data.osm.mapdata.Way
import de.westnordost.streetcomplete.data.osm.osmquests.OsmElementQuestType
import de.westnordost.streetcomplete.data.osm.osmquests.OsmQuest
import de.westnordost.streetcomplete.data.osmnotes.edits.NoteEditAction
import de.westnordost.streetcomplete.data.osmnotes.edits.NoteEditsController
import de.westnordost.streetcomplete.data.quest.Quest
import de.westnordost.streetcomplete.data.quest.QuestKey
import de.westnordost.streetcomplete.data.visiblequests.HideQuestController
import de.westnordost.streetcomplete.data.visiblequests.QuestsHiddenController
import de.westnordost.streetcomplete.osm.applyReplacePlaceTo
import de.westnordost.streetcomplete.quests.sidewalk_long_form.AddGenericLong
import de.westnordost.streetcomplete.screens.main.map.Compass
import de.westnordost.streetcomplete.util.getNameAndLocationSpanned
import de.westnordost.streetcomplete.util.ktx.isSplittable
import de.westnordost.streetcomplete.util.ktx.viewLifecycleScope
import de.westnordost.streetcomplete.view.add
import de.westnordost.streetcomplete.view.confirmIsSurvey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named
import java.io.ByteArrayOutputStream
import java.util.Locale
import kotlin.math.PI

/** Abstract base class for any bottom sheet with which the user answers a specific quest(ion)  */
abstract class AbstractOsmQuestForm<T> : AbstractQuestForm(), IsShowingQuestDetails {

    // dependencies
    private val elementEditsController: ElementEditsController by inject()
    private val noteEditsController: NoteEditsController by inject()
    private val hiddenQuestsController: QuestsHiddenController by inject()
    private val featureDictionaryLazy: Lazy<FeatureDictionary> by inject(named("FeatureDictionaryLazy"))
    private val mapDataWithEditsSource: MapDataWithEditsSource by inject()
    private val surveyChecker: SurveyChecker by inject()

    protected val featureDictionary: FeatureDictionary get() = featureDictionaryLazy.value
    private val kartaViewApiClient: KartaViewApiClient by inject()
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>

    // only used for testing / only used for ShowQuestFormsScreen! Found no better way to do this
    var addElementEditsController: AddElementEditsController = elementEditsController
    var hideQuestController: HideQuestController = hiddenQuestsController

    // passed in parameters
    private val osmElementQuestType: OsmElementQuestType<T> get() = questType as OsmElementQuestType<T>
    protected lateinit var element: Element private set

    private val englishResources: Resources
        get() {
            val conf = Configuration(resources.configuration)
            conf.setLocale(Locale.ENGLISH)
            val localizedContext = super.requireContext().createConfigurationContext(conf)
            return localizedContext.resources
        }

    // overridable by child classes
    open val otherAnswers = listOf<IAnswerItem>()
    open val buttonPanelAnswers = listOf<IAnswerItem>()
    private var compassBearing: Double = 0.0

    interface Listener {
        /** The GPS position at which the user is displayed at */
        val displayedMapLocation: Location?
        val mutableMultiSelectQuests: MutableList<Quest>

        /** Called when the user successfully answered the quest */
        fun onEdited(editType: ElementEditType, geometry: ElementGeometry)

        /** Called when the user chose to leave a note instead */
        fun onComposeNote(
            editType: ElementEditType,
            element: Element,
            geometry: ElementGeometry,
            leaveNoteContext: String,
        )

        fun onCloseDialog()

        /** Called when the user chose to split the way */
        fun onSplitWay(editType: ElementEditType, way: Way, geometry: ElementPolylinesGeometry)

        /** Called when the user chose to move the node */
        fun onMoveNode(editType: ElementEditType, node: Node)

        /** Called when the user chose to hide the quest instead */
        fun onQuestHidden(questKey: QuestKey)
    }

    private val listener: Listener? get() = parentFragment as? Listener ?: activity as? Listener
    private lateinit var compass: Compass

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        compass = Compass(
            context?.getSystemService<SensorManager>()!!,
            context?.getSystemService<WindowManager>()!!.defaultDisplay,
            this::onCompassRotationChanged
        )
        lifecycle.addObserver(compass)

        val args = requireArguments()

        val getElement: Element? = args.getString(ARG_ELEMENT)?.let {
            Json.decodeFromString(it)
        }
        if (getElement != null) {
            element = getElement
        }
        cameraLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    showProgressbar()
                    // Handle the image capture result here
                    val bitmap = result.data?.extras?.getParcelable<Bitmap>("data")
                    startKartViewFlow(bitmap)
                } else {

                    // Handle the error state here
                }
            }
    }

    private fun onCompassRotationChanged(rot: Float, tilt: Float) {
        compassBearing = rot * 180 / PI
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (osmElementQuestType is AddGenericLong) {
            val category = (osmElementQuestType as AddGenericLong).item.elementType
            // the category alone ("Sidewalk", "Kerb"...) doesn't distinguish between several
            // queued quests of the same type - add the OSM element type and id
            val typeAndId = "${element.type.name.lowercase().replaceFirstChar { it.uppercase() }} #${element.id}"
            setTitle("$category — $typeAndId")
        }

        setHideQuestOnClick { hideQuest() }
        setCloseQuestOnClick(listener)
    }

    override fun onStart() {
        super.onStart()
        updateButtonPanel()
    }

    protected fun updateButtonPanel() {
        // val otherAnswersItem =
        //     AnswerItem(R.string.quest_generic_otherAnswers2) { showOtherAnswers() }
        setButtonPanelAnswers(buttonPanelAnswers)
    }

    private fun assembleOtherAnswers(): List<IAnswerItem> {
        val answers = mutableListOf<IAnswerItem>()

        answers.add(AnswerItem(R.string.quest_generic_answer_notApplicable) { onClickCantSay() })

        if (element.isSplittable()) {
            answers.add(AnswerItem(R.string.quest_generic_answer_differs_along_the_way) { onClickSplitWayAnswer() })
        }
        createDeleteOrReplaceElementAnswer()?.let { answers.add(it) }

        if (element is Node // add moveNodeAnswer only if it's a free floating node
            && mapDataWithEditsSource.getWaysForNode(element.id).isEmpty()
            && mapDataWithEditsSource.getRelationsForNode(element.id).isEmpty()
        ) {
            answers.add(AnswerItem(R.string.move_node) { onClickMoveNodeAnswer() })
        }

        answers.addAll(otherAnswers)
        return answers
    }

    private fun createDeleteOrReplaceElementAnswer(): AnswerItem? {
        val isDeletePoiEnabled =
            osmElementQuestType.isDeleteElementEnabled && element.type == ElementType.NODE
        val isReplacePlaceEnabled = osmElementQuestType.isReplacePlaceEnabled
        if (!isDeletePoiEnabled && !isReplacePlaceEnabled) return null
        check(!(isDeletePoiEnabled && isReplacePlaceEnabled)) {
            "Only isDeleteElementEnabled OR isReplaceShopEnabled may be true at the same time"
        }

        return AnswerItem(R.string.quest_generic_answer_does_not_exist) {
            if (isDeletePoiEnabled) {
                deletePoiNode()
            } else if (isReplacePlaceEnabled) {
                replacePlace()
            }
        }
    }

    private fun showOtherAnswers() {
        val otherAnswersButton =
            view?.findViewById<ViewGroup>(R.id.buttonPanel)?.children?.firstOrNull() ?: return
        val answers = assembleOtherAnswers()
        val popup = PopupMenu(requireContext(), otherAnswersButton)
        for (i in answers.indices) {
            val otherAnswer = answers[i]
            val order = answers.size - i
            popup.menu.add(Menu.NONE, i, order, otherAnswer.title)
        }
        popup.show()

        popup.setOnMenuItemClickListener { item ->
            answers[item.itemId].action()
            true
        }
    }

    protected fun onClickCantSay() {
        context?.let {
            AlertDialog.Builder(it)
                .setTitle(R.string.quest_leave_new_note_title)
                .setMessage(R.string.quest_leave_new_note_description)
                .setNegativeButton(R.string.quest_leave_new_note_no) { _, _ -> hideQuest() }
                .setPositiveButton(R.string.quest_leave_new_note_yes) { _, _ -> composeNote() }
                .show()
        }
    }

    private fun onClickSplitWayAnswer() {
        context?.let {
            AlertDialog.Builder(it)
                .setMessage(R.string.quest_split_way_description)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    listener?.onSplitWay(
                        osmElementQuestType,
                        element as Way,
                        geometry as ElementPolylinesGeometry
                    )
                }
                .show()
        }
    }

    private fun onClickMoveNodeAnswer() {
        context?.let {
            AlertDialog.Builder(it)
                .setMessage(R.string.quest_move_node_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    listener?.onMoveNode(osmElementQuestType, element as Node)
                }
                .show()
        }
    }

    protected fun applyAnswer(
        answer: T,
        extraTagList: MutableList<Pair<String, String>> = mutableListOf(),
    ) {
        viewLifecycleScope.launch {
            listener?.mutableMultiSelectQuests?.let { quests ->
                ArrayList(quests).let {
                    if (it.isNotEmpty()) {

                        val elements = mutableListOf<Pair<Element, ElementGeometry>>()
                        for (msQuest in it) {
                            if (msQuest is OsmQuest) {
                                val element = withContext(Dispatchers.IO) {
                                    mapDataWithEditsSource.get(
                                        msQuest.elementType,
                                        msQuest.elementId
                                    )
                                } ?: return@launch
                                elements.add(element to msQuest.geometry)
                            }
                        }

                        for (element in elements) {
                            solve(
                                UpdateElementTagsAction(
                                    element.first,
                                    createQuestChanges(answer, extraTagList, element.first, element.second)
                                ), element.second
                            )
                        }
                    } else {
                        solve(
                            UpdateElementTagsAction(
                                element,
                                createQuestChanges(answer, extraTagList)
                            ), geometry
                        )
                    }
                }
            }
        }
    }

    private fun createQuestChanges(
        answer: T,
        extraTagList: MutableList<Pair<String, String>> = mutableListOf(),
        forElement: Element = element,
        forGeometry: ElementGeometry = geometry,
    ): StringMapChanges {
        val changesBuilder = StringMapChangesBuilder(forElement.tags)
        extraTagList.forEach { changesBuilder[it.first] = it.second }
        osmElementQuestType.applyAnswerTo(answer, changesBuilder, forGeometry, forElement.timestampEdited)
        val changes = changesBuilder.create()
        require(!changes.isEmpty()) {
            "${osmElementQuestType.name} was answered by the user but there are no changes!"
        }
        return changes
    }

    protected fun composeNote() {

        val questTitle = englishResources.getString(osmElementQuestType.getTitle(element.tags))
        val hintLabel = getNameAndLocationSpanned(element, englishResources, featureDictionary)
        val leaveNoteContext = if (hintLabel.isNullOrBlank()) {
            "Unable to answer \"$questTitle\""
        } else {
            "Unable to answer \"$questTitle\" – $hintLabel"
        }
        listener?.onComposeNote(osmElementQuestType, element, geometry, leaveNoteContext)
    }

    protected fun hideQuest() {
        viewLifecycleScope.launch {
            withContext(Dispatchers.IO) { hideQuestController.hide(questKey) }
            listener?.onQuestHidden(questKey)
        }
    }

    protected fun replacePlace() {
        composeNote()
    }

    protected fun deletePoiNode() {
        AlertDialog.Builder(requireContext())
            .setMessage(R.string.osm_element_gone_description)
            .setPositiveButton(R.string.osm_element_gone_confirmation) { _, _ -> onDeletePoiNodeConfirmed() }
            .setNeutralButton(R.string.leave_note) { _, _ -> composeNote() }
            .show()
    }

    private fun onDeletePoiNodeConfirmed() {
        viewLifecycleScope.launch {
            solve(DeletePoiNodeAction(element as Node), geometry)
        }
    }

    private suspend fun solve(action: ElementEditAction, geometry: ElementGeometry) {
        setLocked(true)
        val isSurvey = surveyChecker.checkIsSurvey(geometry)
        if (!isSurvey && !confirmIsSurvey(requireContext())) {
            setLocked(false)
            return
        }
        withContext(Dispatchers.IO) {
            if (action is UpdateElementTagsAction && !action.changes.isValid()) {
                val questTitle =
                    englishResources.getString(osmElementQuestType.getTitle(element.tags))
                val text = createNoteTextForTooLongTags(
                    questTitle,
                    element.type,
                    element.id,
                    action.changes.changes
                )
                noteEditsController.add(0, NoteEditAction.CREATE, geometry.center, text)
            } else {
                addElementEditsController.add(
                    osmElementQuestType,
                    geometry,
                    "survey",
                    action,
                    isSurvey
                )
            }
        }
        listener?.onEdited(osmElementQuestType, geometry)
    }

    override fun setCameraIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            cameraLauncher.launch(takePictureIntent)
        } catch (e: ActivityNotFoundException) {
            // Display error state to the user
        }
    }

    private fun startKartViewFlow(bitmap: Bitmap?) {
        viewLifecycleScope.launch {
            val displayedLocation = listener?.displayedMapLocation
            if (bitmap == null || displayedLocation == null) {
                hideProgressbar()
                return@launch
            }
            val bearing = if (displayedLocation.hasBearing() && displayedLocation.bearing != 0f) {
                displayedLocation.bearing
            } else {
                compassBearing.toFloat()
            }
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
            try {
                val urls = kartaViewApiClient.uploadImages(
                    listOf(byteArrayOutputStream.toByteArray()),
                    LatLon(displayedLocation.latitude, displayedLocation.longitude),
                    bearing
                )
                showSnackBar("Image Uploaded Successfully", view, requireActivity() as ComponentActivity)
                onImageUrlReceived(urls.first())
            } catch (e: Exception) {
                Log.e("KartViewFlow", "KartaView upload failed", e)
                showSnackBar(
                    e.message ?: "Image upload failed. Please try again later.",
                    view, requireActivity() as ComponentActivity
                )
            } finally {
                hideProgressbar()
            }
        }
    }

    private fun showSnackBar(message: String, view: View?, componentActivity: ComponentActivity) {
        if (view != null) {
            Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val ARG_ELEMENT = "element"
        private const val ARG_DISPLAYED_LOCATION = "displayedLocation"

        fun createArguments(element: Element, displayedLocation: Location? = null) = bundleOf(
            ARG_ELEMENT to Json.encodeToString(element),
            ARG_DISPLAYED_LOCATION to displayedLocation
        )
    }
}
