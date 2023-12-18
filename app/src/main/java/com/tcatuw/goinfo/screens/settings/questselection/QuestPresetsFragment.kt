package com.tcatuw.goinfo.screens.settings.questselection

import android.os.Bundle
import android.text.InputFilter
import android.view.View
import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.urlconfig.UrlConfigController
import com.tcatuw.goinfo.data.visiblequests.QuestPresetsController
import com.tcatuw.goinfo.data.visiblequests.QuestTypeOrderController
import com.tcatuw.goinfo.data.visiblequests.VisibleQuestTypeController
import com.tcatuw.goinfo.databinding.FragmentQuestPresetsBinding
import com.tcatuw.goinfo.screens.HasTitle
import com.tcatuw.goinfo.screens.TwoPaneDetailFragment
import com.tcatuw.goinfo.util.ktx.viewLifecycleScope
import com.tcatuw.goinfo.util.viewBinding
import com.tcatuw.goinfo.view.dialogs.EditTextDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/** Shows a screen in which the user can select which preset of quest selections he wants to
 *  use. */
class QuestPresetsFragment : TwoPaneDetailFragment(R.layout.fragment_quest_presets), HasTitle {

    private val questPresetsController: QuestPresetsController by inject()
    private val questTypeOrderController: QuestTypeOrderController by inject()
    private val visibleQuestTypeController: VisibleQuestTypeController by inject()
    private val urlConfigController: UrlConfigController by inject()

    private val binding by viewBinding(FragmentQuestPresetsBinding::bind)

    override val title: String get() = getString(R.string.action_manage_presets)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = QuestPresetsAdapter(requireContext(), questPresetsController, questTypeOrderController, visibleQuestTypeController, urlConfigController)
        lifecycle.addObserver(adapter)
        binding.questPresetsList.adapter = adapter
        binding.addPresetButton.setOnClickListener { onClickAddPreset() }
    }

    private fun onClickAddPreset() {
        val ctx = context ?: return
        val dialog = EditTextDialog(ctx,
            title = ctx.getString(R.string.quest_presets_preset_add),
            hint = ctx.getString(R.string.quest_presets_preset_name),
            callback = { name -> addQuestPreset(name) }
        )
        dialog.editText.filters = arrayOf(InputFilter.LengthFilter(60))
        dialog.show()
    }

    private fun addQuestPreset(name: String) {
        viewLifecycleScope.launch(Dispatchers.IO) {
            val newPresetId = questPresetsController.add(name)
            questPresetsController.selectedId = newPresetId
        }
    }
}
