package de.westnordost.streetcomplete.quests.create_feature

import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.osm.edits.ElementEditType
import de.westnordost.streetcomplete.data.user.achievements.EditTypeAchievement

/** The single edit type under which all nodes created from workspace feature presets are
 *  recorded. Deliberately static (one type for every preset) so that queued, unsynced edits can
 *  always be rehydrated from the database, no matter how the workspace's presets change. */
object AddFeaturePreset : ElementEditType {
    override val icon = R.drawable.ic_pin_new
    override val title = R.string.map_btn_create_node
    override val wikiLink: String? = null
    override val achievements = emptyList<EditTypeAchievement>()
    override val changesetComment = "Add new feature"
    override val visibilityEditable = false
}
