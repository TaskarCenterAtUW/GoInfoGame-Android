package de.westnordost.streetcomplete.view.image_select

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class OutlinedTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : AppCompatTextView(context, attrs, defStyle) {

    private val strokePaint = Paint()

    init {
        // Copy default text paint and modify
        strokePaint.set(paint)
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 4f  // Thickness of the outline
        strokePaint.color = Color.BLACK  // Outline color
        strokePaint.isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        // Draw outline
        val originalTextColor = currentTextColor

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        setTextColor(Color.BLACK)
        super.onDraw(canvas)

        // Draw fill
        paint.style = Paint.Style.FILL
        setTextColor(originalTextColor)
        super.onDraw(canvas)
    }
}

