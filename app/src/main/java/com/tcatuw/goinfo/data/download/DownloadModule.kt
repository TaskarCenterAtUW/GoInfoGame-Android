package com.tcatuw.goinfo.data.download

import com.tcatuw.goinfo.data.download.strategy.MobileDataAutoDownloadStrategy
import com.tcatuw.goinfo.data.download.strategy.WifiAutoDownloadStrategy
import com.tcatuw.goinfo.data.download.tiles.DownloadedTilesController
import com.tcatuw.goinfo.data.download.tiles.DownloadedTilesDao
import com.tcatuw.goinfo.data.download.tiles.DownloadedTilesSource
import org.koin.core.qualifier.named
import org.koin.dsl.module

val downloadModule = module {
    factory { DownloadedTilesDao(get()) }
    factory { MobileDataAutoDownloadStrategy(get(), get()) }
    factory { WifiAutoDownloadStrategy(get(), get()) }

    single { Downloader(get(), get(), get(), get(), get(named("SerializeSync"))) }

    single<DownloadProgressSource> { get<DownloadController>() }
    single { DownloadController(get()) }

    single<DownloadedTilesSource> { get<DownloadedTilesController>() }
    single { DownloadedTilesController(get()) }
}
