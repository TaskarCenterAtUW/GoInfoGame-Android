package com.tcatuw.goinfo.screens.main

import com.tcatuw.goinfo.data.location.RecentLocationStore
import com.tcatuw.goinfo.util.location.LocationAvailabilityReceiver
import org.koin.dsl.module

val mainModule = module {
    single { LocationAvailabilityReceiver(get()) }
    single { RecentLocationStore() }
}
