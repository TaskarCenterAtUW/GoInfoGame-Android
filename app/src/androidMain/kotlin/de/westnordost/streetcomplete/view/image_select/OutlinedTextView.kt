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

    private var isDrawing = false

    /** setTextColor() - used in onDraw to switch between outline and fill - invalidates the view.
     *  Letting that through scheduled another draw from within every draw, so the view redrew
     *  itself on every frame for as long as it was shown (e.g. image-select tile labels): CPU/GPU
     *  and battery busy while nothing changes (and Espresso never saw the app idle). */
    override fun invalidate() {
        if (!isDrawing) super.invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        isDrawing = true
        try {
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
        } finally {
            isDrawing = false
        }
    }
}

