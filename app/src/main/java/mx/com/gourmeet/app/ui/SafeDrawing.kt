package mx.com.gourmeet.app.ui

import android.app.Activity
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnAttach

/**
 * Keeps each activity's content inside the system bars and camera cutout.
 * Installed on the content host so screen-specific keyboard listeners remain intact.
 */
fun Activity.applySafeDrawingInsets() {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    val content = findViewById<View>(android.R.id.content)
    val left = content.paddingLeft
    val top = content.paddingTop
    val right = content.paddingRight
    val bottom = content.paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(content) { view, insets ->
        val safe = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
        // Always derive from the original padding: redispatches must not accumulate.
        view.setPadding(left + safe.left, top + safe.top, right + safe.right, bottom + safe.bottom)
        insets
    }
    content.doOnAttach { ViewCompat.requestApplyInsets(it) }
}
