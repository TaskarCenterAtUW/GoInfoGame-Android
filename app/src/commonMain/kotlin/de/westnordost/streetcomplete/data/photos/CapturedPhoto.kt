package de.westnordost.streetcomplete.data.photos

import kotlinx.serialization.Serializable

/** A photo taken with the camera and saved locally at [path], not uploaded yet. Each photo has its
 *  own [bearing], the compass direction the camera faced (0-359, clockwise from true north, null
 *  if unknown), and [capturedAt], when it was taken (epoch milliseconds, null if unknown). Both are
 *  sent to Panoramax when it is uploaded. */
@Serializable
data class CapturedPhoto(
    val path: String,
    val bearing: Float? = null,
    val capturedAt: Long? = null,
)
