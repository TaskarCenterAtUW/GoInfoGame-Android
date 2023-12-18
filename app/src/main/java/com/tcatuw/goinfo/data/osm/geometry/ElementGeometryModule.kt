package com.tcatuw.goinfo.data.osm.geometry

import org.koin.dsl.module

val elementGeometryModule = module {
    factory { ElementGeometryCreator() }
    factory { ElementGeometryDao(get(), get(), get()) }
    factory { WayGeometryDao(get(), get()) }
    factory { RelationGeometryDao(get(), get()) }
    factory { PolylinesSerializer() }
}
