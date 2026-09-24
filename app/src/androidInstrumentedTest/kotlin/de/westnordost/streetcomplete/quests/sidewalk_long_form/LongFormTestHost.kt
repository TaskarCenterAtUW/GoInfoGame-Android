package de.westnordost.streetcomplete.quests.sidewalk_long_form

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import de.westnordost.streetcomplete.data.osm.edits.AddElementEditsController
import de.westnordost.streetcomplete.data.osm.edits.ElementEditAction
import de.westnordost.streetcomplete.data.osm.edits.ElementEditType
import de.westnordost.streetcomplete.data.osm.edits.update_tags.UpdateElementTagsAction
import de.westnordost.streetcomplete.data.osm.geometry.ElementGeometry
import de.westnordost.streetcomplete.data.osm.geometry.ElementPolylinesGeometry
import de.westnordost.streetcomplete.data.osm.mapdata.Element
import de.westnordost.streetcomplete.data.osm.mapdata.Node
import de.westnordost.streetcomplete.data.osm.mapdata.Way
import de.westnordost.streetcomplete.data.quest.Quest
import de.westnordost.streetcomplete.data.quest.QuestKey
import de.westnordost.streetcomplete.quests.AbstractOsmQuestForm
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Stands in for MainActivity as the [AbstractOsmQuestForm.Listener] of a quest form under test -
 * the form finds its listener via `parentFragment`, so it is added as a child of this fragment.
 * Same idea as the debug "Show Quest Forms" screen in SettingsActivity, minus the map.
 */
class LongFormTestHost : Fragment(), AbstractOsmQuestForm.Listener {

    val containerId = View.generateViewId()

    @Volatile var editedCount = 0
        private set
    @Volatile var closed = false
        private set

    override val displayedMapLocation: Location? = null
    override val mutableMultiSelectQuests: MutableList<Quest> = mutableListOf()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FrameLayout(requireContext()).apply { id = containerId }

    override fun onEdited(editType: ElementEditType, geometry: ElementGeometry) { editedCount++ }
    override fun onCloseDialog() { closed = true }
    override fun onComposeNote(editType: ElementEditType, element: Element, geometry: ElementGeometry, leaveNoteContext: String) {}
    override fun onSplitWay(editType: ElementEditType, way: Way, geometry: ElementPolylinesGeometry) {}
    override fun onMoveNode(editType: ElementEditType, node: Node) {}
    override fun onQuestHidden(questKey: QuestKey) {}
}

/** Records edits instead of queueing them for upload, so tests can assert the exact tag changes
 *  a submit produced. */
class RecordingEditsController : AddElementEditsController {
    val actions = CopyOnWriteArrayList<ElementEditAction>()

    override fun add(
        type: ElementEditType,
        geometry: ElementGeometry,
        source: String,
        action: ElementEditAction,
        isNearUserLocation: Boolean,
    ): Long {
        actions.add(action)
        return FIRST_EDIT_ID + actions.size
    }

    /** The element's tags after applying the single recorded tag edit. */
    fun resultingTags(original: Map<String, String>): Map<String, String> {
        val action = actions.single() as UpdateElementTagsAction
        return original.toMutableMap().also { action.changes.applyTo(it) }
    }

    companion object {
        /** Well clear of real edit ids, since an attached photo is stored under this id */
        const val FIRST_EDIT_ID = 9_000_000L
    }
}
