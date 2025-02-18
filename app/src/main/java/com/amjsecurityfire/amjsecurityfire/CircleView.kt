package com.amjsecurityfire.amjsecurityfire;

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

public class CircleView @JvmOverloads constructor(
    context: Context,
    private val borderColor: Int,
    attrs: AttributeSet? = null
            ) : View(context, attrs) {

        private val paint = Paint().apply {
            isAntiAlias = true
            color = borderColor
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val radius = (width.coerceAtMost(height)) / 2f
            val centerX = width / 2f
            val centerY = height / 2f
            canvas.drawCircle(centerX, centerY, radius, paint) // Canvas não deve ser opcional
        }
}
