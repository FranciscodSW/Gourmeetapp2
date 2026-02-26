package com.example.gourmeet2

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.gourmeet2.data.api.ApiClient
import com.example.gourmeet2.data.models.Comentario
import com.example.gourmeet2.data.models.IngredienteRecetaResponse
import com.example.gourmeet2.data.models.PasoPreparacion
import com.example.gourmeet2.data.models.RecetaConIngredientes
import com.example.gourmeet2.data.models.RecetaRecrcid
import com.example.gourmeet2.data.models.Respuesta
import com.example.gourmeet2.databinding.FragmentDetalleRecetaBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class DetalleRecetaBottomSheet : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentDetalleRecetaBinding
    private var recetaId: Int = -1
    private var listaPasos: List<PasoPreparacion> = emptyList()
    private var pasoActual: Int = 0
    private var totalPasos: Int = 0
    private var initialX = 0f
    private var initialY = 0f
    private var isSwiping = false
    private var listaComentarios: List<Comentario> = emptyList()

    private var recetaDetalle: RecetaConIngredientes? = null

    companion object {
        fun newInstance(recetaId: Int): DetalleRecetaBottomSheet {
            return DetalleRecetaBottomSheet().apply {
                arguments = Bundle().apply {
                    putInt("recetaId", recetaId)
                }
            }
        }
    }
    override fun onStart() {
        super.onStart()

        val bottomSheet =
            dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                ?: return

        val behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet)

        behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
        behavior.peekHeight = (resources.displayMetrics.heightPixels * 0.9).toInt() // 90% de la pantalla
        behavior.isFitToContents = true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDetalleRecetaBinding.inflate(inflater, container, false)

        // Configurar el ScrollView para que no intercepte gestos horizontales
        binding.root.findViewById<View>(R.id.scrollView)?.let { scrollView ->
            scrollView.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = event.x
                        initialY = event.y
                        v.parent.requestDisallowInterceptTouchEvent(true)
                        return@setOnTouchListener false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val diffX = Math.abs(event.x - initialX)
                        val diffY = Math.abs(event.y - initialY)

                        // Si el movimiento es más horizontal que vertical, evitar que el ScrollView lo intercepte
                        if (diffX > diffY * 1.5) {
                            v.parent.requestDisallowInterceptTouchEvent(true)
                            return@setOnTouchListener false
                        }
                    }
                }
                return@setOnTouchListener false
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recetaId = arguments?.getInt("recetaId") ?: -1
        if (recetaId != -1) {
            setupSwipeGesture()
            cargarPasosPreparacion()
            cargarReceta()

            if (recetaDetalle != null) {
                mostrarDatosReceta()
                Log.d("COMENTARIOS_DEBUG", listaComentarios.toString())
                mostrarComentarios(listaComentarios)
            }
        }
    }
    class ComentariosAdapter(
        private val lista: List<Comentario>
    ) : RecyclerView.Adapter<ComentariosAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

            val nombre = view.findViewById<TextView>(R.id.tvNombreUsuario)
            val comentario = view.findViewById<TextView>(R.id.tvComentario)
            val rating = view.findViewById<TextView>(R.id.tvRating)
            val imagen = view.findViewById<ImageView>(R.id.imgUser)
            val recyclerRespuestas = view.findViewById<RecyclerView>(R.id.recyclerRespuestas)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_comentario, parent, false)

            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {

            val item = lista[position]

            holder.nombre.text = item.cliNombre
            holder.comentario.text = item.comentario

            holder.rating.text = "⭐"   // si luego agregas COM_RATING lo ponemos

            // ✅ Imagen usuario
            val urlImagen = item.fotoUsuario?.let {
                "http://192.168.1.80/develoandroid/Img_usuarios/$it"
            }

            Glide.with(holder.itemView.context)
                .load(urlImagen ?: R.drawable.ic_user)
                .into(holder.imagen)

            // ✅ Respuestas
            if (item.respuestas.isNotEmpty()) {

                holder.recyclerRespuestas.visibility = View.VISIBLE
                holder.recyclerRespuestas.layoutManager =
                    LinearLayoutManager(holder.itemView.context)

                holder.recyclerRespuestas.adapter =
                    RespuestasAdapter(item.respuestas)

            } else {
                holder.recyclerRespuestas.visibility = View.GONE
            }
        }

        override fun getItemCount(): Int = lista.size
    }
    class RespuestasAdapter(
        private val lista: List<Respuesta>
    ) : RecyclerView.Adapter<RespuestasAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

            val nombre = view.findViewById<TextView>(R.id.tvNombreUsuarioRespuesta)
            val respuesta = view.findViewById<TextView>(R.id.tvTextoRespuesta)
            val fecha = view.findViewById<TextView>(R.id.tvFechaRespuesta)
            val imagen = view.findViewById<ImageView>(R.id.imgUserRespuesta)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_respuesta, parent, false)

            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {

            val item = lista[position]

            holder.nombre.text = item.cliNombre
            holder.respuesta.text = item.respuesta
            holder.fecha.text = item.respFecha   // si la tienes

            val urlImagen = item.fotoUsuario?.let {
                "http://192.168.1.80/develoandroid/Img_usuarios/$it"
            }

            Glide.with(holder.itemView.context)
                .load(urlImagen ?: R.drawable.ic_user)
                .placeholder(R.drawable.ic_user)
                .error(R.drawable.ic_user)
                .into(holder.imagen)
        }

        override fun getItemCount(): Int = lista.size
    }
    private fun mostrarComentarios(lista: List<Comentario>?) {

        if (lista.isNullOrEmpty()) {
            binding.recyclerComentarios.visibility = View.GONE
            return
        }

        binding.recyclerComentarios.visibility = View.VISIBLE

        binding.recyclerComentarios.layoutManager =
            LinearLayoutManager(requireContext())
        binding.recyclerComentarios.adapter = ComentariosAdapter(lista)
    }
    private fun cargarReceta() {

        viewLifecycleOwner.lifecycleScope.launch {

            try {
                val response = ApiClient.apiService.getRecetaConIngredientes(recetaId)

                if (response.success) {

                    recetaDetalle = response.receta
                    listaComentarios = response.comentarios ?: emptyList()
                    Log.d("COMENTARIOS_DEBUG", listaComentarios.toString())
                    mostrarDatosReceta()
                    mostrarComentarios(listaComentarios)

                } else {
                    Log.e("API", "Error: ${response.error}")
                }

            } catch (e: Exception) {
                Log.e("API", "Error conexión: ${e.message}")
            }
        }
    }
    private fun setupSwipeGesture() {
        binding.cardPreparacion.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = event.x
                    initialY = event.y
                    isSwiping = false

                    // Impedir que el ScrollView intercepte
                    view.parent.requestDisallowInterceptTouchEvent(true)
                    return@setOnTouchListener true
                }

                MotionEvent.ACTION_MOVE -> {
                    val diffX = event.x - initialX
                    val diffY = event.y - initialY

                    // Solo procesar si es principalmente horizontal
                    if (Math.abs(diffX) > Math.abs(diffY) * 2) {
                        isSwiping = true

                        // Mover la tarjeta mientras se desliza
                        binding.cardPreparacion.translationX = diffX * 0.5f

                        // Cambiar opacidad basado en la dirección
                        if (diffX > 0) {
                            binding.cardPreparacion.alpha = 1 - Math.min(diffX / 300, 0.3f)
                        } else {
                            binding.cardPreparacion.alpha = 1 - Math.min(-diffX / 300, 0.3f)
                        }

                        view.parent.requestDisallowInterceptTouchEvent(true)
                        return@setOnTouchListener true
                    } else if (Math.abs(diffY) > Math.abs(diffX) * 1.5) {
                        // Es principalmente vertical, dejar que el ScrollView lo maneje
                        view.parent.requestDisallowInterceptTouchEvent(false)
                        return@setOnTouchListener false
                    }
                }

                MotionEvent.ACTION_UP -> {
                    val diffX = event.x - initialX

                    // Solo procesar si fue un deslizamiento horizontal
                    if (isSwiping) {
                        // Determinar si fue suficiente para cambiar de paso
                        if (Math.abs(diffX) > 100) { // Umbral mínimo de 100px
                            if (diffX > 0) {
                                // Deslizó a la derecha - paso anterior
                                cambiarPasoAnterior()
                            } else {
                                // Deslizó a la izquierda - paso siguiente
                                cambiarPasoSiguiente()
                            }
                        } else {
                            // No fue suficiente, animar de vuelta
                            binding.cardPreparacion.animate()
                                .translationX(0f)
                                .alpha(1f)
                                .setDuration(200)
                                .start()
                        }
                        return@setOnTouchListener true
                    }

                    view.parent.requestDisallowInterceptTouchEvent(false)
                }

                MotionEvent.ACTION_CANCEL -> {
                    binding.cardPreparacion.animate()
                        .translationX(0f)
                        .alpha(1f)
                        .setDuration(200)
                        .start()
                    view.parent.requestDisallowInterceptTouchEvent(false)
                }
            }
            return@setOnTouchListener false
        }
    }
    private fun cambiarPasoAnterior() {
        if (pasoActual > 0) {
            val direccion = 1f // Derecha
            // Animación de salida
            binding.cardPreparacion.animate()
                .translationX(direccion * 300)
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    pasoActual--
                    mostrarPaso(pasoActual)
                    // Preparar para animación de entrada
                    binding.cardPreparacion.translationX = -direccion * 300
                    binding.cardPreparacion.alpha = 0f

                    // Animación de entrada
                    binding.cardPreparacion.animate()
                        .translationX(0f)
                        .alpha(1f)
                        .setDuration(200)
                        .start()
                }
                .start()
        } else {
            // Si es el primer paso, animar de vuelta
            binding.cardPreparacion.animate()
                .translationX(0f)
                .alpha(1f)
                .setDuration(200)
                .start()
        }
    }
    private fun cambiarPasoSiguiente() {
        if (pasoActual < totalPasos - 1) {
            val direccion = -1f // Izquierda

            // Animación de salida
            binding.cardPreparacion.animate()
                .translationX(direccion * 300)
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    pasoActual++
                    mostrarPaso(pasoActual)

                    // Preparar para animación de entrada
                    binding.cardPreparacion.translationX = -direccion * 300
                    binding.cardPreparacion.alpha = 0f

                    // Animación de entrada
                    binding.cardPreparacion.animate()
                        .translationX(0f)
                        .alpha(1f)
                        .setDuration(200)
                        .start()
                }
                .start()
        } else {
            // Si es el último paso, animar de vuelta
            binding.cardPreparacion.animate()
                .translationX(0f)
                .alpha(1f)
                .setDuration(200)
                .start()
        }
    }
    fun actualizarDatos(receta: RecetaConIngredientes) {
        this.recetaDetalle = receta
        if (isAdded) {
            mostrarDatosReceta()
            mostrarComentarios(listaComentarios)
            Log.d("COMENTARIOS_DEBUG", listaComentarios.toString())

        }
    }
    private fun mostrarDatosReceta() {
        recetaDetalle?.let { receta ->
            try {
                binding.tvRecetaNombre.text = receta.recNombre ?: "Sin nombre"
                binding.tvRecetaDescripcion.text = receta.recDescripcion ?: "Sin descripción"
                binding.tvTiempo.text = "⏱ ${receta.recTiempoPreparacion ?: "No especificado"}"
                binding.tvPorciones.text = "👥 ${receta.recPorciones ?: 0} porciones"
                binding.tvDificultad.text = "📊 ${receta.recDificultad ?: "No especificada"}"
                binding.tvDatoGourmet.text = "${receta.datoval ?: "Sin descripción"}"
                val ratingTexto = if (receta.promedio != null) {
                    String.format("%.1f", receta.promedio)   // 🔥 4.3 → 4.3
                } else {
                    "N/A"
                }
                val enlaceYoutube = receta.recEnlaceYoutube
                if (!enlaceYoutube.isNullOrEmpty()) {

                    binding.cardVideo.visibility = View.VISIBLE

                    val videoId = extraerVideoId(enlaceYoutube)

                    val thumbnailUrl = "https://img.youtube.com/vi/$videoId/0.jpg"

                    Log.d("YOUTUBE_DEBUG", thumbnailUrl)

                    Glide.with(this)
                        .load(thumbnailUrl)
                        .into(binding.imgPreviewVideo)

                } else {
                    binding.cardVideo.visibility = View.GONE
                }
                binding.cardVideo.setOnClickListener {

                    receta.recEnlaceYoutube?.let { url ->

                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)
                    }
                }
                binding.tvRating.text = ratingTexto


                binding.tvCategoria.text = "🍽 Categoría ${receta.tipo ?: "Sin categoría"}"

                val url = "http://192.168.1.80/develoandroid/recetas/${receta.recId}/${receta.fotoReceta}"

                Log.d("IMG_DEBUG", url)

                Glide.with(this)
                    .load(url)
                    .into(binding.imgFondo)
                mostrarIngredientes(receta.ingredientes)
                calcularTotales(receta.ingredientes)
                calcularCostoTotal(receta.ingredientes)

            } catch (e: Exception) {
                Log.e("RECETA_ERROR", "Error: ${e.message}")
            }
        }
    }
    private fun extraerVideoId(url: String): String {

        return when {
            url.contains("embed/") -> {
                url.substringAfter("embed/")
            }
            url.contains("watch?v=") -> {
                url.substringAfter("watch?v=")
            }
            else -> {
                url // fallback por si solo mandas ID
            }
        }
    }
    private fun mostrarIngredientes(ingredientes: List<com.example.gourmeet2.data.models.Ingrediente>?) {
        binding.containerIngredientes.removeAllViews()

        ingredientes?.forEach { ingrediente ->
            val layout = LinearLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12.dpToPx() }
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
            }

            val tvIngrediente = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = "• ${ingrediente.cantidad} ${ingrediente.nombreIngrediente}"
                typeface = ResourcesCompat.getFont(requireContext(), R.font.caviardreams)
                textSize = 16f
                setTextColor(ContextCompat.getColor(requireContext(), android.R.color.tertiary_text_light))
            }

            val tvCaloriasIng = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                val calorias = ingrediente.caloriasIngrediente ?: 0f
                text = "${calorias.toInt()} cal"
                typeface = ResourcesCompat.getFont(requireContext(), R.font.caviardreams)
                textSize = 14f
                setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
            }
            val tvPrecioIng = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )


            }

            layout.addView(tvIngrediente)
            layout.addView(tvCaloriasIng)

            binding.containerIngredientes.addView(layout)
        }
    }
    private fun calcularCostoTotal(
        ingredientes: List<com.example.gourmeet2.data.models.Ingrediente>?
    ) {
        val total = ingredientes?.sumOf {
            it.precioEstimado?.toDouble() ?: 0.0
        } ?: 0.0

        binding.tvTotalPrecio.text = "$${"%.2f".format(total)}"
    }

    private fun calcularTotales(ingredientes: List<com.example.gourmeet2.data.models.Ingrediente>?) {
        val totalCalorias = ingredientes?.sumOf { it.caloriasIngrediente?.toDouble() ?: 0.0 } ?: 0.0
        binding.tvTotalCalorias.text = "${totalCalorias.toInt()} cal"
    }

    private fun cargarPasosPreparacion() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.getPasosPreparacion(recetaId)

                if (response.success) {
                    listaPasos = response.pasos.sortedBy { it.paso }
                    totalPasos = listaPasos.size

                    if (listaPasos.isNotEmpty()) {
                        mostrarPaso(pasoActual)
                        crearIndicadoresPuntos()
                    } else {
                        binding.tvDescripcionPaso.text = "No hay pasos disponibles"
                    }
                } else {
                    binding.tvDescripcionPaso.text = "Error cargando pasos"
                }
            } catch (e: Exception) {
                android.util.Log.e("PASOS_ERROR", "Error: ${e.message}")
                binding.tvDescripcionPaso.text = "Error de conexión"
            }
        }
    }

    private fun mostrarPaso(indice: Int) {
        if (indice in listaPasos.indices) {
            pasoActual = indice
            val paso = listaPasos[indice]

            binding.tvDescripcionPaso.text = paso.descripcion
            binding.tvContadorPasos.text = "Paso ${indice + 1}/$totalPasos"

            if (paso.tiempo != null && paso.tiempo > 0) {
                binding.tvTiempoPaso.text = "⏱ ${formatearTiempo(paso.tiempo)}"
                binding.tvTiempoPaso.visibility = View.VISIBLE
            } else {
                binding.tvTiempoPaso.visibility = View.GONE
            }

            actualizarBarraProgreso()
            actualizarIndicadoresPuntos()

            android.util.Log.d("PASOS", "Mostrando paso ${indice + 1}")
        }
    }

    private fun formatearTiempo(minutosFloat: Float): String {
        val minutos = minutosFloat.toInt()
        val segundos = ((minutosFloat - minutos) * 60).toInt()
        return if (segundos > 0) "$minutos min $segundos seg" else "$minutos min"
    }

    private fun crearIndicadoresPuntos() {
        binding.containerPuntos.removeAllViews()

        for (i in 0 until totalPasos) {
            ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(8, 0, 8, 0) }
                binding.containerPuntos.addView(this)
            }
        }
        actualizarIndicadoresPuntos()
    }

    private fun actualizarIndicadoresPuntos() {
        for (i in 0 until binding.containerPuntos.childCount) {
            val imageView = binding.containerPuntos.getChildAt(i) as ImageView
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                if (i == pasoActual) {
                    setSize(40, 40)
                    setColor(ContextCompat.getColor(requireContext(), R.color.black))
                } else {
                    setSize(24, 24)
                    setColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
                }
            }
            imageView.setImageDrawable(drawable)
        }
    }

    private fun actualizarBarraProgreso() {
        if (totalPasos > 0) {
            val progressWeight = (pasoActual + 1).toFloat() / totalPasos
            val weightRestante = 1 - progressWeight

            (binding.progressBarActual.layoutParams as LinearLayout.LayoutParams).weight = progressWeight
            (binding.progressBarPaso.layoutParams as LinearLayout.LayoutParams).weight = weightRestante

            binding.progressBarActual.requestLayout()
            binding.progressBarPaso.requestLayout()
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
}