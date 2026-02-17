package com.example.gourmeet2
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.gourmeet2.data.api.ApiClient
import com.example.gourmeet2.data.models.*
import com.example.gourmeet2.databinding.ActivityMenuPrincipalFreeBinding
import com.example.gourmeet2.databinding.ItemDelCarruselBinding
import com.example.gourmeet2.databinding.ItemRecetaBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.sorted


class Menu_principal_free : AppCompatActivity() {

    private lateinit var binding: ActivityMenuPrincipalFreeBinding
    private lateinit var recetaAdapter: RecetaAdapter
    private val recetasList = mutableListOf<RecetaRecrcid>()

    private val recetasBusquedaList = mutableListOf<RecetaBuscar>()
    private val categoriasList = mutableListOf<Categoria>()
    private var categoriaActualId: Int = -1
    private var searchJob: Job? = null
    private var isSearching: Boolean = false
    private var lastQuery: String = ""
    private var ingredientesSeleccionados = mutableListOf<BuscarIngredientes>()
    private var autoCompleteAdapter: ArrayAdapter<BuscarIngredientes>? = null
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuPrincipalFreeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        bottomNavigationView = binding.bottomNavigation
        // Configurar el listener para el BottomNavigationView
        setupBottomNavigation()
        setupDraggableIngredientes()
        setupBusquedaIngredientes()
        // Mostrar la pantalla de Recetas por defecto (que es tu diseño actual)
        mostrarPantallaRecetas()
    }
    private fun setupBottomNavigation() {
        bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_search_ingredients -> {
                    // Cambiar a pantalla de buscar por ingredientes
                    mostrarPantallaIngredientes()
                    true
                }
                R.id.nav_shopping_planner -> {
                    // Cambiar a pantalla del planeador de compras
                    mostrarPantallaPlaneador()
                    true
                }
                R.id.nav_search_recipes -> {
                    // Cambiar a pantalla de buscar recetas (tu diseño actual)
                    mostrarPantallaRecetas()
                    true
                }
                else -> false
            }
        }

        // Seleccionar "Recetas" por defecto
        bottomNavigationView.selectedItemId = R.id.nav_search_recipes
    }
    enum class PantallaActual {
        RECETAS,
        INGREDIENTES,
        PLANEADOR
    }
    private fun mostrarPantallaPlaneador() {
        // Ocultar el contenido actual de recetas
        binding.contentContainer.visibility = View.GONE
        // ❌ Ingredientes fuera
        binding.chipGroupIngredientes.visibility = View.GONE
        binding.containerIngredientes.visibility = View.GONE

    }
    private fun mostrarPantallaRecetas() {
        pantallaActual = PantallaActual.RECETAS
        binding.contentContainer.visibility = View.VISIBLE
        // ✅ MOSTRAR CONTENEDOR COMPLETO
        binding.cardSearch.visibility = View.VISIBLE
        binding.editTextSearch.visibility = View.VISIBLE
        binding.editTextSearch.hint = "Ingresar receta"
        binding.txtDescripcion.text = "Explora recetas"
        // ❌ Ingredientes fuera
        binding.chipGroupIngredientes.visibility = View.GONE
        binding.containerIngredientes.visibility = View.GONE

        if (!::recetaAdapter.isInitialized) {
            setupRecyclerView()
        }
        setupSearch()
        if (categoriasList.isEmpty()) {
            cargarCategorias()
        } else {
            setupCarrusel()
            if (categoriaActualId > 0) {
                cargarRecetasPorCategoria(categoriaActualId)
            }
        }
        val currentQuery = binding.editTextSearch.text.toString().trim()
        if (currentQuery.isNotEmpty() && categoriaActualId > 0) {
            buscarRecetasPorNombre(currentQuery, categoriaActualId)
        }
    }
    // ============================================================
// VARIABLES DE CLASE (agrega estas al inicio de tu activity)
// ============================================================

    private var carruselCallback: ViewPager2.OnPageChangeCallback? = null
    private var ultimaCategoriaCargada = -1
    private var ultimaCategoriaIngredientes = -1
    private var cargandoEnProgreso = false
    private var lastIngredientesQuery = ""
    private var lastCategoriaBusqueda = -1
    private var modoActual = ModoBusqueda.CATEGORIA
    private var pantallaActual = PantallaActual.RECETAS
    private var carruselInicializado = false
    private var primeraSeleccionCarrusel = true
    private var categoriaPendiente = -1

// ============================================================
// ENUMS
// ============================================================

    enum class ModoBusqueda {
        CATEGORIA,
        INGREDIENTES
    }



