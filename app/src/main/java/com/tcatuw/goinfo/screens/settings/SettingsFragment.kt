package com.tcatuw.goinfo.screens.settings

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import com.tcatuw.goinfo.ApplicationConstants.DELETE_OLD_DATA_AFTER
import com.tcatuw.goinfo.ApplicationConstants.REFRESH_DATA_AFTER
import com.tcatuw.goinfo.Prefs
import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.download.tiles.DownloadedTilesController
import com.tcatuw.goinfo.data.osm.mapdata.MapDataController
import com.tcatuw.goinfo.data.osm.osmquests.OsmQuestController
import com.tcatuw.goinfo.data.osmnotes.NoteController
import com.tcatuw.goinfo.data.osmnotes.notequests.OsmNoteQuestController
import com.tcatuw.goinfo.data.quest.QuestType
import com.tcatuw.goinfo.data.quest.QuestTypeRegistry
import com.tcatuw.goinfo.data.visiblequests.QuestPreset
import com.tcatuw.goinfo.data.visiblequests.QuestPresetsSource
import com.tcatuw.goinfo.data.visiblequests.VisibleQuestTypeSource
import com.tcatuw.goinfo.databinding.DialogDeleteCacheBinding
import com.tcatuw.goinfo.screens.HasTitle
import com.tcatuw.goinfo.screens.TwoPaneListFragment
import com.tcatuw.goinfo.screens.settings.debug.ShowLinksActivity
import com.tcatuw.goinfo.screens.settings.debug.ShowQuestFormsActivity
import com.tcatuw.goinfo.util.getDefaultTheme
import com.tcatuw.goinfo.util.getSelectedLocales
import com.tcatuw.goinfo.util.ktx.format
import com.tcatuw.goinfo.util.ktx.getYamlObject
import com.tcatuw.goinfo.util.ktx.purge
import com.tcatuw.goinfo.util.ktx.setUpToolbarTitleAndIcon
import com.tcatuw.goinfo.util.ktx.toast
import com.tcatuw.goinfo.util.ktx.viewLifecycleScope
import com.tcatuw.goinfo.util.setDefaultLocales
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import java.util.Locale

