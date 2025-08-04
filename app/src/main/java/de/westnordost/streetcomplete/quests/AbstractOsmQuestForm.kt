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
import de.westnordost.osmfeatures.FeatureDictionary
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.karta_view.domain.model.CreateSequenceResponse
import de.westnordost.streetcomplete.data.karta_view.domain.model.ImageUploadResponse
import de.westnordost.streetcomplete.data.location.RecentLocationStore
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
import de.westnordost.streetcomplete.data.osm.mapdata.Node
import de.westnordost.streetcomplete.data.osm.mapdata.Way
import de.westnordost.streetcomplete.data.osm.osmquests.HideOsmQuestController
import de.westnordost.streetcomplete.data.osm.osmquests.OsmElementQuestType
import de.westnordost.streetcomplete.data.osm.osmquests.OsmQuest
import de.westnordost.streetcomplete.data.osm.osmquests.OsmQuestsHiddenController
import de.westnordost.streetcomplete.data.osmnotes.edits.NoteEditAction
import de.westnordost.streetcomplete.data.osmnotes.edits.NoteEditsController
import de.westnordost.streetcomplete.data.preferences.Preferences
import de.westnordost.streetcomplete.data.quest.OsmQuestKey
import de.westnordost.streetcomplete.data.quest.Quest
import de.westnordost.streetcomplete.data.user.UserLoginSource
import de.westnordost.streetcomplete.osm.isPlaceOrDisusedPlace
import de.westnordost.streetcomplete.osm.replacePlace
import de.westnordost.streetcomplete.quests.shop_type.ShopGoneDialog
import de.westnordost.streetcomplete.quests.sidewalk_long_form.AddGenericLong
import de.westnordost.streetcomplete.screens.main.map.Compass
import de.westnordost.streetcomplete.util.getNameAndLocationLabel
import de.westnordost.streetcomplete.util.ktx.geometryType
import de.westnordost.streetcomplete.util.ktx.isSplittable
import de.westnordost.streetcomplete.util.ktx.viewLifecycleScope
import de.westnordost.streetcomplete.view.add
import de.westnordost.streetcomplete.view.checkIsSurvey
import de.westnordost.streetcomplete.view.confirmIsSurvey
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
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
    private val osmQuestsHiddenController: OsmQuestsHiddenController by inject()
    private val featureDictionaryLazy: Lazy<FeatureDictionary> by inject(named("FeatureDictionaryLazy"))
    private val mapDataWithEditsSource: MapDataWithEditsSource by inject()
    private val recentLocationStore: RecentLocationStore by inject()
    private val httpClient: HttpClient by inject()
    private val preferences: Preferences by inject()
    private val userLoginSource: UserLoginSource by inject()
    protected val featureDictionary: FeatureDictionary get() = featureDictionaryLazy.value
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>

    // only used for testing / only used for ShowQuestFormsScreen! Found no better way to do this
    var addElementEditsController: AddElementEditsController = elementEditsController
    var hideOsmQuestController: HideOsmQuestController = osmQuestsHiddenController

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

    private lateinit var compass: Compass
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
        fun onQuestHidden(osmQuestKey: OsmQuestKey)
    }

    private val listener: Listener? get() = parentFragment as? Listener ?: activity as? Listener

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
        val displayedLocation = args.getParcelable<Location>(ARG_DISPLAYED_LOCATION)
        cameraLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    showProgressbar()
                    // Handle the image capture result here
                    val bitmap = result.data?.extras?.getParcelable<Bitmap>("data")
                    startKartViewFlow(bitmap, displayedLocation)
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
            setTitle((osmElementQuestType as AddGenericLong).item.elementType)
        } else {
            setTitle(resources.getHtmlQuestTitle(osmElementQuestType, element.tags))
        }

        setHideQuestOnClick { hideQuest() }
        setCloseQuestOnClick(listener)
    }

    override fun onStart() {
        super.onStart()
        updateButtonPanel()
    }

    override fun onPause() {
        super.onPause()
    }

    protected fun updateButtonPanel() {
        // val otherAnswersItem = AnswerItem(R.string.quest_generic_otherAnswers2) { showOtherAnswers() }
        setButtonPanelAnswers(buttonPanelAnswers)
    }

    override fun setCameraIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            cameraLauncher.launch(takePictureIntent)
        } catch (e: ActivityNotFoundException) {
            // Display error state to the user
        }
    }

    private fun startKartViewFlow(bitmap: Bitmap?, displayedLocation: Location?) {
        viewLifecycleScope.launch {
            val sequenceId = createSequence()
            sequenceId?.apply {
                val isUploaded = uploadImageInSequence(this, bitmap, displayedLocation)
                if (isUploaded.first) {
                    //https://storage13.openstreetcam.org/files/photo/2024/11/19/lth/10206921_53966_673c7da2672cf.jpg
                    //After storage13 add .openstreetcam.org in the url
                    // FInd where storage13 is in the first
                    val first = isUploaded.second?.first?.replace(
                        "storage13",
                        "storage13.openstreetcam.org"
                    )
                    val url = "https://${first}/lth/${isUploaded.second?.second}"
                    onImageUrlReceived(url)
                    closeSequence(this)
                } else {
                    hideProgressbar()
                    Log.e("KartViewFlow", "Image upload failed")
                }
            }
        }
    }

    private suspend fun closeSequence(sequenceId: String) {
        val token = "96aca5c4b80709fc6d9aced613b51905c0fbc37870640d7bdabede269165bde7"
        val response =
            httpClient.post("https://api.openstreetcam.org/1.0/sequence/finished-uploading/") {
                setBody(MultiPartFormDataContent(formData {
                    append("access_token", token)
                    append("sequenceId", sequenceId)
                }))
            }
        if (response.status == HttpStatusCode.OK) {
            val sequence = response.body<CreateSequenceResponse>()
            Log.d("KartViewSequence", sequence.status.httpMessage)
        } else {
            showSnackBar("Failed to close KartaView Sequence. Please try again later " + response.status,
                view, requireActivity() as ComponentActivity)
        }
        hideProgressbar()
    }

    private suspend fun uploadImageInSequence(
        sequenceId: String,
        bitmap: Bitmap?,
        location: Location?,
    ): Pair<Boolean, Pair<String, String>?> {

        val displayedLocation = recentLocationStore.get().first()

        bitmap?.let {
            val byteArrayOutputStream = ByteArrayOutputStream()
            it.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()

            val response = httpClient.post("https://api.openstreetcam.org/1.0/photo/") {
                setBody(MultiPartFormDataContent(formData {
                    append(
                        "access_token",
                        "96aca5c4b80709fc6d9aced613b51905c0fbc37870640d7bdabede269165bde7"
                    )
                    append("sequenceId", sequenceId)
                    append("sequenceIndex", 1)
                    append(
                        "coordinate",
                        "${displayedLocation?.latitude},${displayedLocation?.longitude}"
                    )
                    var finalBearing = 0.0f
                    finalBearing = if (displayedLocation.hasBearing() && displayedLocation.bearing != 0f) {
                        displayedLocation.bearing
                    } else {
                        compassBearing.toFloat()
                    }
                    append("headers", finalBearing.toInt().toString())
                    append("photo", byteArray, Headers.build {
                        append(HttpHeaders.ContentType, "image/jpeg")
                        append(HttpHeaders.ContentDisposition, "filename=\"wework-kartaview.jpg\"")
                    })
                }))
            }

            if (response.status == HttpStatusCode.OK) {
                val uploadResponse = response.body<ImageUploadResponse>()
                val pair = Pair(uploadResponse.osv.photo.path, uploadResponse.osv.photo.photoName)
                showSnackBar(
                    "Image Uploaded Successfully",
                    view,
                    requireActivity() as ComponentActivity
                )
                Log.d("UploadImage", "Image uploaded successfully")
                return Pair(true, pair)
            } else {
                Log.e("UploadImage", "Image upload failed: ${response.status}")
                showSnackBar("Failed to upload image to KartaView. Please try again later " + response.status,
                    view, requireActivity() as ComponentActivity)
                hideProgressbar()
                return Pair(false, null)
            }
        }
        return Pair(false, null)
    }

    private fun showSnackBar(message: String, view: View?, componentActivity: ComponentActivity) {
        if (view != null) {
            Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
        }
    }

    private suspend fun createSequence(): String? {
        val token = "96aca5c4b80709fc6d9aced613b51905c0fbc37870640d7bdabede269165bde7"
        val response = httpClient.post("https://api.openstreetcam.org/1.0/sequence/") {
            setBody(MultiPartFormDataContent(formData {
                append("access_token", token)
            }))
        }
        if (response.status == HttpStatusCode.OK) {
            val sequence = response.body<CreateSequenceResponse>()
            sequence.osv.sequence?.id?.let { Log.d("KartViewSequence", it) }
            Log.d("KartViewStatus", sequence.status.httpMessage)
            return sequence.osv.sequence?.id
        } else {
            hideProgressbar()
            showSnackBar("Failed to create KartaView sequence. Image upload failed. Please try again later " + response.status,
                view, requireActivity() as ComponentActivity)
            return null
        }
    }

    private fun getCardinalDirection(bearing: Float): String {
        val normalizedBearing = (bearing % 360 + 360) % 360 // Normalize to 0..360 range
        return when (normalizedBearing) {
            in 0f..22.5f, in 337.5f..360f -> "North"
            in 22.5f..67.5f -> "North East"
            in 67.5f..112.5f -> "East"
            in 112.5f..157.5f -> "South East"
            in 157.5f..202.5f -> "South"
            in 202.5f..247.5f -> "South West"
            in 247.5f..292.5f -> "West"
            in 292.5f..337.5f -> "North West"
            else -> "Invalid Bearing"
        }
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
        extraTagList: MutableList<Pair<String, String>> = mutableListOf()
    ) {
        viewLifecycleScope.launch {
            listener?.mutableMultiSelectQuests?.let { quests ->
                ArrayList(quests).let {
                    if (it.isNotEmpty()) {

                        val elements = mutableListOf<Element>()
                        for (msQuest in it) {
                            if (msQuest is OsmQuest) {
                                val element = withContext(Dispatchers.IO) {
                                    mapDataWithEditsSource.get(
                                        msQuest.elementType,
                                        msQuest.elementId
                                    )
                                } ?: return@launch
                                elements.add(element)
                            }
                        }

                        for (element in elements) {
                            solve(
                                UpdateElementTagsAction(
                                    element,
                                    createQuestChanges(answer, extraTagList)
                                )
                            )
                        }
                    } else {
                        solve(
                            UpdateElementTagsAction(
                                element,
                                createQuestChanges(answer, extraTagList)
                            )
                        )
                    }
                }
            }
        }
    }

    private fun createQuestChanges(
        answer: T,
        extraTagList: MutableList<Pair<String, String>> = mutableListOf()
    ): StringMapChanges {
        val changesBuilder = StringMapChangesBuilder(element.tags)
        extraTagList.forEach { changesBuilder[it.first] = it.second }
        osmElementQuestType.applyAnswerTo(answer, changesBuilder, geometry, element.timestampEdited)
        val changes = changesBuilder.create()
        require(!changes.isEmpty()) {
            "${osmElementQuestType.name} was answered by the user but there are no changes!"
        }
        return changes
    }

    protected fun composeNote() {
        val questTitle = englishResources.getQuestTitle(osmElementQuestType, element.tags)
        val hintLabel = getNameAndLocationLabel(element, englishResources, featureDictionary)
        val leaveNoteContext = if (hintLabel.isNullOrBlank()) {
            "Unable to answer \"$questTitle\""
        } else {
            "Unable to answer \"$questTitle\" – $hintLabel"
        }
        listener?.onComposeNote(osmElementQuestType, element, geometry, leaveNoteContext)
    }

    protected fun hideQuest() {
        viewLifecycleScope.launch {
            listener?.mutableMultiSelectQuests?.let { quests ->
                ArrayList(quests).let {
                    if (it.isNotEmpty()) {
                        for (element in it) {
                            hideOsmQuestController.hide(element.key as OsmQuestKey)
                            listener?.onQuestHidden(element.key as OsmQuestKey)
                        }
                    } else {
                        withContext(Dispatchers.IO) { hideOsmQuestController.hide(questKey as OsmQuestKey) }
                        listener?.onQuestHidden(questKey as OsmQuestKey)
                    }
                }
            }

        }
    }

    protected fun replacePlace() {
        if (element.isPlaceOrDisusedPlace()) {
            ShopGoneDialog(
                requireContext(),
                element.geometryType,
                countryOrSubdivisionCode,
                featureDictionary,
                onSelectedFeature = this::onShopReplacementSelected,
                onLeaveNote = this::composeNote
            ).show()
        } else {
            composeNote()
        }
    }

    private fun onShopReplacementSelected(tags: Map<String, String>) {
        viewLifecycleScope.launch {
            val builder = StringMapChangesBuilder(element.tags)
            builder.replacePlace(tags)
            solve(UpdateElementTagsAction(element, builder.create()))
        }
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
            solve(DeletePoiNodeAction(element as Node))
        }
    }

    private suspend fun solve(action: ElementEditAction) {
        setLocked(true)
        val isSurvey = checkIsSurvey(geometry, recentLocationStore.get())
        if (!isSurvey && !confirmIsSurvey(requireContext())) {
            setLocked(false)
            return
        }
        withContext(Dispatchers.IO) {
            if (action is UpdateElementTagsAction && !action.changes.isValid()) {
                val questTitle = englishResources.getQuestTitle(osmElementQuestType, element.tags)
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

    companion object {
        private const val ARG_ELEMENT = "element"
        private const val ARG_DISPLAYED_LOCATION = "displayedLocation"
        private const val ARG_MULTI_SELECT_ELEMENTS = "multiSelectElements"

        fun createArguments(element: Element, displayedLocation: Location?) = bundleOf(
            ARG_ELEMENT to Json.encodeToString(element),
            ARG_DISPLAYED_LOCATION to displayedLocation
        )
    }
}
