package com.tcatuw.goinfo.osm.street_parking

import android.content.Context
import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.osm.street_parking.ParkingPosition.HALF_ON_STREET
import com.tcatuw.goinfo.osm.street_parking.ParkingPosition.OFF_STREET
import com.tcatuw.goinfo.osm.street_parking.ParkingPosition.ON_STREET
import com.tcatuw.goinfo.osm.street_parking.ParkingPosition.PAINTED_AREA_ONLY
import com.tcatuw.goinfo.osm.street_parking.ParkingPosition.STAGGERED_HALF_ON_STREET
import com.tcatuw.goinfo.osm.street_parking.ParkingPosition.STAGGERED_ON_STREET
import com.tcatuw.goinfo.osm.street_parking.ParkingPosition.STREET_SIDE
import com.tcatuw.goinfo.view.DrawableImage
import com.tcatuw.goinfo.view.Image
import com.tcatuw.goinfo.view.ResImage
import com.tcatuw.goinfo.view.ResText
import com.tcatuw.goinfo.view.controller.StreetSideDisplayItem
import com.tcatuw.goinfo.view.controller.StreetSideItem2
import com.tcatuw.goinfo.view.image_select.DisplayItem
import com.tcatuw.goinfo.view.image_select.Item2

/** Functions to display a (parsed) street parking in the UI */

fun StreetParking.asItem(
    context: Context,
    isUpsideDown: Boolean
): DisplayItem<StreetParking> = Item2(
    this,
    getDialogIcon(context, isUpsideDown),
    titleResId?.let { ResText(it) }
)

fun StreetParking.asStreetSideItem(
    context: Context,
    isUpsideDown: Boolean,
    isRightSide: Boolean
): StreetSideDisplayItem<StreetParking> = StreetSideItem2(
    this,
    getIcon(context, isUpsideDown, isRightSide),
    titleResId?.let { ResText(it) },
    getDialogIcon(context, isUpsideDown),
    getFloatingIcon()
)

private val StreetParking.titleResId: Int? get() = when (this) {
    NoStreetParking -> R.string.street_parking_no
    is StreetParkingPositionAndOrientation -> position.titleResId
    StreetParkingSeparate -> R.string.street_parking_separate
    UnknownStreetParking, IncompleteStreetParking -> null
}

/** Image that should be shown in the street side select puzzle */
private fun StreetParking.getIcon(context: Context, isUpsideDown: Boolean, isRightSide: Boolean): Image = when (this) {
    is StreetParkingPositionAndOrientation ->
        getIcon(context, isUpsideDown, isRightSide)
    NoStreetParking, StreetParkingSeparate ->
        ResImage(R.drawable.ic_street_none)
    UnknownStreetParking, IncompleteStreetParking ->
        ResImage(if (isUpsideDown) R.drawable.ic_street_side_unknown_l else R.drawable.ic_street_side_unknown)
}

/** Icon that should be shown as the icon in a selection dialog */
private fun StreetParking.getDialogIcon(context: Context, isUpsideDown: Boolean): Image = when (this) {
    is StreetParkingPositionAndOrientation ->
        getDialogIcon(context, isUpsideDown)
    NoStreetParking ->
        ResImage(R.drawable.ic_floating_no)
    StreetParkingSeparate ->
        ResImage(R.drawable.ic_floating_separate)
    IncompleteStreetParking, UnknownStreetParking ->
        ResImage(if (isUpsideDown) R.drawable.ic_street_side_unknown_l else R.drawable.ic_street_side_unknown)
}

/** Icon that should be shown as the floating icon in the street side select puzzle */
private fun StreetParking.getFloatingIcon(): Image? = when (this) {
    StreetParkingSeparate -> ResImage(R.drawable.ic_floating_separate)
    else -> null
}

fun StreetParkingPositionAndOrientation.asItem(context: Context, isUpsideDown: Boolean) =
    Item2(this, getDialogIcon(context, isUpsideDown), ResText(position.titleResId))

/** An icon for a street parking is square and shows always the same car so it is easier to spot
 *  the variation that matters(on kerb, half on kerb etc) */
private fun StreetParkingPositionAndOrientation.getDialogIcon(context: Context, isUpsideDown: Boolean): Image =
    DrawableImage(StreetParkingDrawable(context, orientation, position, isUpsideDown, 128, 128, R.drawable.ic_car1))

/** An image for a street parking to be displayed shows a wide variety of different cars so that
 *  it looks nicer and/or closer to reality */
private fun StreetParkingPositionAndOrientation.getIcon(context: Context, isUpsideDown: Boolean, isRightSide: Boolean): Image {
    val drawable = StreetParkingDrawable(context, orientation, position, isUpsideDown, 128, 512)
    // show left and right side staggered to each other
    if (isRightSide) drawable.phase = 0.5f
    return DrawableImage(drawable)
}

private val ParkingPosition.titleResId: Int get() = when (this) {
    ON_STREET -> R.string.street_parking_on_street
    HALF_ON_STREET -> R.string.street_parking_half_on_kerb
    OFF_STREET -> R.string.street_parking_on_kerb
    STREET_SIDE -> R.string.street_parking_street_side
    PAINTED_AREA_ONLY, STAGGERED_ON_STREET -> R.string.street_parking_staggered_on_street
    STAGGERED_HALF_ON_STREET -> R.string.street_parking_staggered_half_on_kerb
}

val DISPLAYED_PARKING_POSITIONS: List<ParkingPosition> = listOf(
    ON_STREET,
    HALF_ON_STREET,
    OFF_STREET,
    STREET_SIDE,
    STAGGERED_ON_STREET,
    STAGGERED_HALF_ON_STREET
)