// ============================================================
// MOSTRAR PANTALLA DE INGREDIENTES
// ============================================================

    private fun mostrarPantallaIngredientes() {
        pantallaActual = PantallaActual.INGREDIENTES

        // Mostrar vistas necesarias
        binding.contentContainer.visibility = View.VISIBLE
        binding.containerIngredientes.visibility = View.VISIBLE
        binding.chipGroupIngredientes.visibility = View.VISIBLE
        binding.viewPagerCarrusel.visibility = View.VISIBLE

        // Ajustar posición del panel
        binding.containerIngredientes.post {
            val panel = binding.containerIngredientes
            val rawMaxUp = -panel.height.toFloat() + 120.dpToPx()
            val maxUp = minOf(rawMaxUp, 0f)
            panel.translationY = maxUp
        }
        Log.d("FLOW_TRACE",
            "mostrarModoIngredientes() ← llamada desde mostrarPantallaIngredientes()"
        )
        mostrarModoIngredientes()

        // Ocultar búsqueda principal
        binding.editTextSearch.visibility = View.GONE
        binding.cardSearch.visibility = View.GONE
        binding.editTextSearch.text.clear()

        // Limpiar ingredientes
        binding.chipGroupIngredientes.removeAllViews()
        ingredientesSeleccionados.clear()
        // Cargar categorías si es necesario
        if (categoriasList.isEmpty()) {
            Log.d("FLOW_TRACE",
                "cargarCategoriasingredientes() ← llamada desde mostrarPantallaIngredientes()"
            )
            cargarCategoriasingredientes()
        } else {
            Log.d("FLOW_TRACE",
                "setupCarruselIngredientes() ← llamada desde mostrarPantallaIngredientes()"
            )
            setupCarruselIngredientes()
            categoriaActualId = categoriasList[0].id
        }
    }

// ============================================================
// SETUP CARRUSEL DE INGREDIENTES (CORREGIDO)
// ============================================================

    private fun setupCarruselIngredientes() {

        if (categoriasList.isEmpty()) return

        val adapter = CarruselAdapter(categoriasList) { position ->
            binding.viewPagerCarrusel.currentItem = position
        }

        binding.viewPagerCarrusel.adapter = adapter

        carruselCallback?.let {
            binding.viewPagerCarrusel.unregisterOnPageChangeCallback(it)
        }

        carruselCallback = object : ViewPager2.OnPageChangeCallback() {

            override fun onPageSelected(position: Int) {

                if (pantallaActual != PantallaActual.INGREDIENTES) return

                val categoria = categoriasList[position]

                categoriaActualId = categoria.id

                Log.d("FLOW_TRACE", "INGREDIENTES → Categoria ${categoria.descripcion}")

                val ingredientes = obtenerIngredientesSeleccionados()

                // ✅ FIX CLAVE
                if (ingredientes.isEmpty()) {

                    Log.d("FLOW_TRACE", "Sin ingredientes → cargar normal")

                    cargarRecetasPorCategoria(categoria.id)   // 🔥 ESTA ES LA SOLUCIÓN
                    binding.txtDescripcion.text = "Mostrando ${categoria.descripcion}"

                    return
                }

                Log.d("FLOW_TRACE", "Con ingredientes → búsqueda")

                buscarRecetasPorIngredientes(ingredientes, categoria.id)
                binding.txtDescripcion.text = "Filtrando ${categoria.descripcion}"
            }
        }

        binding.viewPagerCarrusel.registerOnPageChangeCallback(carruselCallback!!)
    }




    private fun obtenerIngredientesSeleccionados(): List<String> {
        return ingredientesSeleccionados.map { it.descripcion }
    }

    // ============================================================
