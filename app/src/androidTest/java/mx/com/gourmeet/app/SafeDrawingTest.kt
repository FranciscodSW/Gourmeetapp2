package mx.com.gourmeet.app

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
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

    @Test fun guestPantryExplainsMissingSession() {
        InstrumentationRegistry.getInstrumentation().targetContext
            .getSharedPreferences("user", Context.MODE_PRIVATE).edit().clear().commit()
        checkScreen(Menu_principal_free::class.java, "guest-pantry", true)
    }

    private fun <A : Activity> checkScreen(type: Class<A>, label: String, guestPantry: Boolean = false) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            instrumentation.targetContext.startActivity(
                Intent(instrumentation.targetContext, type)
                    .putExtra("CLI_ID", 1).putExtra("HOG_ID", 1).putExtra("HOG_USU_ID", 1)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
        }
        try {
            Thread.sleep(1800)
            if (guestPantry) {
                instrumentation.runOnMainSync {
                    val activity = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                        .first { type.isInstance(it) }
                    activity.findViewById<View>(R.id.cardCentro).performClick()
                }
                Thread.sleep(400)
            }
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
                org.junit.Assert.assertNull("Unexpected action bar hides the app header",
                    (activity as androidx.appcompat.app.AppCompatActivity).supportActionBar)
                if (guestPantry) {
                    assertTrue("Guest pantry must explain sign-in", activity.findViewById<View>(R.id.panelSinAlacenas).isShown)
                    org.junit.Assert.assertEquals(activity.getString(R.string.pantry_sign_in_action),
                        activity.findViewById<TextView>(R.id.btnCrearPrimeraAlacena).text.toString())
                }
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
                if (activity is Menu_principal_free && !guestPantry) {
                    for (id in listOf(R.id.txtTitulo, R.id.txtSeccionActual)) {
                        val labelView = root.findViewById<TextView>(id)
                        val labelLayout = requireNotNull(labelView.layout)
                        val availableWidth = labelView.width - labelView.compoundPaddingLeft - labelView.compoundPaddingRight
                        val availableHeight = labelView.height - labelView.compoundPaddingTop - labelView.compoundPaddingBottom
                        assertTrue("Navigation/header text is vertically clipped", labelLayout.getLineBottom(labelLayout.lineCount - 1) <= availableHeight)
                        assertTrue("Navigation/header text exceeds available width", (0 until labelLayout.lineCount).all { labelLayout.getLineWidth(it) <= availableWidth + 1 })
                    }
                    val sectionTitle = root.findViewById<TextView>(R.id.txtCoincidencia)
                    assertTrue("Recipe section title disappears on its light background",
                        androidx.core.graphics.ColorUtils.calculateContrast(sectionTitle.currentTextColor, android.graphics.Color.rgb(245,245,245)) >= 4.5)
                    val carousel = requireNotNull(root.findViewById<RecyclerView>(R.id.rvCoincidencia))
                    assertTrue("Load the recipe-list stress fixtures", (carousel.adapter?.itemCount ?: 0) >= 3)
                    val card = requireNotNull(carousel.getChildAt(0))
                    assertTrue("Recipe card exceeds its list viewport", card.width <= carousel.width)
                    val stats = card.findViewById<ViewGroup>(R.id.cardStats)
                    for (id in listOf(R.id.txtTiempo, R.id.txtNivel, R.id.txtCosto, R.id.txtTipo)) {
                        val textView = card.findViewById<TextView>(id)
                        val textLayout = requireNotNull(textView.layout)
                        assertTrue("Recipe statistic text is clipped",
                            textLayout.getLineBottom(textLayout.lineCount - 1) <=
                                textView.height - textView.compoundPaddingTop - textView.compoundPaddingBottom)
                        val textBounds = Rect()
                        textView.getDrawingRect(textBounds)
                        stats.offsetDescendantRectToMyCoords(textView, textBounds)
                        assertTrue("Recipe statistic is outside its panel", textBounds.top >= 0 && textBounds.bottom <= stats.height)
                    }
                    val title = card.findViewById<TextView>(R.id.txtTitulo)
                    val titleLayout = requireNotNull(title.layout)
                    assertTrue("Long recipe title is truncated", (0 until titleLayout.lineCount).all { titleLayout.getEllipsisCount(it) == 0 })
                    android.util.Log.i("GOURMEET_UI_TEST", "recipe-card width=${card.width}, viewport=${carousel.width}, statsHeight=${stats.height}, titleLines=${titleLayout.lineCount}")
                }
                if (activity is Login) {
                    val privacy = activity.layoutInflater.inflate(R.layout.aceptar_terminos, null) as ViewGroup
                    for (index in 0 until privacy.childCount) {
                        val labelView = privacy.getChildAt(index)
                        if (labelView is TextView && labelView !is android.widget.Button && labelView.text.isNotBlank()) {
                            assertTrue("Privacy text disappears in system dark mode",
                                androidx.core.graphics.ColorUtils.calculateContrast(labelView.currentTextColor, android.graphics.Color.WHITE) >= 4.5)
                        }
                    }
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
