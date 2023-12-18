package com.tcatuw.goinfo.data.osm.mapdata

import com.tcatuw.goinfo.data.osm.geometry.ElementGeometry
import com.tcatuw.goinfo.data.osm.geometry.ElementPointGeometry

interface MapDataWithGeometry : MapData {
    fun getNodeGeometry(id: Long): ElementPointGeometry?
    fun getWayGeometry(id: Long): ElementGeometry?
    fun getRelationGeometry(id: Long): ElementGeometry?

    fun getGeometry(elementType: ElementType, id: Long): ElementGeometry? = when (elementType) {
        ElementType.NODE -> getNodeGeometry(id)
        ElementType.WAY -> getWayGeometry(id)
        ElementType.RELATION -> getRelationGeometry(id)
    }
}