// CARGAR RECETAS POR CATEGORÍA (RENOMBRADO)
// ============================================================
private fun ejecutarBusqueda() {

    if (categoriaActualId <= 0) return
    if (cargandoEnProgreso) return

    cargandoEnProgreso = true

    if (ingredientesSeleccionados.isNotEmpty()) {

        Log.d("FLOW", "→ BUSQUEDA POR INGREDIENTES")
        Log.d("FLOW_TRACE",
            "buscarRecetasPorIngredientes() ← ejecutarBusqueda()"
        )
        buscarRecetasPorIngredientes(
            obtenerIngredientesTexto(),
            categoriaActualId
        )

    } else {

        Log.d("FLOW", "→ CARGA POR CATEGORIA")
        Log.d("FLOW_TRACE",
            "cargarRecetasPorCategoriaIngredientes() ← ejecutarBusqueda()"
        )
        cargarRecetasPorCategoriaIngredientes(categoriaActualId)
    }
}

    private fun cargarRecetasPorCategoriaIngredientes(categoriaId: Int) {
        // 🔥 PREVENIR LLAMADAS DUPLICADAS
        if (categoriaId == ultimaCategoriaCargada && !cargandoEnProgreso) {
            Log.d("API_DEBUG", "🛑 Categoría $categoriaId ya cargada, ignorando")
            return
        }

        if (cargandoEnProgreso) {
            Log.d("API_DEBUG", "⏳ Carga en progreso para categoría $categoriaId, ignorando")
            return
        }

        Log.d("API_DEBUG", "=== CARGANDO RECETAS POR CATEGORÍA $categoriaId (INGREDIENTES) ===")
        ultimaCategoriaCargada = categoriaId
        cargandoEnProgreso = true
        modoActual = ModoBusqueda.CATEGORIA

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.getRecetasPorCategoria(categoriaId)

                withContext(Dispatchers.Main) {
                    if (modoActual != ModoBusqueda.CATEGORIA) {
                        Log.d("API_DEBUG", "Modo cambiado, ignorando respuesta")
                        cargandoEnProgreso = false

                        // Si hay categoría pendiente, cargarla
                        if (categoriaPendiente != -1) {
                            val pendiente = categoriaPendiente
                            categoriaPendiente = -1
                            if (ingredientesSeleccionados.isEmpty()) {
                                Log.d("FLOW_TRACE",
                                    " cargarRecetasPorCategoriaIngredientes ← cargarRecetasPorCategoriaIngredientes()"
                                )
                                cargarRecetasPorCategoriaIngredientes(pendiente)
                            } else {
                                Log.d("FLOW_TRACE",
                                    " buscarRecetasPorIngredientes ← cargarRecetasPorCategoriaIngredientes()"
                                )
                                buscarRecetasPorIngredientes(obtenerIngredientesTexto(), pendiente)
                            }
                        }
                        return@withContext
                    }

                    if (response.success) {
                        val recetasConvertidas = response.recetas.map { receta ->
                            RecetaRecrcid(
                                id = receta.id,
                                nombre = receta.nombre ?: "Sin nombre",
                                descripcion = receta.descripcion ?: "Sin descripción",
                                tiempoPreparacion = receta.tiempoPreparacion ?: "No especificado",
                                porciones = receta.porciones ?: 0,
                                fechaCreacion = receta.fechaCreacion ?: "",
                                dificultad = receta.dificultad,
                                calorias = receta.calorias,
                                enlaceYoutube = receta.enlaceYoutube,
                                recrcid = receta.recrcid
                            )
                        }

                        recetasList.clear()
                        recetasList.addAll(recetasConvertidas)
                        recetaAdapter.updateList(recetasList)

                        binding.txtDescripcion.text = when {
                            response.count > 0 -> "${response.count} recetas encontradas"
                            else -> "No se encontraron recetas"
                        }

                        Log.d("API_DEBUG", "Mostrando ${response.count} recetas en UI")
                    }
                    cargandoEnProgreso = false

                    // Si hay categoría pendiente, cargarla
                    if (categoriaPendiente != -1) {
                        val pendiente = categoriaPendiente
                        categoriaPendiente = -1
                        if (ingredientesSeleccionados.isEmpty()) {
                            cargarRecetasPorCategoriaIngredientes(pendiente)
                        } else {
                            buscarRecetasPorIngredientes(obtenerIngredientesTexto(), pendiente)
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("API_ERROR", "Error cargando categoría: ${e.message}")
                    binding.txtDescripcion.text = "Error al cargar recetas"
                    cargandoEnProgreso = false

                    // Si hay categoría pendiente, cargarla
                    if (categoriaPendiente != -1) {
                        val pendiente = categoriaPendiente
                        categoriaPendiente = -1
                        if (ingredientesSeleccionados.isEmpty()) {
                            Log.d("FLOW_TRACE",
                                " cargarRecetasPorCategoriaIngredientes ← cargarRecetasPorCategoriaIngredientes()"
                            )
                            cargarRecetasPorCategoriaIngredientes(pendiente)
                        } else {
                            Log.d("FLOW_TRACE",
                                " buscarRecetasPorIngredientes( ← cargarRecetasPorCategoriaIngredientes()"
                            )
                            buscarRecetasPorIngredientes(obtenerIngredientesTexto(), pendiente)
                        }
                    }
                }
            }
        }
    }

