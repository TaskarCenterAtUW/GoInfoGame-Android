package com.tcatuw.goinfo.screens.measure

import org.koin.dsl.module

val arModule = module {
    factory { ArSupportChecker(get()) }
}
