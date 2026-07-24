package de.westnordost.streetcomplete.quests.create_feature

import de.westnordost.streetcomplete.data.edithistory.Edit
import de.westnordost.streetcomplete.data.osm.edits.ElementEdit
import de.westnordost.streetcomplete.data.osm.edits.create.CreateNodeAction
import de.westnordost.streetcomplete.data.quest.QuestType
import de.westnordost.streetcomplete.quests.sidewalk_long_form.AddGenericLong
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.CustomIcon
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.FeaturePreset
import de.westnordost.streetcomplete.view.presetIconIndex
import java.io.File

/** Holds the current workspace's feature presets and custom icons so that UI outside the
 *  create-feature flow (e.g. the edit history, which only has the edit's tags) can resolve
 *  which preset a created node came from and show its icon/name. Populated by
 *  MainActivity.doLongForm() from the workspace's long-form definition. */
class FeaturePresetCatalog {
    var presets: List<FeaturePreset> = emptyList()
        private set
    var customIcons: List<CustomIcon> = emptyList()
        private set

    fun update(presets: List<FeaturePreset>, customIcons: List<CustomIcon>) {
        this.presets = presets
        this.customIcons = customIcons
    }

    /** The preset whose tags are all contained in [tags] (the created node may carry extra tags
     *  like ext:image_url1 added at sync time); ties go to the most specific preset. */
    fun findPresetFor(tags: Map<String, String>): FeaturePreset? =
        presets
            .filter { it.tags.isNotEmpty() && tags.entries.containsAll(it.tags.entries) }
            .maxByOrNull { it.tags.size }

    fun findCustomIconUrl(iconName: String): String? =
        customIcons.firstOrNull { it.name == iconName && it.type == "feature-preset" }?.url

    fun findQuestIconUrl(iconName: String): String? =
        customIcons.firstOrNull { it.name == iconName && it.type == "quest" }?.url

    /** Built-in drawable of the preset matching [tags], or null. The schema uses drawable-style
     *  icon names ("preset_temaki_bench"), presetIconIndex iD-style names ("temaki-bench") -
     *  accept both. */
    fun findPresetIconResId(tags: Map<String, String>): Int? {
        val iconName = findPresetFor(tags)?.icon ?: return null
        return presetIconIndex[iconName]
            ?: presetIconIndex[iconName.removePrefix("preset_").replaceFirst('_', '-')]
    }
}

/** For a node created from a workspace feature preset: the built-in icon of the actual feature
 *  that was added (e.g. the bench). Null for other edits, unknown presets and URL-based custom
 *  icons - callers fall back to the edit type's own icon. */
fun FeaturePresetCatalog.featurePresetIconOf(edit: Edit): Int? {
    if (edit !is ElementEdit || edit.type !is AddFeaturePreset) return null
    val action = edit.action as? CreateNodeAction ?: return null
    return findPresetIconResId(action.tags)
}

/** For a node created from a workspace feature preset whose icon is a URL-based custom icon:
 *  the icon's cached file, or null if this isn't such an edit, a built-in icon applies, or the
 *  icon hasn't been downloaded yet. */
fun FeaturePresetCatalog.featurePresetCustomIconFileOf(edit: Edit, iconCache: CustomIconCache): File? {
    if (edit !is ElementEdit || edit.type !is AddFeaturePreset) return null
    val action = edit.action as? CreateNodeAction ?: return null
    if (findPresetIconResId(action.tags) != null) return null // built-in icon takes precedence
    val iconName = findPresetFor(action.tags)?.icon ?: return null
    val url = findCustomIconUrl(iconName) ?: return null
    return iconCache.getCached(url)
}

/** For a long-form quest (element type) whose element_type_icon is a URL-based custom icon and
 *  isn't a built-in drawable: the icon's cached file (downloading it first if needed - quests have
 *  no picker step to have triggered a download earlier, unlike feature presets), or null. */
suspend fun FeaturePresetCatalog.questCustomIconFileOrNull(questType: QuestType, iconCache: CustomIconCache): File? {
    val iconName = (questType as? AddGenericLong)?.unresolvedIconName ?: return null
    val url = findQuestIconUrl(iconName) ?: return null
    return iconCache.getOrDownload(url)
}

/** Same as [questCustomIconFileOrNull] but never triggers a download - only returns the file if
 *  already cached. Safe to call from non-suspend contexts (e.g. the multi-select long-press
 *  highlight); the quest's base pin normally already triggered caching via
 *  [questCustomIconFileOrNull] by the time this is needed. */
fun FeaturePresetCatalog.cachedQuestCustomIconFileOf(questType: QuestType, iconCache: CustomIconCache): File? {
    val iconName = (questType as? AddGenericLong)?.unresolvedIconName ?: return null
    val url = findQuestIconUrl(iconName) ?: return null
    return iconCache.getCached(url)
}

/** Stable style-image name for a cached custom icon file (the file name is the URL's hash) */
fun customPinIconName(iconFile: File): String = "custom-pin-" + iconFile.name
