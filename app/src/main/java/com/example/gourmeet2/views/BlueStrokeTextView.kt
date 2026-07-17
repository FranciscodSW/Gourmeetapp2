package com.example.gourmeet2.views
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.example.gourmeet2.R

class BlueStrokeTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatTextView(context, attrs) {

    override fun onDraw(canvas: Canvas) {

        val paint = paint

        // Contorno blanco
        val originalColor = currentTextColor

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f

        setTextColor(
            ContextCompat.getColor(
                context,
                android.R.color.white
            )
        )

        super.onDraw(canvas)

        // Relleno azul
        paint.style = Paint.Style.FILL

        setTextColor(
            ContextCompat.getColor(
                context,
                R.color.azulgourmeet
            )
        )

        super.onDraw(canvas)

        // Restaurar color
        setTextColor(originalColor)
    }
}