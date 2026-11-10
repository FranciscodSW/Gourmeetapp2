package mx.com.gourmeet.app

import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.provider.CalendarContract
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import kotlinx.coroutines.runBlocking
import mx.com.gourmeet.app.data.api.ApiClient
import mx.com.gourmeet.app.data.models.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class RecipeInteractionsTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var menu: Menu_principal_free
    private val cleanup = mutableListOf<Int>()

    private fun main(block: () -> Unit) = instrumentation.runOnMainSync(block)
    private fun waitUntil(message: String, predicate: () -> Boolean) {
        repeat(80) {
            var done = false
            main { done = predicate() }
            if (done) return
            Thread.sleep(100)
        }
        fail(message)
    }

    @Before fun openLocalMenu() {
        require(ApiClient.BASE_URL.startsWith("http://127.0.0.1:") || ApiClient.BASE_URL.startsWith("http://10.0.2.2:"))
        instrumentation.targetContext.getSharedPreferences("user", Context.MODE_PRIVATE).edit()
            .putInt("id", 1).putString("nombre", "Usuario de pruebas")
            .putString("correo", "local@example.invalid").putInt("nivel", 1).commit()
        main {
            instrumentation.targetContext.startActivity(Intent(instrumentation.targetContext, Menu_principal_free::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        }
        waitUntil("Menu did not open") {
            ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                .filterIsInstance<Menu_principal_free>().firstOrNull()?.let { menu = it; true } ?: false
        }
        Thread.sleep(1000)
    }

    @After fun close() {
        runBlocking { cleanup.forEach { ApiClient.apiService.eliminarColeccion(EliminarColeccionRequest(it, 1)) } }
        main { if (::menu.isInitialized) menu.finish() }
    }

    private fun openRecipe(): DetalleRecetaFragment {
        main { menu.abrirDetalleReceta(1) }
        waitUntil("Recipe did not load") {
            menu.supportFragmentManager.findFragmentById(R.id.containerDetalleReceta)?.view
                ?.findViewById<TextView>(R.id.txtTitulo)?.text?.toString() == "Arroz de laboratorio"
        }
        lateinit var fragment: DetalleRecetaFragment
        main { fragment = menu.supportFragmentManager.findFragmentById(R.id.containerDetalleReceta) as DetalleRecetaFragment }
        return fragment
    }

    private fun node(id: Int? = null, text: String? = null): AccessibilityNodeInfo {
        val automation = instrumentation.uiAutomation
        automation.serviceInfo = automation.serviceInfo.apply { flags = flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS }
        repeat(80) {
            val root = automation.rootInActiveWindow
            val candidates = if (id != null) root?.findAccessibilityNodeInfosByViewId(instrumentation.targetContext.resources.getResourceName(id))
                else root?.findAccessibilityNodeInfosByText(requireNotNull(text))
            val found = candidates?.firstOrNull { it.isEnabled && (text == null || it.text?.toString() == text) }
            if (found != null) return found
            Thread.sleep(100)
        }
        error("Missing accessible control: id=$id text=$text")
    }

    private fun capture(label: String) {
        val phase = InstrumentationRegistry.getArguments().getString("phase") ?: "recipe-review"
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "ui-evidence").apply { mkdirs() }
        requireNotNull(instrumentation.uiAutomation.takeScreenshot()).let { bitmap ->
            File(directory, "$phase-$label.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }

    @Test fun detailHasReadableContentAndActions() {
        val fragment = openRecipe()
        main {
            val root = fragment.requireView()
            val title = root.findViewById<TextView>(R.id.txtTitulo)
            assertFalse(title is mx.com.gourmeet.app.views.StrokeTextView)
            assertTrue(androidx.core.graphics.ColorUtils.calculateContrast(title.currentTextColor, android.graphics.Color.WHITE) >= 4.5)
            assertEquals(0, root.findViewById<View>(R.id.txtCoincidencia).visibility.let { if (it == View.GONE) 0 else 1 })
            for (id in listOf(R.id.layoutCompartir, R.id.layoutPlaneador, R.id.layoutColeccion, R.id.layoutFavoritos)) {
                val action = root.findViewById<View>(id)
                assertTrue("Recipe action has no click listener", action.hasOnClickListeners())
                assertTrue("Recipe action has no accessible label", !action.contentDescription.isNullOrBlank())
            }
        }
        capture("detail-top")
        main {
            val action = fragment.requireView().findViewById<View>(R.id.cardMasFunciones)
            val scroll = fragment.requireView() as android.widget.ScrollView
            val rect = Rect(); action.getDrawingRect(rect); scroll.offsetDescendantRectToMyCoords(action, rect)
            scroll.scrollTo(0, rect.top)
        }
        Thread.sleep(200)
        capture("detail-actions")
    }

    @Test fun unavailableRecipeExplainsHowToRetry() {
        main { menu.abrirDetalleReceta(2_000_000_000) }
        waitUntil("Missing recipe must explain the failure") {
            menu.findViewById<TextView>(R.id.recipeLoadStatus)?.text?.contains("No pudimos cargar") == true
        }
        main {
            val status = menu.findViewById<View>(R.id.recipeLoadStatus)
            assertTrue(status.isEnabled && status.hasOnClickListeners())
            assertFalse(menu.findViewById<View>(R.id.layoutCompartir).isEnabled)
            assertFalse(menu.findViewById<View>(R.id.layoutPlaneador).isEnabled)
        }
    }

    @Test fun keyboardSearchTouchesTheImeAndRestoresNavigation() {
        lateinit var input: EditText
        main {
            input = menu.findViewById(R.id.editBusqueda)
            input.requestFocus()
            (menu.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
        }
        waitUntil("Keyboard did not open") {
            ViewCompat.getRootWindowInsets(menu.window.decorView)?.isVisible(WindowInsetsCompat.Type.ime()) == true
        }
        Thread.sleep(350)
        main {
            val decor = menu.window.decorView
            val ime = requireNotNull(ViewCompat.getRootWindowInsets(decor)).getInsets(WindowInsetsCompat.Type.ime()).bottom
            val search = menu.findViewById<View>(R.id.panelBusqueda)
            val location = IntArray(2); search.getLocationOnScreen(location)
            val topOfIme = decor.height - ime
            val gap = topOfIme - location[1] - search.height
            assertTrue("Search overlaps keyboard: gap=$gap", gap >= -2)
            assertTrue("Search floats above keyboard: gap=$gap", gap <= (16 * menu.resources.displayMetrics.density).toInt())
            assertEquals(0f, search.translationY, 0.01f)
            assertEquals(View.GONE, menu.findViewById<View>(R.id.barraInferior).visibility)
        }
        capture("keyboard")
        main { (menu.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(input.windowToken, 0); input.clearFocus() }
        waitUntil("Navigation did not return after hiding keyboard") { menu.findViewById<View>(R.id.barraInferior).visibility == View.VISIBLE }
        main { assertEquals(0f, menu.findViewById<View>(R.id.panelBusqueda).translationY, 0.01f) }
    }

    @Test fun favoritesPersistAndCanBeRemoved() {
        val initial = runBlocking { ApiClient.apiService.verificarFavorito(VerificarFavoritoRequest(1, 1)).guardada }
        val fragment = openRecipe()
        val action = fragment.requireView().findViewById<View>(R.id.layoutFavoritos)
        waitUntil("Favorite state did not load") { action.isEnabled && action.isSelected == initial }
        try {
            main { action.performClick() }
            waitUntil("Favorite toggle did not finish") { action.isEnabled && action.isSelected != initial }
            assertEquals(!initial, runBlocking { ApiClient.apiService.verificarFavorito(VerificarFavoritoRequest(1, 1)).guardada })
            main { menu.supportFragmentManager.popBackStackImmediate() }
            val reopened = openRecipe().requireView().findViewById<View>(R.id.layoutFavoritos)
            waitUntil("Favorite did not persist on reopening") { reopened.isSelected != initial }
            main { reopened.performClick() }
            waitUntil("Removing favorite did not finish") { reopened.isEnabled && reopened.isSelected == initial }
            assertEquals(initial, runBlocking { ApiClient.apiService.verificarFavorito(VerificarFavoritoRequest(1, 1)).guardada })
        } finally {
            runBlocking {
                if (ApiClient.apiService.verificarFavorito(VerificarFavoritoRequest(1, 1)).guardada != initial)
                    ApiClient.apiService.gestionarFavorito(GestionarFavoritoRequest(1, 1))
            }
        }
    }

    @Test fun collectionCreationAndMembershipPersist() {
        val fragment = openRecipe()
        main { fragment.requireView().findViewById<View>(R.id.layoutColeccion).performClick() }
        val name = "Prueba UI ${System.currentTimeMillis()}"
        assertTrue(node(id = R.id.recipeCreateCollection).performAction(AccessibilityNodeInfo.ACTION_CLICK))
        assertTrue(node(id = R.id.recipeCollectionName).performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,
            Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, name) }))
        assertTrue(node(text = "Crear y guardar").performAction(AccessibilityNodeInfo.ACTION_CLICK))
        var id: Int? = null
        repeat(50) {
            if (id == null) {
                id = runBlocking { ApiClient.apiService.listarColeccionesConRecetas(ListarColeccionesRecetasRequest(1)).colecciones.firstOrNull { it.COL_NOMBRE == name }?.COL_ID }
                if (id == null) Thread.sleep(100)
            }
        }
        val created = requireNotNull(id); cleanup.add(created)
        fun stored() = runBlocking { ApiClient.apiService.listarColeccionesConRecetas(ListarColeccionesRecetasRequest(1)).colecciones.first { it.COL_ID == created }.RECETAS.any { it.REC_ID == 1 } }
        fun waitForMembership(expected: Boolean) {
            repeat(50) {
                if (stored() == expected) return
                Thread.sleep(100)
            }
            fail("Collection membership did not persist")
        }
        assertTrue(stored())
        Thread.sleep(250)
        assertTrue(node(text = "$name · Guardada").performAction(AccessibilityNodeInfo.ACTION_CLICK))
        waitForMembership(false)
        Thread.sleep(250)
        assertTrue(node(text = name).performAction(AccessibilityNodeInfo.ACTION_CLICK))
        waitForMembership(true)
        Thread.sleep(250)
        assertNotNull(node(text = "$name · Guardada"))
        capture("collections")
        instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        main { menu.supportFragmentManager.popBackStackImmediate() }
        val reopened = openRecipe()
        main { reopened.requireView().findViewById<View>(R.id.layoutColeccion).performClick() }
        Thread.sleep(500)
        assertNotNull(node(text = "$name · Guardada"))
    }

    @Test fun shareAndCalendarPrepareEditableIntents() {
        val fragment = openRecipe()
        val captured = AtomicReference<Intent>()
        val monitor = object : Instrumentation.ActivityMonitor() {
            override fun onStartActivity(intent: Intent): Instrumentation.ActivityResult? {
                if (intent.action == Intent.ACTION_CHOOSER || intent.action == Intent.ACTION_INSERT) {
                    captured.set(intent)
                    return Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null)
                }
                return null
            }
        }
        instrumentation.addMonitor(monitor)
        try {
            main { fragment.requireView().findViewById<View>(R.id.layoutCompartir).performClick() }
            val chooser = requireNotNull(captured.get())
            assertEquals(Intent.ACTION_CHOOSER, chooser.action)
            val send = requireNotNull(chooser.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java))
            assertEquals(Intent.ACTION_SEND, send.action)
            assertEquals("text/plain", send.type)
            assertTrue(send.getStringExtra(Intent.EXTRA_TEXT)!!.contains("Arroz de laboratorio"))
            captured.set(null)
            main { fragment.requireView().findViewById<View>(R.id.layoutPlaneador).performClick() }
            val calendar = requireNotNull(captured.get())
            assertEquals(CalendarContract.Events.CONTENT_URI, calendar.data)
            assertEquals("vnd.android.cursor.dir/event", calendar.type)
            assertTrue(calendar.getStringExtra(CalendarContract.Events.TITLE)!!.contains("Arroz de laboratorio"))
            assertEquals(20 * 60_000L, calendar.getLongExtra(CalendarContract.EXTRA_EVENT_END_TIME, 0) - calendar.getLongExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, 0))
        } finally {
            instrumentation.removeMonitor(monitor)
        }
    }
}
