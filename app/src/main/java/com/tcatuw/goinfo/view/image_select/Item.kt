package com.tcatuw.goinfo.view.image_select

import com.tcatuw.goinfo.view.Image
import com.tcatuw.goinfo.view.ResImage
import com.tcatuw.goinfo.view.ResText
import com.tcatuw.goinfo.view.Text

data class Item<T>(
    override val value: T?,
    val drawableId: Int? = null,
    val titleId: Int? = null,
    val descriptionId: Int? = null,
    override val items: List<GroupableDisplayItem<T>>? = null
) : GroupableDisplayItem<T> {

    override val image: Image? get() = drawableId?.let { ResImage(it) }
    override val title: Text? get() = titleId?.let { ResText(titleId) }
    override val description: Text? get() = descriptionId?.let { ResText(descriptionId) }
}

data class Item2<T>(
    override val value: T?,
    override val image: Image? = null,
    override val title: Text? = null,
    override val description: Text? = null
) : DisplayItem<T>
