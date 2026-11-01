package mx.com.gourmeet.app

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Checks real window geometry, including system bars and the configured display cutout. */
@RunWith(AndroidJUnit4::class)
class SafeDrawingTest {
    @Before fun seedLocalSession() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSharedPreferences("user", Context.MODE_PRIVATE).edit()
            .putInt("id", 1).putString("nombre", "Usuario de pruebas")
            .putString("correo", "local@example.invalid").putInt("nivel", 1)
            .putInt("edad", 30).apply()
    }

    @Test fun loginAvoidsSystemObstructions() = checkScreen(Login::class.java, "login")
    @Test fun menuAvoidsSystemObstructions() = checkScreen(Menu_principal_free::class.java, "menu")
    @Test fun pantryAvoidsSystemObstructions() = checkScreen(MiAlacenaActivity::class.java, "pantry")

    @Test fun householdPantryAvoidsSystemObstructions() = checkScreen(AlacenaActivity::class.java, "household-pantry")
    @Test fun homeAvoidsSystemObstructions() = checkScreen(MiHogarActivity::class.java, "home")
    @Test fun plannerAvoidsSystemObstructions() = checkScreen(PlaneadorSemanalActivity::class.java, "planner")
    @Test fun registrationAvoidsSystemObstructions() = checkScreen(Registro::class.java, "registration")

    private fun <A : Activity> checkScreen(type: Class<A>, label: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            instrumentation.targetContext.startActivity(
                Intent(instrumentation.targetContext, type)
                    .putExtra("CLI_ID", 1).putExtra("HOG_ID", 1).putExtra("HOG_USU_ID", 1)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
        }
        try {
            Thread.sleep(1800) // Allow layout, the initial inset dispatch and entrance animations.
            val phase = InstrumentationRegistry.getArguments().getString("phase") ?: "current"
            val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "ui-evidence")
            directory.mkdirs()
            var screenshot: Bitmap? = null
            for (attempt in 0 until 10) {
                val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
                var blueSamples = 0
                for (x in 1..15) for (y in 1..15) {
                    val pixel = bitmap.getPixel(bitmap.width * x / 16, bitmap.height * y / 16)
                    if (android.graphics.Color.blue(pixel) > android.graphics.Color.red(pixel) + 40) blueSamples++
                }
                if (blueSamples >= 2) { screenshot = bitmap; break }
                bitmap.recycle()
                Thread.sleep(500)
            }
            requireNotNull(screenshot) { "$label did not render app content" }.let { bitmap ->
                File(directory, "$phase-$label.png").outputStream().use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
                bitmap.recycle()
            }
            instrumentation.runOnMainSync {
                val activity = ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(Stage.RESUMED).first { type.isInstance(it) }
                val container = activity.findViewById<ViewGroup>(android.R.id.content)
                val root = container.getChildAt(0)
                val insets = requireNotNull(ViewCompat.getRootWindowInsets(activity.window.decorView))
                    .getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
                val position = IntArray(2)
                root.getLocationOnScreen(position)
                val bounds = Rect()
                activity.window.decorView.getWindowVisibleDisplayFrame(bounds)
                val windowHeight = activity.window.decorView.height
                val windowWidth = activity.window.decorView.width
                val padding = listOf(container.paddingLeft, container.paddingTop, container.paddingRight, container.paddingBottom)
                val originalInsets = requireNotNull(ViewCompat.getRootWindowInsets(activity.window.decorView))
                repeat(2) { ViewCompat.dispatchApplyWindowInsets(container, originalInsets) }
                org.junit.Assert.assertEquals("Insets must not accumulate padding", padding,
                    listOf(container.paddingLeft, container.paddingTop, container.paddingRight, container.paddingBottom))
                if (activity is Login) {
                    for (id in listOf(R.id.btnLogin, R.id.btnRegistrar, R.id.btnsin_cuenta)) {
                        val button = activity.findViewById<TextView>(id)
                        val text = requireNotNull(button.layout)
                        assertTrue("Login action text is clipped at this font size",
                            text.getLineBottom(text.lineCount - 1) <=
                                button.height - button.compoundPaddingTop - button.compoundPaddingBottom)
                    }
                }
                val message = "$label: root=(${position[0]},${position[1]},${root.width},${root.height}), " +
                    "window=${windowWidth}x$windowHeight, safe=$insets"
                android.util.Log.i("GOURMEET_UI_TEST", message)
                assertTrue("Top overlaps status bar/camera. $message", position[1] >= insets.top)
                assertTrue("Left overlaps camera/system bar. $message", position[0] >= insets.left)
                assertTrue("Right overlaps system bar. $message", position[0] + root.width <= windowWidth - insets.right)
                assertTrue("Bottom overlaps navigation. $message", position[1] + root.height <= windowHeight - insets.bottom)
            }
        } finally {
            instrumentation.runOnMainSync {
                ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                    .filter { type.isInstance(it) }.forEach { it.finish() }
            }
        }
    }
}
