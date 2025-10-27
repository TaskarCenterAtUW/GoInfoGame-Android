package de.westnordost.streetcomplete.screens.main.map

import android.content.Context
import android.graphics.PointF
import android.os.Bundle
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.osm.mapdata.LatLon

class AccessibilityOverlayView(
    context: Context,
    val position: LatLon,
    var screenPosition : PointF,
    val key : String,
    private val onDoubleTap: (LatLon, PointF) -> Unit,
) :
    View(context) {

    var description: String = ""

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        isFocusable = true
        isClickable = true
        isLongClickable = true
        accessibilityDelegate = object : AccessibilityDelegate() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfo
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.apply {
                    className = Button::class.java.name
                    contentDescription = "Pin: $description"
                    isClickable = true
                    addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK)
                    addAction(
                        AccessibilityNodeInfo.AccessibilityAction(
                            AccessibilityNodeInfo.ACTION_LONG_CLICK,
                            context.getString(R.string.accessibility_action_long_press)
                        )
                    )
                }
            }

            override fun performAccessibilityAction(
                host: View,
                action: Int,
                args: Bundle?
            ): Boolean {
                if (action == AccessibilityNodeInfo.ACTION_CLICK) {
                    onDoubleTap(position, screenPosition)
                    return true
                }
                return super.performAccessibilityAction(host, action, args)
            }
        }
    }
}