// ============================================================
// BUSCAR RECETAS POR INGREDIENTES (CORREGIDO)
// ============================================================

    private fun buscarRecetasPorIngredientes(
        ingredientes: List<String>,
        categoriaId: Int
    ) {

        if (ingredientes.isEmpty()) return   // 🔥 Protección extra

        val termino = ingredientes.joinToString(",")

        modoActual = ModoBusqueda.INGREDIENTES
        cargandoEnProgreso = true

        Log.d("INGREDIENTES", "Buscando → $termino en categoría $categoriaId")

        CoroutineScope(Dispatchers.IO).launch {

            try {

                val response = ApiClient.apiService
                    .getRecetasPorIngredientes(termino, categoriaId)

                withContext(Dispatchers.Main) {

                    val recetasConvertidas = response.recetas.map { receta ->

                        RecetaRecrcid(
                            id = receta.id,
                            nombre = receta.nombre,
                            descripcion = receta.descripcion,
                            tiempoPreparacion = receta.tiempoPreparacion,
                            porciones = receta.porciones.toIntOrNull() ?: 0, // 🔥 FIX CRÍTICO
                            fechaCreacion = receta.fechaCreacion,
                            dificultad = receta.dificultad,
                            calorias = receta.calorias,
                            enlaceYoutube = receta.youtube,
                            recrcid = receta.categoriaId
                        )
                    }

                    recetasList.clear()
                    recetasList.addAll(recetasConvertidas)

                    recetaAdapter.updateList(recetasList)

                    binding.txtDescripcion.text = when {
                        response.count > 0 ->
                            "${response.count} recetas encontradas"
                        else ->
                            "No se encontraron recetas"
                    }

                    Log.d("INGREDIENTES", "Mostrando ${response.count} recetas")
                }

            } catch (e: Exception) {

                withContext(Dispatchers.Main) {

                    Log.e("API_ERROR", "Error ingredientes → ${e.message}")

                    binding.txtDescripcion.text = "Error al buscar recetas"
                }

            } finally {

                withContext(Dispatchers.Main) {
                    cargandoEnProgreso = false   // 🔥 FIX IMPORTANTE
                }
            }
        }
    }



    private fun buscarSiHayIngredientes() {
        if (categoriaActualId <= 0) return

        if (ingredientesSeleccionados.isNotEmpty()) {
            Log.d("FLOW_TRACE",
                " buscarRecetasPorIngredientes( ← buscarSiHayIngredientes()"
            )
            buscarRecetasPorIngredientes(
                obtenerIngredientesTexto(),
                categoriaActualId
            )

        } else {
            Log.d("FLOW_TRACE",
                " cargarRecetasPorCategoriaIngredientes( ← buscarSiHayIngredientes()"
            )
            cargarRecetasPorCategoriaIngredientes(categoriaActualId)
        }
    }

    private fun cargarCategoriasingredientes() {
        Log.d("DEBUG_FLOW", "cargarCategoriasingredientes()")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.getCategorias()

                withContext(Dispatchers.Main) {
                    if (response.success) {
                        categoriasList.clear()
                        categoriasList.addAll(response.categorias)
                        Log.d("CATEGORIAS", "Cargadas ${response.categorias.size} categorías")
                        Log.d("FLOW_TRACE",
                            " setupCarruselIngredientes() ← cargarCategoriasingredientes()"
                        )
                        setupCarruselIngredientes()

                        if (categoriasList.isNotEmpty()) {
                            categoriaActualId = categoriasList[0].id
                            binding.txtDescripcion.text = "Ingredientes • ${categoriasList[0].descripcion}"
                            Log.d("FLOW_TRACE",
                                " buscarSiHayIngredientes()← cargarCategoriasingredientes()"
                            )
                            buscarSiHayIngredientes()
                        }
                    } else {
                        Log.e("CATEGORIAS", "Error en respuesta de API")
                        usarCategoriasPorDefecto()
                    }

                }
            } catch (e: Exception) {
                Log.e("CATEGORIAS", "Error cargando categorías: ${e.message}")
                withContext(Dispatchers.Main) {
                    usarCategoriasPorDefecto()
                }
            }
        }
    }



    private fun agregarIngredienteSeleccionado(ingrediente: BuscarIngredientes) {

        if (ingredientesSeleccionados.any { it.id == ingrediente.id }) return

        ingredientesSeleccionados.add(ingrediente)

        val view = crearIngredienteView(ingrediente)
        binding.chipGroupIngredientesBottom.addView(view)
        Log.d("FLOW_TRACE",
            " actualizarUI()← agregarIngredienteSeleccionado()"
        )
        actualizarUI()
        Log.d("FLOW_TRACE",
            " ejecutarBusqueda()← agregarIngredienteSeleccionado()"
        )
        ejecutarBusqueda()
    }


// ============================================================
// ON DESTROY - LIMPIAR CALLBACKS
// ============================================================

    override fun onDestroy() {
        super.onDestroy()
        carruselCallback?.let {
            binding.viewPagerCarrusel.unregisterOnPageChangeCallback(it)
        }
    }

