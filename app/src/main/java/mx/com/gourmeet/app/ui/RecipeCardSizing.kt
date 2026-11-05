package mx.com.gourmeet.app.ui

import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnLayout
import kotlin.math.min
import kotlin.math.roundToInt

fun View.fitRecipeCardTo(parent: ViewGroup) {
    parent.doOnLayout {
        val viewport = parent.width - parent.paddingLeft - parent.paddingRight
        if (viewport > 0) {
            val cap = (320 * resources.displayMetrics.density).roundToInt()
            val margins = (layoutParams as? ViewGroup.MarginLayoutParams)?.let { it.leftMargin + it.rightMargin } ?: 0
            layoutParams = layoutParams.apply {
                width = (min(cap, (viewport * 0.9f).roundToInt()) - margins).coerceAtLeast(1)
                height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
        }
    }
}
