package com.example.gourmeet2.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class StrokeTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatTextView(context, attrs) {

    override fun onDraw(canvas: Canvas) {

        val originalColor = currentTextColor

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        setTextColor(Color.parseColor("#177AFF"))
        super.onDraw(canvas)

        paint.style = Paint.Style.FILL
        setTextColor(originalColor)
        super.onDraw(canvas)
    }
}