/** Shows the settings lists */
class SettingsFragment :
    TwoPaneListFragment(),
    HasTitle,
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val prefs: SharedPreferences by inject()
    private val downloadedTilesController: DownloadedTilesController by inject()
    private val noteController: NoteController by inject()
    private val mapDataController: MapDataController by inject()
    private val osmQuestController: OsmQuestController by inject()
    private val osmNoteQuestController: OsmNoteQuestController by inject()
    private val resurveyIntervalsUpdater: ResurveyIntervalsUpdater by inject()
    private val questTypeRegistry: QuestTypeRegistry by inject()
    private val visibleQuestTypeSource: VisibleQuestTypeSource by inject()
    private val questPresetsSource: QuestPresetsSource by inject()

    override val title: String get() = getString(R.string.action_settings)

    private val visibleQuestTypeListener = object : VisibleQuestTypeSource.Listener {
        override fun onQuestTypeVisibilityChanged(questType: QuestType, visible: Boolean) {
            setQuestPreferenceSummary()
        }

        override fun onQuestTypeVisibilitiesChanged() {
            setQuestPreferenceSummary()
        }
    }

    private val questPresetsListener = object : QuestPresetsSource.Listener {
        override fun onSelectedQuestPresetChanged() {
            setQuestPresetsPreferenceSummary()
        }

        override fun onAddedQuestPreset(preset: QuestPreset) {
            setQuestPresetsPreferenceSummary()
        }

        override fun onRenamedQuestPreset(preset: QuestPreset) {
            setQuestPresetsPreferenceSummary()
        }

        override fun onDeletedQuestPreset(presetId: Long) {
            setQuestPresetsPreferenceSummary()
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        PreferenceManager.setDefaultValues(requireContext(), R.xml.preferences, false)
        addPreferencesFromResource(R.xml.preferences)

        findPreference<NumberPickerPreference>("map.tilecache")?.setSummaryProvider { pref ->
            requireContext().getString(R.string.pref_tilecache_size_summary, (pref as NumberPickerPreference).value)
        }

        findPreference<Preference>("delete_cache")?.setOnPreferenceClickListener {
            val dialogBinding = DialogDeleteCacheBinding.inflate(layoutInflater)
            dialogBinding.descriptionText.text = resources.getString(R.string.delete_cache_dialog_message,
                (1.0 * REFRESH_DATA_AFTER / (24 * 60 * 60 * 1000)).format(Locale.getDefault(), 1),
                (1.0 * DELETE_OLD_DATA_AFTER / (24 * 60 * 60 * 1000)).format(Locale.getDefault(), 1)
            )
            AlertDialog.Builder(requireContext())
                .setView(dialogBinding.root)
                .setPositiveButton(R.string.delete_confirmation) { _, _ -> lifecycleScope.launch { deleteCache() } }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
            true
        }

        findPreference<Preference>("quests.restore.hidden")?.setOnPreferenceClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.restore_dialog_message)
                .setPositiveButton(R.string.restore_confirmation) { _, _ -> lifecycleScope.launch {
                    val hidden = unhideQuests()
                    context?.toast(getString(R.string.restore_hidden_success, hidden), Toast.LENGTH_LONG)
                } }
                .setNegativeButton(android.R.string.cancel, null)
                .show()

            true
        }

        findPreference<Preference>("debug")?.isVisible = com.tcatuw.goinfo.BuildConfig.DEBUG

        findPreference<Preference>("debug.quests")?.setOnPreferenceClickListener {
            startActivity(Intent(context, ShowQuestFormsActivity::class.java))
            true
        }

        findPreference<Preference>("debug.links")?.setOnPreferenceClickListener {
            startActivity(Intent(context, ShowLinksActivity::class.java))
            true
        }

        buildLanguageSelector()
    }

    private fun buildLanguageSelector() {
        val entryValues = resources.getYamlObject<MutableList<String>>(R.raw.languages)
        val entries = entryValues.map {
            val locale = Locale.forLanguageTag(it)
            val name = locale.displayName
            val nativeName = locale.getDisplayName(locale)
            return@map nativeName + if (name != nativeName) " — $name" else ""
        }.toMutableList()

        // add default as first element
        entryValues.add(0, "")
        entries.add(0, getString(R.string.language_default))

        findPreference<ListPreference>("language.select")?.also {
            it.entries = entries.toTypedArray()
            it.entryValues = entryValues.toTypedArray()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpToolbarTitleAndIcon(view.findViewById(R.id.toolbar))
    }

    override fun onStart() {
        super.onStart()

        setQuestPreferenceSummary()
        setQuestPresetsPreferenceSummary()

        visibleQuestTypeSource.addListener(visibleQuestTypeListener)
        questPresetsSource.addListener(questPresetsListener)
    }

    override fun onStop() {
        super.onStop()

        visibleQuestTypeSource.removeListener(visibleQuestTypeListener)
        questPresetsSource.removeListener(questPresetsListener)
    }

    override fun onResume() {
        super.onResume()
        prefs.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        prefs.unregisterOnSharedPreferenceChangeListener(this)
    }

    @SuppressLint("InflateParams")
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when (key) {
            Prefs.AUTOSYNC -> {
                if (Prefs.Autosync.valueOf(prefs.getString(Prefs.AUTOSYNC, "ON")!!) != Prefs.Autosync.ON) {
                    AlertDialog.Builder(requireContext())
                        .setView(layoutInflater.inflate(R.layout.dialog_tutorial_upload, null))
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                }
            }
            Prefs.THEME_SELECT -> {
                val theme = Prefs.Theme.valueOf(prefs.getString(Prefs.THEME_SELECT, getDefaultTheme())!!)
                AppCompatDelegate.setDefaultNightMode(theme.appCompatNightMode)
                activity?.let { ActivityCompat.recreate(it) }
            }
            Prefs.LANGUAGE_SELECT -> {
                setDefaultLocales(getSelectedLocales(requireContext()))
                activity?.let { ActivityCompat.recreate(it) }
            }
            Prefs.RESURVEY_INTERVALS -> {
                resurveyIntervalsUpdater.update()
            }
        }
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference is DialogPreferenceCompat) {
            val fragment = preference.createDialog()
            fragment.arguments = bundleOf("key" to preference.key)
            fragment.setTargetFragment(this, 0)
            fragment.show(parentFragmentManager, "androidx.preference.PreferenceFragment.DIALOG")
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    private suspend fun deleteCache() = withContext(Dispatchers.IO) {
        context?.externalCacheDir?.purge()
        downloadedTilesController.clear()
        mapDataController.clear()
        noteController.clear()
    }

    private suspend fun unhideQuests() = withContext(Dispatchers.IO) {
        osmQuestController.unhideAll() + osmNoteQuestController.unhideAll()
    }

    private fun setQuestPreferenceSummary() {
        val enabledCount = questTypeRegistry.count { visibleQuestTypeSource.isVisible(it) }
        val totalCount = questTypeRegistry.size
        val summary = getString(R.string.pref_subtitle_quests, enabledCount, totalCount)
        viewLifecycleScope.launch {
            findPreference<Preference>("quests")?.summary = summary
        }
    }

    private fun setQuestPresetsPreferenceSummary() {
        val presetName = questPresetsSource.selectedQuestPresetName ?: getString(R.string.quest_presets_default_name)
        val summary = getString(R.string.pref_subtitle_quests_preset_name, presetName)
        viewLifecycleScope.launch {
            findPreference<Preference>("quest_presets")?.summary = summary
        }
    }
}
