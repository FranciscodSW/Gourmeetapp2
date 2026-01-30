package com.example.gourmeet2

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.gourmeet2.data.api.ApiClient
import com.example.gourmeet2.data.models.Categoria
import com.example.gourmeet2.data.models.RecetaRecrcid
import com.example.gourmeet2.databinding.ActivityMenuPrincipalFreeBinding
import com.example.gourmeet2.databinding.ItemDelCarruselBinding
import com.example.gourmeet2.databinding.ItemRecetaBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Menu_principal_free : AppCompatActivity() {

    private lateinit var binding: ActivityMenuPrincipalFreeBinding
    private lateinit var recetaAdapter: RecetaAdapter
    private val recetasList = mutableListOf<RecetaRecrcid>()
    private val categoriasList = mutableListOf<Categoria>()
    private var categoriaActualId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMenuPrincipalFreeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        cargarCategorias()
    }

    private fun setupRecyclerView() {
        recetaAdapter = RecetaAdapter(recetasList)
        binding.recyclerItems.layoutManager = LinearLayoutManager(this)
        binding.recyclerItems.adapter = recetaAdapter
        binding.recyclerItems.visibility = android.view.View.VISIBLE
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

                        // Cargar recetas de la primera categoría automáticamente
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

        // Crear adapter con callback para manejar clicks
        val adapter = CarruselAdapter(categoriasList) { position ->
            // Cuando se hace click en un item del carrusel, mover el ViewPager a esa posición
            binding.viewPagerCarrusel.currentItem = position
        }

        binding.viewPagerCarrusel.adapter = adapter

        // Listener para detectar cambios al deslizar
        binding.viewPagerCarrusel.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    if (position < categoriasList.size) {
                        val categoria = categoriasList[position]

                        // Cambiar color de fondo
                        binding.fondoDinamico.setBackgroundColor(parseColorSeguro(categoria.color))
                        binding.txtDescripcion.text = "Categoría: ${categoria.descripcion}"

                        // Cargar recetas de la nueva categoría seleccionada
                        categoriaActualId = categoria.id
                        cargarRecetasPorCategoria(categoria.id)

                        Log.d("CARRUSEL", "Categoría seleccionada: ${categoria.descripcion} (ID: ${categoria.id})")
                    }
                }
            }
        )

        // Estado inicial
        if (categoriasList.isNotEmpty()) {
            val primeraCategoria = categoriasList[0]
            binding.fondoDinamico.setBackgroundColor(parseColorSeguro(primeraCategoria.color))
        }
    }

    private fun cargarRecetasPorCategoria(categoriaId: Int) {
        Log.d("API_DEBUG", "=== INICIANDO CARGA PARA CATEGORÍA $categoriaId ===")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("API_DEBUG", "1. Llamando API...")
                // CORRECCIÓN: Usar la función correcta
                val response = ApiClient.apiService.getRecetasPorCategoria(categoriaId)

                Log.d("API_DEBUG", "2. Respuesta recibida:")
                Log.d("API_DEBUG", "   - success: ${response.success}")
                Log.d("API_DEBUG", "   - categoria_id: ${response.categoriaId}") // CORREGIDO
                Log.d("API_DEBUG", "   - count: ${response.count}") // CORREGIDO
                Log.d("API_DEBUG", "   - recetas size: ${response.recetas.size}")

                // Log de la primera receta para ver estructura
                if (response.recetas.isNotEmpty()) {
                    val primeraReceta = response.recetas[0]
                    Log.d("API_DEBUG", "3. Primera receta estructura:")
                    Log.d("API_DEBUG", "   - id: ${primeraReceta.id}")
                    Log.d("API_DEBUG", "   - nombre: ${primeraReceta.nombre}")
                    Log.d("API_DEBUG", "   - descripcion: ${primeraReceta.descripcion}")
                    Log.d("API_DEBUG", "   - tiempoPreparacion: ${primeraReceta.tiempoPreparacion}")
                    Log.d("API_DEBUG", "   - fechaCreacion: ${primeraReceta.fechaCreacion}")
                    Log.d("API_DEBUG", "   - dificultad: ${primeraReceta.dificultad}")
                    Log.d("API_DEBUG", "   - calorias: ${primeraReceta.calorias}")
                    Log.d("API_DEBUG", "   - recrcid: ${primeraReceta.recrcid}") // Añadido
                }

                withContext(Dispatchers.Main) {
                    if (response.success) {
                        Log.d("API_DEBUG", "4. Actualizando UI con ${response.recetas.size} recetas")

                        recetasList.clear()
                        recetasList.addAll(response.recetas) // DESCOMENTAR esta línea

                        try {
                            recetaAdapter.notifyDataSetChanged()
                            Log.d("API_DEBUG", "5. Adapter actualizado exitosamente")
                        } catch (e: Exception) {
                            Log.e("API_ERROR", "Error al actualizar adapter: ${e.message}", e)
                        }

                        val categoriaActual = categoriasList.find { it.id == categoriaId }
                        val nombreCategoria = categoriaActual?.descripcion ?: "Categoría $categoriaId"

                        binding.txtDescripcion.text = "$nombreCategoria: ${response.recetas.size} recetas"

                    } else {
                        binding.txtDescripcion.text = "❌ Error en respuesta del servidor"
                    }
                }

            } catch (e: Exception) {
                Log.e("API_ERROR", "EXCEPCIÓN COMPLETA:", e)
                withContext(Dispatchers.Main) {
                    binding.txtDescripcion.text = "Error: ${e.localizedMessage}"
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
            Color.parseColor("#4CAF50") // Verde por defecto
        }
    }

    // Adapter del carrusel CON CALLBACK
    inner class CarruselAdapter(
        private val categorias: List<Categoria>,
        private val onCategoriaClick: (Int) -> Unit // Callback para clicks
    ) : RecyclerView.Adapter<CarruselAdapter.CarruselViewHolder>() {

        inner class CarruselViewHolder(
            val binding: ItemDelCarruselBinding
        ) : RecyclerView.ViewHolder(binding.root) {
            init {
                // Configurar click listener
                binding.root.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onCategoriaClick(position) // Llamar al callback
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

            // Opcional: Resaltar categoría actual
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

    // Adapter para mostrar recetas
    inner class RecetaAdapter(
        private val recetas: List<RecetaRecrcid>
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

                    // Convertir porciones a String si es necesario
                    val porcionesTexto = try {
                        receta.porciones?.toString() ?: "0"
                    } catch (e: Exception) {
                        "0"
                    }
                    tvPorciones.text = "$porcionesTexto porciones"

                    tvDificultad.text = "Nivel: ${receta.dificultad ?: "No especificado"}"

                    // Calorías - manejar string vacío
                    tvCalorias.text = if (!receta.calorias.isNullOrEmpty() && receta.calorias != "") {
                        "${receta.calorias} cal"
                    } else {
                        "Calorías: N/A"
                    }
                }

                // Click listener para abrir detalle
                holder.itemView.setOnClickListener {
                    Log.d("RECETA_CLICK", "Receta: ${receta.nombre} (ID: ${receta.id})")
                }
            } catch (e: Exception) {
                Log.e("BIND_ERROR", "Error en posición $position: ${e.message}")
            }
        }

        override fun getItemCount(): Int = recetas.size
    }
}