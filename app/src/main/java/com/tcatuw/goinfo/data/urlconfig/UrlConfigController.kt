package com.tcatuw.goinfo.data.urlconfig

import com.tcatuw.goinfo.data.overlays.OverlayRegistry
import com.tcatuw.goinfo.data.overlays.SelectedOverlayController
import com.tcatuw.goinfo.data.quest.QuestTypeRegistry
import com.tcatuw.goinfo.data.visiblequests.QuestPresetsController
import com.tcatuw.goinfo.data.visiblequests.QuestTypeOrderController
import com.tcatuw.goinfo.data.visiblequests.VisibleQuestTypeController

/** Configure (quest preset, selected overlay) through an URL */
class UrlConfigController(
    private val questTypeRegistry: QuestTypeRegistry,
    private val overlayRegistry: OverlayRegistry,
    private val selectedOverlayController: SelectedOverlayController,
    private val questPresetsController: QuestPresetsController,
    private val visibleQuestTypeController: VisibleQuestTypeController,
    private val questTypeOrderController: QuestTypeOrderController
) {
    fun parse(url: String): UrlConfig? =
        parseConfigUrl(url, questTypeRegistry, overlayRegistry)

    fun apply(config: UrlConfig) {
        val presetId = if (config.presetName != null) {
            val existingPreset = questPresetsController.getByName(config.presetName)
            existingPreset?.id ?: questPresetsController.add(config.presetName)
        } else 0

        val questTypes = questTypeRegistry.associateWith { it in config.questTypes }
        visibleQuestTypeController.setVisibilities(questTypes, presetId)
        questTypeOrderController.setOrders(config.questTypeOrders, presetId)

        // set the current quest preset + overlay last, so the above do not trigger updates
        questPresetsController.selectedId = presetId
        selectedOverlayController.selectedOverlay = config.overlay
    }

    fun create(presetId: Long): String {
        val urlConfig = UrlConfig(
            presetName = questPresetsController.getName(presetId),
            questTypes = visibleQuestTypeController.getVisible(presetId),
            questTypeOrders = questTypeOrderController.getOrders(presetId),
            overlay = selectedOverlayController.selectedOverlay
        )
        return createConfigUrl(urlConfig, questTypeRegistry, overlayRegistry)
    }
}
