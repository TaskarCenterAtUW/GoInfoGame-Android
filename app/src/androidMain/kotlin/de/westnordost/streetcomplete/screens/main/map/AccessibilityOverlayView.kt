package de.westnordost.streetcomplete.screens.main.map

import android.content.Context
import android.graphics.PointF
import android.os.Bundle
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.Toast
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.osm.mapdata.LatLon

class AccessibilityOverlayView(
    context: Context,
    val position: LatLon,
    var screenPosition: PointF,
    val key: String,
    properties: Map<String, String>,
    private val onDoubleTap: (Map<String, String>) -> Unit,
    private val onLongPress: (Map<String, String>) -> Unit
) :
    View(context) {

    var description: String = ""

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        isFocusable = true
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
                    isLongClickable = true
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
                return when (action) {
                    AccessibilityNodeInfo.ACTION_CLICK -> {
                        onDoubleTap(properties)
                        true
                    }
                    AccessibilityNodeInfo.ACTION_LONG_CLICK -> {
                        // 🔹 Handle TalkBack "double-tap and hold" here
                        // onLongPress(position, screenPosition, properties)
                        true
                    }
                    else -> super.performAccessibilityAction(host, action, args)
                }
            }
        }

        setOnLongClickListener { // 🔹 Handle regular long-press here
            onLongPress(properties)
            true
        }
    }
}

