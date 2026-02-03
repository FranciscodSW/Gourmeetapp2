package com.example.gourmeet2

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.gourmeet2.data.api.ApiClient
import com.example.gourmeet2.data.models.Categoria
import com.example.gourmeet2.data.models.RecetaBuscar
import com.example.gourmeet2.data.models.RecetaRecrcid
import com.example.gourmeet2.databinding.ActivityMenuPrincipalFreeBinding
import com.example.gourmeet2.databinding.ItemDelCarruselBinding
import com.example.gourmeet2.databinding.ItemRecetaBinding
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
    private val recetasBusquedaList = mutableListOf<RecetaBuscar>() // Lista separada para búsqueda
    private val categoriasList = mutableListOf<Categoria>()
    private var categoriaActualId: Int = -1

    // Variables para controlar la búsqueda
    private var searchJob: Job? = null
    private var isSearching: Boolean = false
    private var lastQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMenuPrincipalFreeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupSearch()
        cargarCategorias()
    }

    private fun setupRecyclerView() {
        recetaAdapter = RecetaAdapter(recetasList)
        binding.recyclerItems.layoutManager = LinearLayoutManager(this)
        binding.recyclerItems.adapter = recetaAdapter
        binding.recyclerItems.visibility = android.view.View.VISIBLE
    }

    private fun setupSearch() {
        // Configurar TextWatcher para búsqueda en tiempo real
        binding.editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()

                // Cancelar búsqueda anterior si existe
                searchJob?.cancel()

                if (query.isNotEmpty()) {
                    isSearching = true
                    // Lanzar nueva búsqueda con retardo para evitar demasiadas llamadas
                    searchJob = CoroutineScope(Dispatchers.Main).launch {
                        delay(500) // Esperar 500ms después de que el usuario deja de escribir
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
                    // Si el campo está vacío, volver a cargar recetas de la categoría actual
                    isSearching = false
                    if (categoriaActualId > 0) {
                        cargarRecetasPorCategoria(categoriaActualId)
                    }
                }
            }
        })

        // Configurar acción del botón de búsqueda en el teclado
        binding.editTextSearch.setOnEditorActionListener { v, actionId, event ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
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
                        response.categorias.forEachIndexed { index, categoria ->
                            Log.d("CATEGORIAS", "${index + 1}. ID: ${categoria.id} - ${categoria.descripcion}")
                        }

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

                        // Cambiar color de fondo
                        binding.fondoDinamico.setBackgroundColor(parseColorSeguro(categoria.color))

                        // Actualizar categoría actual
                        categoriaActualId = categoria.id

                        // Limpiar búsqueda si hay una activa
                        if (isSearching) {
                            binding.editTextSearch.text.clear()
                            isSearching = false
                        }

                        // Cargar recetas según si hay búsqueda activa o no
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

    private fun cargarRecetasPorCategoria(categoriaId: Int) {
        Log.d("API_DEBUG", "=== CARGANDO RECETAS POR CATEGORÍA $categoriaId ===")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.getRecetasPorCategoria(categoriaId)

                Log.d("API_DEBUG", "Respuesta recibida - success: ${response.success}, count: ${response.count}")

                withContext(Dispatchers.Main) {
                    if (response.success) {
                        recetasList.clear()
                        recetasList.addAll(response.recetas)
                        recetaAdapter.updateList(recetasList) // Usar método updateList

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
        // Solo buscar si la consulta es diferente a la anterior
        if (lastQuery == query && isSearching) {
            return
        }

        lastQuery = query
        Log.d("BUSQUEDA", "=== BUSCANDO: '$query' en categoría $categoriaId ===")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // LLAMAR A LA API DE BÚSQUEDA
                val response = ApiClient.apiService.buscarRecetas(query, categoriaId)

                Log.d("BUSQUEDA", "Respuesta búsqueda - success: ${response.success}, count: ${response.count}")

                withContext(Dispatchers.Main) {
                    if (response.success) {
                        // Limpiar lista de búsqueda y convertir RecetaBuscar a RecetaRecrcid
                        recetasBusquedaList.clear()
                        recetasBusquedaList.addAll(response.recetas)
                        // Convertir RecetaBuscar a RecetaRecrcid para el adapter
                        val recetasConvertidas = response.recetas.map { recetaBuscar ->
                            RecetaRecrcid(  // ← NOTA: Usar el nombre correcto
                                id = recetaBuscar.id,
                                nombre = recetaBuscar.nombre ?: "Sin nombre",
                                descripcion = recetaBuscar.descripcion ?: "Sin descripción",
                                tiempoPreparacion = recetaBuscar.tiempoPreparacion ?: "No especificado",
                                porciones = recetaBuscar.porciones ?: 0,  // ← Asegurar que sea Int
                                fechaCreacion = recetaBuscar.fechaCreacion ?: "",
                                dificultad = recetaBuscar.dificultad,
                                calorias = recetaBuscar.calorias,
                                enlaceYoutube = recetaBuscar.enlaceYoutube,
                                recrcid = recetaBuscar.categoriaId
                            )
                        }

                        // Actualizar lista principal
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

                    // Si falla la búsqueda, volver a cargar recetas de la categoría
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

    // Adapter del carrusel CON CALLBACK
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

    // Adapter para mostrar recetas - MODIFICADO para aceptar actualizaciones
    inner class RecetaAdapter(
        private var recetas: List<RecetaRecrcid>
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
                    // Aquí puedes abrir el detalle de la receta
                }
            } catch (e: Exception) {
                Log.e("BIND_ERROR", "Error en posición $position: ${e.message}")
            }
        }

        override fun getItemCount(): Int = recetas.size

        // Método para actualizar la lista
        fun updateList(newList: List<RecetaRecrcid>) {
            recetas = newList
            notifyDataSetChanged()
        }
    }
}