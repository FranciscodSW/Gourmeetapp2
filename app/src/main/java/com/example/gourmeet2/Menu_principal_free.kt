package com.example.gourmeet2

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.gourmeet2.data.api.ApiClient
import com.example.gourmeet2.data.models.Categoria
import com.example.gourmeet2.data.models.IngredienteRecetaResponse
import com.example.gourmeet2.data.models.RecetaBuscar
import com.example.gourmeet2.data.models.RecetaRecrcid
import com.example.gourmeet2.databinding.ActivityMenuPrincipalFreeBinding
import com.example.gourmeet2.databinding.ItemDelCarruselBinding
import com.example.gourmeet2.databinding.ItemRecetaBinding
import com.example.gourmeet2.DetalleRecetaBottomSheet
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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


    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMenuPrincipalFreeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bottomNavigationView = binding.bottomNavigation

        // Configurar el listener para el BottomNavigationView
        setupBottomNavigation()

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

    private fun mostrarPantallaRecetas() {
        // Mostrar el contenedor de recetas
        binding.contentContainer.visibility = View.VISIBLE

        // Inicializar componentes solo si no están inicializados
        if (!::recetaAdapter.isInitialized) {
            setupRecyclerView()
        }

        // Configurar búsqueda (esto se puede llamar múltiples veces)
        setupSearch()

        // Cargar categorías si no están cargadas
        if (categoriasList.isEmpty()) {
            cargarCategorias()
        } else {
            // Si ya tenemos categorías, solo actualizar la UI
            setupCarrusel()
            if (categoriaActualId > 0) {
                cargarRecetasPorCategoria(categoriaActualId)
            }
        }

        // Si hay un query activo, restaurarlo
        val currentQuery = binding.editTextSearch.text.toString().trim()
        if (currentQuery.isNotEmpty() && categoriaActualId > 0) {
            buscarRecetasPorNombre(currentQuery, categoriaActualId)
        }
    }



    private fun mostrarPantallaIngredientes() {
        binding.contentContainer.visibility = View.GONE
        mostrarLayoutIngredientes()

    }

    private fun mostrarPantallaPlaneador() {
        // Ocultar el contenido actual de recetas
        binding.contentContainer.visibility = View.GONE

        // Aquí debes crear e inflar el layout para el planeador de compras
        mostrarLayoutPlaneador()
    }

    private fun mostrarLayoutIngredientes() {
        val mainLayout = binding.main as ConstraintLayout
       // mainLayout.removeView(mainLayout.findViewById(R.id.ingredientes_container))

        // 2. Inflar el layout de ingredientes
        /*val ingredientesLayout = layoutInflater.inflate(
            R.layout.layout_buscar_ingredientes,
            mainLayout,
            false
        )

        // 3. Configurar constraints para que esté sobre el BottomNavigationView
        val params = ingredientesLayout.layoutParams as ConstraintLayout.LayoutParams
        params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
        params.bottomToTop = R.id.bottom_navigation
        params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
        params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID

        ingredientesLayout.id = R.id.ingredientes_container
        mainLayout.addView(ingredientesLayout)

        // 4. Configurar la lógica de búsqueda por ingredientes
        setupBusquedaIngredientes(ingredientesLayout)

         */
    }

    private fun mostrarLayoutPlaneador() {
        /*
        // Similar a mostrarLayoutIngredientes pero con el layout del planeador
        val mainLayout = binding.main as ConstraintLayout
        mainLayout.removeView(mainLayout.findViewById(R.id.planeador_container))

        val planeadorLayout = layoutInflater.inflate(
            R.layout.layout_planeador_compras,
            mainLayout,
            false
        )

        val params = planeadorLayout.layoutParams as ConstraintLayout.LayoutParams
        params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
        params.bottomToTop = R.id.bottom_navigation
        params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
        params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID

        planeadorLayout.id = R.id.planeador_container
        mainLayout.addView(planeadorLayout)

        setupPlaneadorCompras(planeadorLayout)

         */
    }

    private fun setupBusquedaIngredientes(view: View) {
        /*
        // Aquí implementas la lógica para buscar por ingredientes
        // Ejemplo básico:
        val editTextIngredientes = view.findViewById<EditText>(R.id.editTextIngredientes)
        val btnBuscar = view.findViewById<Button>(R.id.btnBuscarIngredientes)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerIngredientes)

        btnBuscar.setOnClickListener {
            val ingredientes = editTextIngredientes.text.toString()
            if (ingredientes.isNotEmpty()) {
                buscarRecetasPorIngredientes(ingredientes)
            }
        }

         */
    }

    private fun setupPlaneadorCompras(view: View) {
        /*
        // Aquí implementas la lógica del planeador de compras
        // Ejemplo básico:
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerPlaneador)
        val btnAgregar = view.findViewById<Button>(R.id.btnAgregarItem)
        val editTextItem = view.findViewById<EditText>(R.id.editTextItem)

        btnAgregar.setOnClickListener {
            val item = editTextItem.text.toString()
            if (item.isNotEmpty()) {
                agregarItemPlaneador(item)
            }
        }

         */
    }

    private fun buscarRecetasPorIngredientes(ingredientes: String) {
        /*
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.buscarRecetasPorIngredientes(ingredientes)

                withContext(Dispatchers.Main) {
                    if (response.success) {
                        // Mostrar resultados en el RecyclerView
                        mostrarResultadosIngredientes(response.recetas)
                    } else {
                        Toast.makeText(
                            this@Menu_principal_free,
                            "Error buscando recetas",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@Menu_principal_free,
                        "Error de conexión: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

         */
    }

    private fun agregarItemPlaneador(item: String) {
        // Lógica para agregar items al planeador
        // Puedes usar Room, SharedPreferences o una lista en memoria
    }
    private fun setupRecyclerView() {
        recetaAdapter = RecetaAdapter(recetasList, this)
        binding.recyclerItems.layoutManager = LinearLayoutManager(this)
        binding.recyclerItems.adapter = recetaAdapter
        binding.recyclerItems.visibility = View.VISIBLE
    }

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

                        Log.d("CATEGORIAS", "Cargadas ${response.categorias.size} categorías")
                        setupCarrusel()

                        if (categoriasList.isNotEmpty()) {
                            categoriaActualId = categoriasList[0].id
                            cargarRecetasPorCategoria(categoriaActualId)
                            binding.txtDescripcion.text = "Categoría: ${categoriasList[0].descripcion}"
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

        setupCarrusel()

        if (categoriasList.isNotEmpty()) {
            categoriaActualId = categoriasList[0].id
            cargarRecetasPorCategoria(categoriaActualId)
            binding.txtDescripcion.text = "Categorías por defecto"
        }
    }

    private fun setupCarrusel() {
        if (categoriasList.isEmpty()) {
            Log.e("CARRUSEL", "No hay categorías para mostrar")
            return
        }

        val adapter = CarruselAdapter(categoriasList) { position ->
            binding.viewPagerCarrusel.currentItem = position
        }

        binding.viewPagerCarrusel.adapter = adapter

        binding.viewPagerCarrusel.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    if (position < categoriasList.size) {
                        val categoria = categoriasList[position]
                        binding.fondoDinamico.setBackgroundColor(parseColorSeguro(categoria.color))
                        categoriaActualId = categoria.id

                        if (isSearching) {
                            binding.editTextSearch.text.clear()
                            isSearching = false
                        }

                        val query = binding.editTextSearch.text.toString().trim()
                        if (query.isNotEmpty()) {
                            buscarRecetasPorNombre(query, categoria.id)
                            binding.txtDescripcion.text = "Buscando en ${categoria.descripcion}: '$query'"
                        } else {
                            cargarRecetasPorCategoria(categoria.id)
                            binding.txtDescripcion.text = "Categoría: ${categoria.descripcion}"
                        }

                        Log.d("CARRUSEL", "Categoría seleccionada: ${categoria.descripcion} (ID: ${categoria.id})")
                    }
                }
            }
        )

        if (categoriasList.isNotEmpty()) {
            val primeraCategoria = categoriasList[0]
            binding.fondoDinamico.setBackgroundColor(parseColorSeguro(primeraCategoria.color))
        }
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

        // Clase interna RecetaViewHolder debe estar definida DENTRO de RecetaAdapter
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
            try {
                val receta = recetas[position]

                with(holder.binding) {
                    tvRecetaNombre.text = receta.nombre ?: "Sin nombre"
                    tvRecetaDescripcion.text = receta.descripcion ?: "Sin descripción"
                    tvTiempo.text = receta.tiempoPreparacion ?: "Tiempo no especificado"

                    val porcionesTexto = try {
                        receta.porciones?.toString() ?: "0"
                    } catch (e: Exception) {
                        "0"
                    }
                    tvPorciones.text = "$porcionesTexto porciones"

                    tvDificultad.text = "Nivel: ${receta.dificultad ?: "No especificado"}"

                    tvCalorias.text = if (!receta.calorias.isNullOrEmpty() && receta.calorias != "") {
                        "${receta.calorias} cal"
                    } else {
                        "Calorías: N/A"
                    }
                }

                holder.itemView.setOnClickListener {
                    Log.d("RECETA_CLICK", "Receta: ${receta.nombre} (ID: ${receta.id})")

                    // Abrir BottomSheet con el ID de la receta
                    val bottomSheet = DetalleRecetaBottomSheet.newInstance(receta.id)
                    bottomSheet.show(fragmentActivity.supportFragmentManager, "DetalleRecetaBottomSheet")

                    // Opcional: Cargar datos de la receta antes de mostrar
                    cargarDetallesReceta(receta.id, bottomSheet)
                }
            } catch (e: Exception) {
                Log.e("BIND_ERROR", "Error en posición $position: ${e.message}")
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
}