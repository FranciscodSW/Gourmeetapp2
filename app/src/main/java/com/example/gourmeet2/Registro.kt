package com.example.gourmeet2

import android.content.Intent
import android.graphics.Color
import android.location.Geocoder
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.gourmeet2.data.api.ApiClient
import com.example.gourmeet2.data.models.UsuarioRegistro
import com.example.gourmeet2.databinding.ActivityRegistroBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class Registro : AppCompatActivity() {
    // Variables para controlar el estado del contenedor
    lateinit var btnEdad: MaterialButton
    lateinit var contenedor: LinearLayout
    private enum class EstadoContenedor {
        EXPANDIDO,      // Altura máxima
        MINIMIZADO,     // Altura mínima (solo se ve la barra)
        CERRADO         // Completamente oculto
    }
    private var edadSeleccionada: Int = 0
    data class NivelCocina(
        val nombre: String,
        val descripcion: String,
        val imagen: Int
    )
    private var estadoActual = EstadoContenedor.CERRADO
    private var alturaExpandida = 0
    private var alturaMinimizada = 0

    private var lastY = 0f
    private var contenedorExpandido = false
    private val alturaMinima = 400 // Altura mínima en píxeles
    private val alturaMaxima = 2000 // Altura máxima en píxeles
    private val MAP_REQUEST = 100
    private val ALTURA_BARRA = 80
    private var ubicacionSeleccionada: String = ""
    private var latitudSeleccionada: Double = 0.0
    private var longitudSeleccionada: Double = 0.0


    private var avatarSeleccionadoUrl: String = ""
    private val mapaLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.let { data ->
                latitudSeleccionada = data.getDoubleExtra("lat", 0.0)
                longitudSeleccionada = data.getDoubleExtra("lng", 0.0)
                val direccion = data.getStringExtra("direccion") ?: ""

                // Actualizar el texto del botón con la ubicación seleccionada
                binding.btneditUbicacion.text = direccion
                ubicacionSeleccionada = direccion

                // Mostrar un toast de confirmación
                Toast.makeText(this, "Ubicación seleccionada", Toast.LENGTH_SHORT).show()
            }
        }}

    private lateinit var binding: ActivityRegistroBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        enableEdgeToEdge()

        binding = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)
        btnEdad = binding.btneditSeleccionEdad
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.btneditUbicacion.setOnClickListener {

            val intent = Intent(this, MapaSeleccionActivity::class.java)
            mapaLauncher.launch(intent)
        }
        setupContenedor()


        binding.btnSeleccionAvatar.setOnClickListener {
            mostrarSelectorAvatarEnContenedor()
            setupBarraArrastre()
        }

        binding.btnSeleccionNivel.setOnClickListener {
          mostrarSelectorNivel()
            setupBarraArrastre()
        }
        binding.btneditSeleccionEdad.setOnClickListener {
            mostrarSelectorEdad()
            setupBarraArrastre()


        }


        setupValidaciones()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == MAP_REQUEST && resultCode == RESULT_OK) {

            val lat = data?.getDoubleExtra("lat", 0.0)
            val lng = data?.getDoubleExtra("lng", 0.0)

            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat!!, lng!!, 1)

            if (!addresses.isNullOrEmpty()) {


            }
        }
    }
    private fun setupContenedor() {
        // Convertir dp a píxeles
        val barraHeight = (ALTURA_BARRA * resources.displayMetrics.density).toInt()

        // Calcular alturas basadas en la pantalla
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels

        // Altura expandida: 70% de la pantalla
        alturaExpandida = (screenHeight * 0.9).toInt()

        // Altura minimizada: solo la barra + padding
        alturaMinimizada = barraHeight + 32 // 16dp padding top + 16dp padding bottom

        // Estado inicial: minimizado
        estadoActual = EstadoContenedor.MINIMIZADO
        actualizarAlturaContenedor(alturaMinimizada)
        actualizarTextoBarra()

        // Configurar click en la barra
        binding.barraArrastre.setOnClickListener {
            toggleContenedor()
        }

        // Opcional: doble click para cerrar completamente
        binding.barraArrastre.setOnLongClickListener {
            ocultarContenedorInferior()
            true
        }
    }
    private fun actualizarAlturaContenedor(altura: Int) {
        val params = binding.contenedorInferior.layoutParams
        params.height = altura
        binding.contenedorInferior.layoutParams = params
    }


    private fun setupBarraArrastre() {
        // Configurar click en la barra para expandir/contraer
        binding.barraArrastre.setOnClickListener {
            toggleContenedor()
        }

        // Opcional: Configurar gesto de arrastre
        binding.barraArrastre.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Guardar posición inicial del toque
                    // Aquí podrías implementar arrastre continuo
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaY = event.rawY - lastY
                    val newHeight = (binding.contenedorInferior.height - deltaY).toInt()
                    if (newHeight in alturaMinimizada..alturaExpandida) {
                        val params = binding.contenedorInferior.layoutParams
                        params.height = newHeight
                        binding.contenedorInferior.layoutParams = params
                    }
                    lastY = event.rawY
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // Determinar si debe expandirse o contraerse basado en la altura final
                    val params = binding.contenedorInferior.layoutParams
                    contenedorExpandido = params.height > (alturaMinima + alturaMaxima) / 2
                    actualizarTextoBarra()
                    true
                }
                else -> false
            }
        }
    }

    private fun toggleContenedor() {
        contenedorExpandido = !contenedorExpandido

        val params = binding.contenedorInferior.layoutParams
        params.height = if (contenedorExpandido) alturaMaxima else alturaMinima
        binding.contenedorInferior.layoutParams = params

        // Animar el cambio
        binding.contenedorInferior.animate()
            .setDuration(300)
            .start()

        actualizarTextoBarra()
    }

    private fun actualizarTextoBarra() {
        binding.tvExpandirContraer.text = if (contenedorExpandido) {
            "Desliza para contraer"
        } else {
            "Desliza para expandir"
        }
    }

    // También necesitas configurar el contenedor con altura fija inicial
    private fun configurarContenedor() {
        val params = binding.contenedorInferior.layoutParams
        params.height = alturaMinima
        binding.contenedorInferior.layoutParams = params
        contenedorExpandido = false
        actualizarTextoBarra()
    }


    private fun setupValidaciones() {
        binding.btnRegistrar.setOnClickListener {
            val nombre = binding.editNombre.text.toString().trim()
            val correo = binding.editCorreo.text.toString().trim()
            val pass = binding.editPassword.text.toString()
            val confirmPass = binding.editConfirmPassword.text.toString()
            if (!validarNombre(nombre)) return@setOnClickListener
            if (!validarCorreo(correo)) return@setOnClickListener
            if (!validarPassword(pass)) return@setOnClickListener
            if (pass != confirmPass) {
                mostrarError("Las contraseñas no coinciden")
                return@setOnClickListener
            }
            mostrarExito(nombre)
            // registrarUsuario(nombre, correo, pass)
        }
    }

    private fun registrarUsuario(nombre: String, correo: String, pass: String) {

        val ip = obtenerIP()   // 🔥 AQUÍ

        binding.txtError.visibility = View.GONE

        CoroutineScope(Dispatchers.IO).launch {

            try {

                val usuario = UsuarioRegistro(
                    nombre = nombre,
                    correo = correo,
                    password = pass,
                    cliPrimerIp = ip   // 🔥 enviada
                )

                val response = ApiClient.apiService.registrarUsuario(usuario)

                withContext(Dispatchers.Main) {

                    if (response.success) {
                        mostrarExito(nombre)
                    } else {
                        mostrarError(response.error ?: "Error desconocido")
                    }
                }

            } catch (e: Exception) {

                withContext(Dispatchers.Main) {

                    mostrarError("Error de conexión")

                }
            }
        }
    }


    // =========================
    // ✅ VALIDAR NOMBRE
    // =========================
    private fun validarNombre(nombre: String): Boolean {

        if (nombre.isEmpty()) {
            mostrarError("Ingresa tu nombre")
            return false
        }

        val regex = Regex("^[A-ZÁÉÍÓÚÑ][a-záéíóúñA-ZÁÉÍÓÚÑ ]*$")

        if (!regex.matches(nombre)) {
            mostrarError("Nombre inválido. Solo letras y debe iniciar con mayúscula")
            return false
        }

        return true
    }

    // =========================
    // ✅ VALIDAR CORREO
    // =========================
    private fun validarCorreo(correo: String): Boolean {

        if (correo.isEmpty()) {
            mostrarError("Ingresa tu correo")
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            mostrarError("Correo inválido")
            return false
        }

        return true
    }

    // =========================
    // ✅ VALIDAR PASSWORD
    // =========================
    private fun validarPassword(pass: String): Boolean {

        if (pass.length < 8) {
            mostrarError("La contraseña debe tener mínimo 8 caracteres")
            return false
        }

        val regexMayuscula = Regex(".*[A-Z].*")

        if (!regexMayuscula.matches(pass)) {
            mostrarError("La contraseña debe contener al menos una mayúscula")
            return false
        }

        return true
    }

    // =========================
    // ✅ MENSAJES
    // =========================
    private fun mostrarError(msg: String) {

        binding.txtError.apply {
            text = msg
            visibility = View.VISIBLE
            alpha = 0f
            animate().alpha(1f).setDuration(250).start()
        }
    }

    private fun mostrarExito(nombre: String) {
        binding.layoutRegistro.visibility = View.GONE
        binding.scrollPersonalizar.visibility = View.VISIBLE
    }

    private fun obtenerIP(): String {
        return try {
            val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
            val ipInt = wifiManager.connectionInfo.ipAddress

            String.format(
                "%d.%d.%d.%d",
                ipInt and 0xff,
                ipInt shr 8 and 0xff,
                ipInt shr 16 and 0xff,
                ipInt shr 24 and 0xff
            )
        } catch (e: Exception) {
            "0.0.0.0"
        }
    }

    private fun mostrarSelectorAvatarEnContenedor() {
        binding.tvTituloContenedor.text = "Selecciona tu avatar"

        // Inflar el layout de avatares
        val avatarView = layoutInflater.inflate(R.layout.dialog_avatar, null)
        val radioGroup = avatarView.findViewById<RadioGroup>(R.id.radiogroupgenero)
        val grid = avatarView.findViewById<GridLayout>(R.id.gridavatares)

        fun cargarAvatares(genero: String) {
            grid.removeAllViews()
            for (i in 1..5) {
                val imageView = ImageView(this)
                val params = GridLayout.LayoutParams()
                params.width = 250
                params.height = 250
                params.setMargins(16, 16, 16, 16)
                imageView.layoutParams = params
                imageView.scaleType = ImageView.ScaleType.CENTER_CROP

                val nombreImagen = if (genero == "M") {
                    "ic_usuario_$i"
                } else {
                    "ic_usuario_f$i"
                }

                val resourceId = resources.getIdentifier(
                    nombreImagen,
                    "drawable",
                    packageName
                )

                if (resourceId != 0) {
                    imageView.setImageResource(resourceId)
                }

                imageView.setOnClickListener {
                    avatarSeleccionadoUrl = nombreImagen
                    binding.btnSeleccionAvatar.text = "Avatar seleccionado"
                    ocultarContenedorInferior()
                    Toast.makeText(this, "Avatar seleccionado", Toast.LENGTH_SHORT).show()
                }
                grid.addView(imageView)
            }
        }

        // Cargar avatares masculinos por defecto
        cargarAvatares("M")

        // Configurar cambio de género
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbmasculino) {
                cargarAvatares("M")
            } else {
                cargarAvatares("F")
            }
        }

        // 🔥 SOLUCIÓN: En lugar de usar RecyclerView, usamos un contenedor lineal
        // para mostrar el contenido del avatar
        val contenedorAvatar = LinearLayout(this)
        contenedorAvatar.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        contenedorAvatar.orientation = LinearLayout.VERTICAL
        contenedorAvatar.addView(avatarView)

        // Reemplazar el contenido del contenedor inferior
        val parent = binding.recyclerOpciones.parent as? ViewGroup
        val index = (parent?.indexOfChild(binding.recyclerOpciones) ?: 0) + 1

        // Ocultar RecyclerView y mostrar el contenedor personalizado
        binding.recyclerOpciones.visibility = View.GONE

        // Buscar si ya existe un contenedor personalizado y eliminarlo
        val existingContainer = binding.contenedorInferior.findViewWithTag<View>("avatar_container")
        existingContainer?.let { binding.contenedorInferior.removeView(it) }

        // Agregar el nuevo contenedor con tag para identificarlo
        contenedorAvatar.tag = "avatar_container"
        binding.contenedorInferior.addView(contenedorAvatar, binding.contenedorInferior.childCount - 1)

        mostrarContenedorInferior()
    }
    private fun mostrarSelectorNivel() {

        binding.tvTituloContenedor.text = "Selecciona tu nivel"

        val listaNiveles = listOf(
            NivelCocina(
                "Ayudante de cocina (Commis)",
                "Apoya en tareas básicas: lavar, cortar, preparar ingredientes.",
                R.drawable.ic_gorrito_1
            ),
            NivelCocina(
                "Cocinero (Chef de partida)",
                "Encargado de una estación específica (carnes, pastas, postres, etc.).",
                R.drawable.ic_gorrito_2
            ),
            NivelCocina(
                "Subchef (Sous Chef)",
                "Segundo al mando. Supervisa al equipo y reemplaza al chef cuando no está.",
                R.drawable.ic_gorrito_3
            ),
            NivelCocina(
                "Chef Ejecutivo",
                "Responsable del menú, calidad, costos y organización de la cocina.",
                R.drawable.ic_gorrito_4
            ),
            NivelCocina(
                "Chef Corporativo",
                "Supervisa varias cocinas o restaurantes dentro de una empresa.",
                R.drawable.ic_gorrito_5
            )
        )

        val contenedor = LinearLayout(this)
        contenedor.orientation = LinearLayout.VERTICAL
        contenedor.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        listaNiveles.forEach { nivel ->

            val item = layoutInflater.inflate(R.layout.dialog_nivel, null)
            val img = item.findViewById<ImageView>(R.id.imggorrito)
            val txtNombre = item.findViewById<TextView>(R.id.txtnivel)
            val txtDescripcion = item.findViewById<TextView>(R.id.txtdescripcion)
            img.setImageResource(nivel.imagen)
            txtNombre.text = nivel.nombre
            txtDescripcion.text = nivel.descripcion
            item.setOnClickListener {
                binding.btnSeleccionNivel.setText(nivel.nombre)
                ocultarContenedorInferior()
                Toast.makeText(this, "Nivel seleccionado", Toast.LENGTH_SHORT).show()
            }
            contenedor.addView(item)
        }

        // Ocultar RecyclerView
        binding.recyclerOpciones.visibility = View.GONE

        // Eliminar contenedor anterior si existe
        val existing = binding.contenedorInferior.findViewWithTag<View>("nivel_container")
        existing?.let { binding.contenedorInferior.removeView(it) }

        contenedor.tag = "nivel_container"
        binding.contenedorInferior.addView(contenedor, binding.contenedorInferior.childCount - 1)

        mostrarContenedorInferior()
    }
    private fun ocultarContenedorInferior() {
        binding.contenedorInferior.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                binding.contenedorInferior.visibility = View.GONE
                // Limpiar vistas personalizadas
                val viewsToRemove = mutableListOf<View>()
                for (i in 0 until binding.contenedorInferior.childCount) {
                    val child = binding.contenedorInferior.getChildAt(i)
                    if (child.tag != null && child != binding.tvTituloContenedor &&
                        child != binding.btnCerrarContenedor) {
                        viewsToRemove.add(child)
                    }
                }
                viewsToRemove.forEach { binding.contenedorInferior.removeView(it) }
                // Mostrar RecyclerView nuevamente
                binding.recyclerOpciones.visibility = View.VISIBLE
            }
    }
    private fun mostrarContenedorInferior() {

        binding.contenedorInferior.visibility = View.VISIBLE

        // 🔥 Forzar expansión
        val params = binding.contenedorInferior.layoutParams
        params.height = alturaExpandida
        binding.contenedorInferior.layoutParams = params

        contenedorExpandido = true
        actualizarTextoBarra()

        binding.contenedorInferior.alpha = 0f
        binding.contenedorInferior.animate()
            .alpha(1f)
            .setDuration(300)
            .setListener(null)
    }
    class NivelAdapter(private val lista: List<NivelCocina>) :
        RecyclerView.Adapter<NivelAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val img = view.findViewById<ImageView>(R.id.imgGorrito)
            val txt = view.findViewById<TextView>(R.id.txtNivel)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_nivel, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = lista[position]
            holder.img.setImageResource(item.imagen)
            holder.txt.text = item.nombre
        }

        override fun getItemCount() = lista.size
    }
    private fun mostrarSelectorEdad() {
        binding.tvTituloContenedor.text = "Selecciona tu edad"

        // Crear contenedor principal
        val contenedorEdad = LinearLayout(this)
        contenedorEdad.orientation = LinearLayout.VERTICAL
        contenedorEdad.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        // Crear TextView para mostrar la edad seleccionada
        val txtEdadSeleccionada = TextView(this).apply {
            text = "18 años"
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(Color.BLACK)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 32
                bottomMargin = 32
            }
        }

        // Crear RecyclerView para el carrusel
        val recyclerView = RecyclerView(this)
        recyclerView.layoutParams = RecyclerView.LayoutParams(
            RecyclerView.LayoutParams.MATCH_PARENT,
            400 // Altura fija para el carrusel
        )

        // Configurar LayoutManager horizontal con centrado
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.layoutManager = layoutManager

        // Agregar SnapHelper para efecto de centrado
        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(recyclerView)

        // Crear lista de edades (18 a 100 años)
        val edades = (18..100).toList()

        // Crear y configurar el adaptador
        val adapter = EdadAdapter(edades) { edad ->
            txtEdadSeleccionada.text = "$edad años"
            edadSeleccionada = edad
        }
        recyclerView.adapter = adapter

        // Scroll a la edad por defecto (18)
        recyclerView.post {
            layoutManager.scrollToPosition(0)
        }

        // Agregar vistas al contenedor
        contenedorEdad.addView(txtEdadSeleccionada)
        contenedorEdad.addView(recyclerView)

        // Botón para confirmar selección
        val btnConfirmar = MaterialButton(this).apply {
            text = "CONFIRMAR EDAD"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 32
                leftMargin = 32
                rightMargin = 32
                bottomMargin = 32
            }
            cornerRadius = 20
            setOnClickListener {
                if (edadSeleccionada > 0) {
                    btnEdad.text = "$edadSeleccionada años"
                    ocultarContenedorInferior()
                    Toast.makeText(this@Registro, "Edad seleccionada: $edadSeleccionada", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@Registro, "Selecciona una edad", Toast.LENGTH_SHORT).show()
                }
            }
        }
        contenedorEdad.addView(btnConfirmar)

        // Ocultar RecyclerView principal
        binding.recyclerOpciones.visibility = View.GONE

        // Eliminar contenedor anterior si existe
        val existing = binding.contenedorInferior.findViewWithTag<View>("edad_container")
        existing?.let { binding.contenedorInferior.removeView(it) }

        contenedorEdad.tag = "edad_container"
        binding.contenedorInferior.addView(contenedorEdad, binding.contenedorInferior.childCount - 1)

        mostrarContenedorInferior()
    }
    inner class EdadAdapter(
        private val edades: List<Int>,
        private val onItemClick: (Int) -> Unit
    ) : RecyclerView.Adapter<EdadAdapter.EdadViewHolder>() {

        private var selectedPosition = 0

        inner class EdadViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val txtEdad: TextView = itemView.findViewById(R.id.txtEdad)
            private val cardView: androidx.cardview.widget.CardView = itemView.findViewById(R.id.cardView)

            fun bind(edad: Int, isSelected: Boolean) {
                txtEdad.text = edad.toString()

                // Cambiar estilo según si está seleccionado
                if (isSelected) {
                    cardView.setCardBackgroundColor(Color.parseColor("#0E90E4"))
                    txtEdad.setTextColor(Color.WHITE)
                    cardView.cardElevation = 8f
                } else {
                    cardView.setCardBackgroundColor(Color.WHITE)
                    txtEdad.setTextColor(Color.BLACK)
                    cardView.cardElevation = 2f
                }

                itemView.setOnClickListener {
                    val previousPosition = selectedPosition
                    selectedPosition = adapterPosition
                    notifyItemChanged(previousPosition)
                    notifyItemChanged(selectedPosition)
                    onItemClick(edad)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EdadViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_edad, parent, false)
            return EdadViewHolder(view)
        }

        override fun onBindViewHolder(holder: EdadViewHolder, position: Int) {
            holder.bind(edades[position], position == selectedPosition)
        }

        override fun getItemCount() = edades.size
    }

    data class Restriccion(
        val Id_Restricciones: Int,
        val Res_Nombre: String,
        val Res_Descripcion: String,
        var seleccionada: Boolean = false
    )
    class RestriccionAdapter(
        private val lista: MutableList<Restriccion>
    ) : RecyclerView.Adapter<RestriccionAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            //val card: MaterialCardView = view.findViewById(R.id.cardRestriccion)
            val nombre: TextView = view.findViewById(R.id.tvNombre)
            val descripcion: TextView = view.findViewById(R.id.tvDescripcion)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_restriccion, parent, false)

            return ViewHolder(view)
        }

        override fun getItemCount() = lista.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {

            val item = lista[position]

            holder.nombre.text = item.Res_Nombre
            holder.descripcion.text = item.Res_Descripcion

            // COLOR
            if (item.seleccionada) {
                //holder.card.setCardBackgroundColor(Color.parseColor("#0E90E4"))
                holder.nombre.setTextColor(Color.WHITE)
                holder.descripcion.setTextColor(Color.WHITE)
            } else {
               // holder.card.setCardBackgroundColor(Color.WHITE)
                holder.nombre.setTextColor(Color.BLACK)
                holder.descripcion.setTextColor(Color.GRAY)

            }


        }
    }



}

