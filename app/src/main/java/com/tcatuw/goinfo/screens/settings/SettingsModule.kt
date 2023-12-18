package com.tcatuw.goinfo.screens.settings

import org.koin.dsl.module

val settingsModule = module {
    single { ResurveyIntervalsUpdater(get()) }
}
