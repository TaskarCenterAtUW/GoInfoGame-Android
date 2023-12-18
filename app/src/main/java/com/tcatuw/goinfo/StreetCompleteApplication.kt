package com.tcatuw.goinfo

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.tcatuw.goinfo.data.CacheTrimmer
import com.tcatuw.goinfo.data.CleanerWorker
import com.tcatuw.goinfo.data.Preloader
import com.tcatuw.goinfo.data.dbModule
import com.tcatuw.goinfo.data.download.downloadModule
import com.tcatuw.goinfo.data.download.tiles.DownloadedTilesController
import com.tcatuw.goinfo.data.edithistory.EditHistoryController
import com.tcatuw.goinfo.data.edithistory.editHistoryModule
import com.tcatuw.goinfo.data.maptiles.maptilesModule
import com.tcatuw.goinfo.data.messages.messagesModule
import com.tcatuw.goinfo.data.meta.metadataModule
import com.tcatuw.goinfo.data.osm.created_elements.createdElementsModule
import com.tcatuw.goinfo.data.osm.edits.elementEditsModule
import com.tcatuw.goinfo.data.osm.geometry.elementGeometryModule
import com.tcatuw.goinfo.data.osm.mapdata.mapDataModule
import com.tcatuw.goinfo.data.osm.osmquests.osmQuestModule
import com.tcatuw.goinfo.data.osmApiModule
import com.tcatuw.goinfo.data.osmnotes.edits.noteEditsModule
import com.tcatuw.goinfo.data.osmnotes.notequests.osmNoteQuestModule
import com.tcatuw.goinfo.data.osmnotes.notesModule
import com.tcatuw.goinfo.data.overlays.overlayModule
import com.tcatuw.goinfo.data.quest.questModule
import com.tcatuw.goinfo.data.upload.uploadModule
import com.tcatuw.goinfo.data.urlconfig.urlConfigModule
import com.tcatuw.goinfo.data.user.UserLoginStatusController
import com.tcatuw.goinfo.data.user.achievements.achievementsModule
import com.tcatuw.goinfo.data.user.statistics.statisticsModule
import com.tcatuw.goinfo.data.user.userModule
import com.tcatuw.goinfo.data.visiblequests.questPresetsModule
import com.tcatuw.goinfo.overlays.overlaysModule
import com.tcatuw.goinfo.quests.oneway_suspects.data.trafficFlowSegmentsModule
import com.tcatuw.goinfo.quests.questsModule
import com.tcatuw.goinfo.screens.main.mainModule
import com.tcatuw.goinfo.screens.main.map.mapModule
import com.tcatuw.goinfo.screens.measure.arModule
import com.tcatuw.goinfo.screens.settings.ResurveyIntervalsUpdater
import com.tcatuw.goinfo.screens.settings.settingsModule
import com.tcatuw.goinfo.util.CrashReportExceptionHandler
import com.tcatuw.goinfo.util.getDefaultTheme
import com.tcatuw.goinfo.util.getSelectedLocale
import com.tcatuw.goinfo.util.getSystemLocales
import com.tcatuw.goinfo.util.ktx.addedToFront
import com.tcatuw.goinfo.util.ktx.nowAsEpochMilliseconds
import com.tcatuw.goinfo.util.setDefaultLocales
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin
import java.util.concurrent.TimeUnit

class StreetCompleteApplication : Application() {

    private val preloader: Preloader by inject()
    private val crashReportExceptionHandler: CrashReportExceptionHandler by inject()
    private val resurveyIntervalsUpdater: ResurveyIntervalsUpdater by inject()
    private val downloadedTilesController: DownloadedTilesController by inject()
    private val prefs: SharedPreferences by inject()
    private val editHistoryController: EditHistoryController by inject()
    private val userLoginStatusController: UserLoginStatusController by inject()
    private val cacheTrimmer: CacheTrimmer by inject()

    private val applicationScope = CoroutineScope(SupervisorJob() + CoroutineName("Application"))

    override fun onCreate() {
        super.onCreate()

        deleteDatabase(ApplicationConstants.OLD_DATABASE_NAME)

        startKoin {
            androidContext(this@StreetCompleteApplication)
            workManagerFactory()
            modules(
                achievementsModule,
                appModule,
                createdElementsModule,
                dbModule,
                downloadModule,
                editHistoryModule,
                elementEditsModule,
                elementGeometryModule,
                mapDataModule,
                mapModule,
                mainModule,
                maptilesModule,
                metadataModule,
                noteEditsModule,
                notesModule,
                messagesModule,
                osmApiModule,
                osmNoteQuestModule,
                osmQuestModule,
                questModule,
                questPresetsModule,
                questsModule,
                settingsModule,
                statisticsModule,
                trafficFlowSegmentsModule,
                uploadModule,
                userModule,
                arModule,
                overlaysModule,
                overlayModule,
                urlConfigModule
            )
        }

        /* Force log out users who use the old OAuth consumer key+secret because it does not exist
           anymore. Trying to use that does not result in a "not authorized" API response, but some
           response the app cannot handle */
        if (!prefs.getBoolean(Prefs.OSM_LOGGED_IN_AFTER_OAUTH_FUCKUP, false)) {
            if (userLoginStatusController.isLoggedIn) {
                userLoginStatusController.logOut()
            }
        }

        setDefaultLocales()

        crashReportExceptionHandler.install()

        applicationScope.launch {
            preloader.preload()
            editHistoryController.deleteSyncedOlderThan(nowAsEpochMilliseconds() - ApplicationConstants.MAX_UNDO_HISTORY_AGE)
        }

        enqueuePeriodicCleanupWork()

        setDefaultTheme()

        resurveyIntervalsUpdater.update()

        val lastVersion = prefs.getString(Prefs.LAST_VERSION_DATA, null)
        if (com.tcatuw.goinfo.BuildConfig.VERSION_NAME != lastVersion) {
            prefs.edit { putString(Prefs.LAST_VERSION_DATA, com.tcatuw.goinfo.BuildConfig.VERSION_NAME) }
            if (lastVersion != null) {
                onNewVersion()
            }
        }
    }

    private fun onNewVersion() {
        // on each new version, invalidate quest cache
        downloadedTilesController.invalidateAll()
    }

    override fun onTerminate() {
        super.onTerminate()
        applicationScope.cancel()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE, ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                // very low on memory -> drop caches
                cacheTrimmer.clearCaches()
            }
            ComponentCallbacks2.TRIM_MEMORY_MODERATE, ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                // memory needed, but not critical -> trim only
                cacheTrimmer.trimCaches()
            }
        }
    }

    private fun setDefaultLocales() {
        val locale = getSelectedLocale(this)
        if (locale != null) {
            setDefaultLocales(getSystemLocales().addedToFront(locale))
        }
    }

    private fun setDefaultTheme() {
        val theme = Prefs.Theme.valueOf(prefs.getString(Prefs.THEME_SELECT, getDefaultTheme())!!)
        AppCompatDelegate.setDefaultNightMode(theme.appCompatNightMode)
    }

    private fun enqueuePeriodicCleanupWork() {
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "Cleanup",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequest.Builder(
                CleanerWorker::class.java,
                1, TimeUnit.DAYS,
                1, TimeUnit.DAYS,
            ).setInitialDelay(1, TimeUnit.HOURS).build()
        )
    }
}