// ============================================================
// MÉTODOS EXISTENTES QUE SE MANTIENEN IGUAL
// ============================================================

    private fun setupRecyclerView() {
        recetaAdapter = RecetaAdapter(recetasList, this)
        binding.recyclerItems.layoutManager = LinearLayoutManager(this)
        binding.recyclerItems.adapter = recetaAdapter
        binding.recyclerItems.visibility = View.VISIBLE
    }

    private fun setupDraggableIngredientes() {

        binding.containerIngredientes.post {

            val panel = binding.containerIngredientes
            val handle = binding.cardSearchBottom

            // ✅ POSICIÓN INICIAL (CLAVE)
            panel.translationY = -200f

            var initialY = 0f
            var initialTranslation = 0f

            handle.setOnTouchListener { _, event ->

                when (event.action) {

                    MotionEvent.ACTION_DOWN -> {
                        initialY = event.rawY
                        initialTranslation = panel.translationY
                        true
                    }

                    MotionEvent.ACTION_MOVE -> {

                        val deltaY = event.rawY - initialY
                        val newTranslation = initialTranslation + deltaY

                        val minY = -panel.height.toFloat() + 100
                        val maxY = 200f

                        panel.translationY = newTranslation.coerceIn(minY, maxY)

                        true
                    }

                    else -> false
                }
            }
        }
    }


    private fun crearIngredienteView(ingrediente: BuscarIngredientes): View {
        val view = layoutInflater.inflate(
            R.layout.item_ingrediente_card,
            binding.chipGroupIngredientesBottom,
            false
        )
        val imgEmoji = view.findViewById<ImageView>(R.id.imgEmoji)
        val txtNombre = view.findViewById<TextView>(R.id.txtNombre)
        val btnClose = view.findViewById<ImageView>(R.id.btnEliminar)
        txtNombre.text = ingrediente.descripcion
        if (!ingrediente.foto.isNullOrEmpty() && ingrediente.foto != "null") {
            Glide.with(this)
                .load(construirUrlCompleta(ingrediente.foto))
                .override(96, 96)
                .centerCrop()
                .into(imgEmoji)
        }
        // Cálculo para 4 columnas
        val screenWidth = resources.displayMetrics.widthPixels
        val paddingContainer = 48.dpToPx()
        val paddingChipGroup = 16.dpToPx()
        val chipSpacing = 12.dpToPx()
        val availableWidth = screenWidth - paddingContainer - paddingChipGroup
        val itemWidth = (availableWidth - (2 * chipSpacing)) / 4
        val params = ChipGroup.LayoutParams(itemWidth, ChipGroup.LayoutParams.WRAP_CONTENT)
        view.layoutParams = params
        Log.d("FLOW_TRACE",
            " actualizarUI() ← crearIngredienteView"
        )
        actualizarUI()
        btnClose.setOnClickListener {
            Log.d("INGREDIENTE", "Eliminando: ${ingrediente.descripcion}")
            ingredientesSeleccionados.removeAll { it.id == ingrediente.id }
            binding.chipGroupIngredientesBottom.removeView(view)
            Log.d("FLOW_TRACE",
                " actualizarUI() ← crearIngredienteView"
            )
            actualizarUI()
            Log.d("FLOW_TRACE",
                " buscarSiHayIngredientes() ← crearIngredienteView"
            )
            buscarSiHayIngredientes()
        }
        return view
    }

    private fun actualizarUI() {
        if (ingredientesSeleccionados.isEmpty()) {
            binding.chipGroupIngredientesBottom.removeAllViews()
            val textView = TextView(this).apply {
                text = ""
                gravity = Gravity.CENTER
                setTextColor(Color.GRAY)
                setPadding(0, 32.dpToPx(), 0, 0)
            }
            binding.chipGroupIngredientesBottom.addView(textView)
        }
    }

    private fun buscarIngredientes(query: String) {
        Log.d("DEBUG_FLOW", "buscarIngredientes()")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.getBuscarIngredientes(query)
                withContext(Dispatchers.Main) {
                    if (response.success) {
                        autoCompleteAdapter = ArrayAdapter(
                            this@Menu_principal_free,
                            android.R.layout.simple_dropdown_item_1line,
                            response.ingredientes
                        )
                        binding.editTextSearchBottom.setAdapter(autoCompleteAdapter)
                        autoCompleteAdapter?.filter?.filter(query) { count ->
                            if (count > 0) {
                                binding.editTextSearchBottom.showDropDown()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("INGREDIENTES", "Error: ${e.message}")
            }
        }
    }

    fun mostrarModoIngredientes() {
        binding.containerIngredientes.visibility = View.VISIBLE
        binding.editTextSearchBottom.requestFocus()
        Log.d("FLOW_TRACE",
            " actualizarUI() ← mostrarModoIngredientes()"
        )
        actualizarUI()
    }

    private fun setupBusquedaIngredientes() {
        Log.d("DEBUG_FLOW", "setupBusquedaIngredientes()")
        binding.editTextSearchBottom.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                if (query.length >= 1) {
                    Log.d("FLOW_TRACE",
                        " buscarIngredientes(query)← setupBusquedaIngredientes()"
                    )

                    buscarIngredientes(query)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.editTextSearchBottom.setOnItemClickListener { _, _, position, _ ->
            val ingrediente = autoCompleteAdapter?.getItem(position) ?: return@setOnItemClickListener
            Log.d("FLOW_TRACE",
                " agregarIngredienteSeleccionado(ingrediente)← binding.editTextSearchBottom"
            )
            agregarIngredienteSeleccionado(ingrediente)
            binding.editTextSearchBottom.text.clear()
        }
    }

    private fun construirUrlCompleta(rutaRelativa: String): String {
        val rutaLimpia = rutaRelativa.trim()
        if (rutaLimpia.startsWith("http://") || rutaLimpia.startsWith("https://")) {
            return rutaLimpia
        }
        val rutaFinal = if (rutaLimpia.startsWith("/")) rutaLimpia.substring(1) else rutaLimpia
        return "http://192.168.1.102/develoandroid/$rutaFinal"
    }

    private fun obtenerIngredientesTexto(): List<String> {
        return ingredientesSeleccionados.map { it.descripcion }
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }





    // ============================================================
// Para buscar desde la parte de recetasn
// ============================================================
    private fun setupSearch() {
        // Configurar el TextWatcher para búsqueda en tiempo real
        binding.editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                searchJob?.cancel()

                if (query.isNotEmpty()) {
                    isSearching = true
                    searchJob = CoroutineScope(Dispatchers.Main).launch {
                        delay(500)
                        if (categoriaActualId > 0) {
                            Log.d("FLOW_TRACE",
                                " buscarRecetasPorNombre← setupSearch()"
                            )
                            buscarRecetasPorNombre(query, categoriaActualId)
                        } else {
                            Toast.makeText(
                                this@Menu_principal_free,
                                "Selecciona una categoría primero",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    isSearching = false
                    if (categoriaActualId > 0) {
                        Log.d("FLOW_TRACE",
                            " para buscar por alimentos  cargarRecetasPorCategoria(← setupSearch()"
                        )
                        cargarRecetasPorCategoria(categoriaActualId)
                    }
                }
            }
        })

        // Configurar acción de búsqueda al presionar Enter
        binding.editTextSearch.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.editTextSearch.text.toString().trim()
                if (query.isNotEmpty() && categoriaActualId > 0) {
                    Log.d("FLOW_TRACE",
                        " buscarRecetasPorNombre ← binding.editTextSearch"
                    )
                    buscarRecetasPorNombre(query, categoriaActualId)
                }
                true
            } else {
                false
            }
        }
    }
    private fun cargarCategorias() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.getCategorias()

                withContext(Dispatchers.Main) {
                    if (response.success) {
                        categoriasList.clear()
                        categoriasList.addAll(response.categorias)
                        Log.d("FLOW_TRACE",
                            "setupCarrusel() ← cargarCategorias()"
                        )
                        Log.d("CATEGORIAS", "Cargadas ${response.categorias.size} categorías")
                        setupCarrusel()

                        if (categoriasList.isNotEmpty()) {
                            categoriaActualId = categoriasList[0].id
                            Log.d("FLOW_TRACE",
                                "cargarRecetasPorCategoria(← cargarCategorias()"
                            )
                            cargarRecetasPorCategoria(categoriaActualId)
                            binding.txtDescripcion.text = "Categoría: ${categoriasList[0].descripcion}"
                        }
                    } else {
                        Log.e("CATEGORIAS", "Error en respuesta de API")
                        Log.d("FLOW_TRACE",
                            "usarCategoriasPorDefecto()(← cargarCategorias()"
                        )
                        usarCategoriasPorDefecto()
                    }
                }
            } catch (e: Exception) {
                Log.e("CATEGORIAS", "Error cargando categorías: ${e.message}")
                withContext(Dispatchers.Main) {
                    Log.d("FLOW_TRACE",
                        "usarCategoriasPorDefecto()(← cargarCategorias()"
                    )
                    usarCategoriasPorDefecto()
                }
            }
        }
    }
    private fun usarCategoriasPorDefecto() {
        categoriasList.clear()
        categoriasList.addAll(listOf(
            Categoria(1, "Snack", "#660099"),
            Categoria(2, "Bebida", "#CC3300"),
            Categoria(3, "Plato fuerte", "#0066FF"),
            Categoria(4, "Postre", "#CC0099"),
            Categoria(5, "Entrada", "#009900"),
            Categoria(6, "Ensalada", "#4CAF50"),
            Categoria(7, "Especialidad", "#FF9800")
        ))
        Log.d("FLOW_TRACE",
            "setupCarrusel()← usarCategoriasPorDefecto()"
        )
        setupCarrusel()
        if (categoriasList.isNotEmpty()) {
            categoriaActualId = categoriasList[0].id
            Log.d("FLOW_TRACE",
                "cargarRecetasPorCategoria← usarCategoriasPorDefecto()"
            )
            cargarRecetasPorCategoria(categoriaActualId)
            binding.txtDescripcion.text = "Categorías por defecto"
        }
    }
    private fun setupCarrusel() {

        if (categoriasList.isEmpty()) return

        val adapter = CarruselAdapter(categoriasList) { position ->
            binding.viewPagerCarrusel.currentItem = position
        }

        binding.viewPagerCarrusel.adapter = adapter   // 🔥 SIEMPRE

        carruselCallback?.let {
            binding.viewPagerCarrusel.unregisterOnPageChangeCallback(it)
        }

        carruselCallback = object : ViewPager2.OnPageChangeCallback() {

            override fun onPageSelected(position: Int) {

                if (pantallaActual != PantallaActual.RECETAS) return

                val categoria = categoriasList[position]

                categoriaActualId = categoria.id

                Log.d("FLOW_TRACE", "RECETAS → Categoria ${categoria.descripcion}")

                val query = binding.editTextSearch.text.toString().trim()

                if (query.isNotEmpty()) {
                    buscarRecetasPorNombre(query, categoria.id)
                } else {
                    cargarRecetasPorCategoria(categoria.id)
                }
            }
        }

        binding.viewPagerCarrusel.registerOnPageChangeCallback(carruselCallback!!)
    }


    public fun cargarRecetasPorCategoria(categoriaId: Int) {
        Log.d("API_DEBUG", "=== CARGANDO RECETAS POR CATEGORÍA $categoriaId ===")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.getRecetasPorCategoria(categoriaId)
                Log.d("API_DEBUG", "Respuesta recibida - success: ${response.success}, count: ${response.count}")
                withContext(Dispatchers.Main) {
                    if (response.success) {
                        recetasList.clear()
                        recetasList.addAll(response.recetas)
                        recetaAdapter.updateList(recetasList)

                        val categoriaActual = categoriasList.find { it.id == categoriaId }
                        val nombreCategoria = categoriaActual?.descripcion ?: "Categoría $categoriaId"

                        binding.txtDescripcion.text = "$nombreCategoria: ${response.recetas.size} recetas"
                        Log.d("API_DEBUG", "Mostrando ${recetasList.size} recetas en UI")
                    } else {
                        binding.txtDescripcion.text = "❌ Error cargando recetas"
                    }
                }
            } catch (e: Exception) {
                Log.e("API_ERROR", "Error cargando recetas: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    binding.txtDescripcion.text = "Error: ${e.localizedMessage}"
                }
            }
        }
    }
    private fun buscarRecetasPorNombre(query: String, categoriaId: Int) {
        if (lastQuery == query && isSearching) {
            return
        }

        lastQuery = query
        Log.d("BUSQUEDA", "=== BUSCANDO: '$query' en categoría $categoriaId ===")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.buscarRecetas(query, categoriaId)

                Log.d("BUSQUEDA", "Respuesta búsqueda - success: ${response.success}, count: ${response.count}")

                withContext(Dispatchers.Main) {
                    if (response.success) {
                        recetasBusquedaList.clear()
                        recetasBusquedaList.addAll(response.recetas)

                        val recetasConvertidas = response.recetas.map { recetaBuscar ->
                            RecetaRecrcid(
                                id = recetaBuscar.id,
                                nombre = recetaBuscar.nombre ?: "Sin nombre",
                                descripcion = recetaBuscar.descripcion ?: "Sin descripción",
                                tiempoPreparacion = recetaBuscar.tiempoPreparacion ?: "No especificado",
                                porciones = recetaBuscar.porciones ?: 0,
                                fechaCreacion = recetaBuscar.fechaCreacion ?: "",
                                dificultad = recetaBuscar.dificultad,
                                calorias = recetaBuscar.calorias,
                                enlaceYoutube = recetaBuscar.enlaceYoutube,
                                recrcid = recetaBuscar.categoriaId
                            )
                        }

                        recetasList.clear()
                        recetasList.addAll(recetasConvertidas)
                        recetaAdapter.updateList(recetasList)

                        val categoriaActual = categoriasList.find { it.id == categoriaId }
                        val nombreCategoria = categoriaActual?.descripcion ?: "Categoría $categoriaId"

                        if (response.count > 0) {
                            binding.txtDescripcion.text = "${response.count} resultados para '$query' en $nombreCategoria"
                        } else {
                            binding.txtDescripcion.text = "No se encontraron recetas para '$query' en $nombreCategoria"
                        }

                        Log.d("BUSQUEDA", "Mostrando ${recetasList.size} recetas en búsqueda")

                    } else {
                        binding.txtDescripcion.text = "❌ Error en búsqueda"
                    }
                }

            } catch (e: Exception) {
                Log.e("BUSQUEDA_ERROR", "Error en búsqueda: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    binding.txtDescripcion.text = "Error en búsqueda: ${e.localizedMessage}"
                    if (!isSearching) {
                        Log.d("FLOW_TRACE",
                            "cargarRecetasPorCategoria(categoriaId) ← setupCarrusel()"
                        )
                        cargarRecetasPorCategoria(categoriaId)
                    }
                }
            }
        }
    }
    private fun parseColorSeguro(colorHex: String): Int {
        return try {
            if (colorHex.startsWith("#")) {
                Color.parseColor(colorHex)
            } else {
                Color.parseColor("#$colorHex")
            }
        } catch (e: IllegalArgumentException) {
            Log.e("COLOR", "Color inválido: $colorHex, usando color por defecto")
            Color.parseColor("#4CAF50")
        }
    }
    inner class CarruselAdapter(
        private val categorias: List<Categoria>,
        private val onCategoriaClick: (Int) -> Unit
    ) : RecyclerView.Adapter<CarruselAdapter.CarruselViewHolder>() {
        inner class CarruselViewHolder(
            val binding: ItemDelCarruselBinding
        ) : RecyclerView.ViewHolder(binding.root) {
            init {
                binding.root.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onCategoriaClick(position)
                    }
                }
            }
        }
        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): CarruselViewHolder {
            val binding = ItemDelCarruselBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return CarruselViewHolder(binding)
        }
        override fun onBindViewHolder(holder: CarruselViewHolder, position: Int) {
            val categoria = categorias[position]

            holder.binding.txtCategoria.text = categoria.descripcion
            holder.binding.cardCategoria.setCardBackgroundColor(
                parseColorSeguro(categoria.color)
            )

            if (categoria.id == categoriaActualId) {
                holder.binding.cardCategoria.cardElevation = 12f
                holder.binding.cardCategoria.alpha = 1.0f
            } else {
                holder.binding.cardCategoria.cardElevation = 4f
                holder.binding.cardCategoria.alpha = 0.8f
            }
        }
        override fun getItemCount(): Int = categorias.size
    }
    inner class RecetaAdapter(
        private var recetas: List<RecetaRecrcid>,
        private val fragmentActivity: FragmentActivity
    ) : RecyclerView.Adapter<RecetaAdapter.RecetaViewHolder>() {
        inner class RecetaViewHolder(
            val binding: ItemRecetaBinding
        ) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecetaViewHolder {
            val binding = ItemRecetaBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return RecetaViewHolder(binding)
        }

        override fun onBindViewHolder(holder: RecetaViewHolder, position: Int) {

            val receta = recetas[position]

            with(holder.binding) {

                tvRecetaNombre.text = receta.nombre ?: "Sin nombre"
                tvRecetaDescripcion.text = receta.descripcion ?: "Sin descripción"
                tvTiempo.text = receta.tiempoPreparacion ?: "Tiempo no especificado"

                tvPorciones.text = "${receta.porciones ?: 0} porciones"
                tvDificultad.text = "Nivel: ${receta.dificultad ?: "No especificado"}"

                tvCalorias.text = if (!receta.calorias.isNullOrEmpty()) {
                    "${receta.calorias} cal"
                } else {
                    "Calorías: N/A"
                }

                // 🔥 Cargar imágenes
                cargarImagenesReceta(receta.id, recyclerImagenes)
            }

            // 🔥 CLICK DEL ITEM COMPLETO
            holder.itemView.setOnClickListener {

                Log.d("RECETA_CLICK", "Receta: ${receta.nombre} (ID: ${receta.id})")

                val bottomSheet = DetalleRecetaBottomSheet.newInstance(receta.id)
                bottomSheet.show(fragmentActivity.supportFragmentManager, "DetalleRecetaBottomSheet")

                cargarDetallesReceta(receta.id, bottomSheet)
            }
        }


        private fun cargarDetallesReceta(recetaId: Int, bottomSheet: DetalleRecetaBottomSheet) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = ApiClient.apiService.getRecetaConIngredientes(recetaId)

                    withContext(Dispatchers.Main) {
                        if (response.success && response.receta != null) {
                            bottomSheet.actualizarDatos(response.receta)
                        } else {
                            Toast.makeText(
                                fragmentActivity,
                                "Error cargando detalles de la receta",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Log.e("DETALLE_ERROR", "Error cargando detalles: ${e.message}")
                        Toast.makeText(
                            fragmentActivity,
                            "Error de conexión",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        override fun getItemCount(): Int = recetas.size

        fun updateList(newList: List<RecetaRecrcid>) {
            recetas = newList
            notifyDataSetChanged()
        }
    }
// ============================================================
// Para poner las imagenes
// ============================================================
private fun cargarImagenesReceta(recetaId: Int, recyclerView: RecyclerView) {

    recyclerView.layoutManager =
        LinearLayoutManager(recyclerView.context, LinearLayoutManager.HORIZONTAL, false)

    CoroutineScope(Dispatchers.IO).launch {

        try {

            val response = ApiClient.apiService.getImagenesIngredientes(recetaId)

            withContext(Dispatchers.Main) {

                if (response.success) {

                    val imagenesValidas = response.imagenes.filterNotNull()

                    recyclerView.adapter = ImagenAdapter(imagenesValidas)
                }
            }

        } catch (e: Exception) {

            Log.e("IMG_API", "Error → ${e.message}")
        }
    }
}
    class ImagenAdapter(
        private val imagenes: List<String>
    ) : RecyclerView.Adapter<ImagenAdapter.ImagenViewHolder>() {

        inner class ImagenViewHolder(val imageView: ImageView)
            : RecyclerView.ViewHolder(imageView)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImagenViewHolder {

            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_imagen_receta, parent, false) as ImageView

            return ImagenViewHolder(view)
        }

        override fun onBindViewHolder(holder: ImagenViewHolder, position: Int) {

            val ruta = imagenes[position]

            Glide.with(holder.itemView.context)
                .load("http://192.168.1.102/develoandroid/${ruta.trimStart('/')}")
                .centerCrop()
                .into(holder.imageView)
        }

        override fun getItemCount() = imagenes.size
    }


}