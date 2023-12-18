package com.tcatuw.goinfo.view.image_select

import com.tcatuw.goinfo.view.Image
import com.tcatuw.goinfo.view.Text

interface DisplayItem<T> {
    val value: T?
    val image: Image?
    val title: Text?
    val description: Text?
}

interface GroupableDisplayItem<T> : DisplayItem<T> {
    val items: List<GroupableDisplayItem<T>>?
    val isGroup: Boolean get() = !items.isNullOrEmpty()
}
