package com.example.gourmeet2

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.Menu
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.example.gourmeet2.data.api.ApiClient
import com.example.gourmeet2.data.models.*
import com.example.gourmeet2.databinding.ActivityMenuPrincipalFreeBinding
import com.example.gourmeet2.utils.SesionUsuario
import kotlinx.coroutines.launch
import android.Manifest
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ScrollView
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.view.children
import com.facebook.appevents.codeless.internal.ViewHierarchy.setOnClickListener
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import androidx.core.widget.doAfterTextChanged
import androidx.activity.OnBackPressedCallback
class Menu_principal_free : AppCompatActivity() {
    private var menuAbierto = false
    private lateinit var binding: ActivityMenuPrincipalFreeBinding
    private var modoActual = Modo.INGREDIENTES
    enum class Modo {INGREDIENTES,RECETAS }
    private val listaProveedores = mutableListOf<Proveedor>()
    private lateinit var adapterProveedores: ProveedorAdapter
    enum class Seccion { BUSCADOR, ALACENA, PLANEADOR}
    private var panelBusquedaAbierto = false
    private var seccionActual = Seccion.BUSCADOR
    private var textoBusqueda = ""
    private var busquedaProveedorAbierta = false
    private val ingredientesSeleccionados = mutableListOf<BuscarIngredientes>()
    private lateinit var seleccionadosAdapter: SeleccionadosAdapter
    private val recetasSeleccionadas = mutableListOf<BuscarRecetas>()
    private lateinit var recetasAdapter: RecetasAdapter
    private var categoriaSeleccionada: Int? = null
    private lateinit var adapterResultados: AdapterResultados
    private lateinit var adapter: IngredienteAdapter
    private lateinit var adapterSeccionesProveedores: SeccionesProveedoresAdapter
    private var latitudUsuario: Double? = null
    private var longitudUsuario: Double? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuPrincipalFreeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {

                override fun handleOnBackPressed() {

                    // ======================================
                    // DETALLE DE RECETA ABIERTO
                    // ======================================

                    if (
                        binding.containerDetalleReceta.visibility ==
                        View.VISIBLE
                    ) {

                        cerrarDetalleReceta()

                        return
                    }


                    // ======================================
                    // DETALLE DE PROVEEDOR ABIERTO
                    // ======================================

                    if (
                        binding.containerDetalleProveedor.visibility ==
                        View.VISIBLE
                    ) {

                        cerrarDetalleProveedor()

                        return
                    }


                    // ======================================
                    // MIS COLECCIONES ABIERTAS
                    // ======================================

                    if (
                        binding.rvMisColeccionesProveedores.visibility ==
                        View.VISIBLE
                    ) {

                        cerrarMisColeccionesProveedores()

                        return
                    }


                    // ======================================
                    // FILTROS ABIERTOS
                    // ======================================

                    if (
                        binding.panelFiltrosProveedores.visibility ==
                        View.VISIBLE
                    ) {

                        cerrarFiltrosProveedores()

                        return
                    }
                    // ======================================
                    // BÚSQUEDA DE PROVEEDOR ABIERTA
                    // ======================================
                    if (busquedaProveedorAbierta) {

                        cerrarBusquedaProveedor()

                        return
                    }


                    // ======================================
                    // SI NO HAY NINGÚN PANEL ABIERTO
                    // DEJAMOS QUE ANDROID REGRESE
                    // ======================================

                    isEnabled = false

                    onBackPressedDispatcher.onBackPressed()
                }
            }
        )


        cargarUsuario()
        cargarInformacionUsuario()
        inicializarMenuLateral()
        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)
        inicializarProveedores()
        configurarBusquedaProveedores()
        obtenerUbicacionUsuario()


        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                binding.containerDetalleReceta.visibility = View.GONE
                binding.containerDetalleProveedor.visibility =
                    View.GONE
            }
        }
        actualizarModo()
        binding.editBusqueda.setOnEditorActionListener { _, _, _ ->
            true    // Consume cualquier acción del botón del teclado
        }
        binding.editBusqueda.addTextChangedListener {
            val texto = it.toString()
            if (texto.length >= 2) {
                if (modoActual == Modo.INGREDIENTES) {
                    buscarIngredientes(texto)
                } else {
                    buscarRecetas(texto)
                }
            }
        }
        binding.btnProveedores.setOnClickListener {

            if (binding.panelProveedores.visibility == View.VISIBLE) {

                // ==========================================
                // CERRAR → SALE HACIA LA IZQUIERDA
                // ==========================================

                binding.panelProveedores.animate()
                    .translationX(
                        -binding.panelProveedores.width.toFloat()
                    )
                    .setDuration(300)
                    .withEndAction {

                        binding.panelProveedores.visibility =
                            View.GONE

                        binding.panelProveedores.translationX =
                            0f
                    }
                    .start()

            } else {

                // ==========================================
                // CERRAR MIS COLECCIONES
                // ==========================================

                binding.rvMisColeccionesProveedores.visibility =
                    View.GONE


                // ==========================================
                // MOSTRAR LISTA NORMAL DE PROVEEDORES
                // ==========================================

                if (listaProveedores.isNotEmpty()) {

                    binding.rvProveedores.visibility =
                        View.VISIBLE

                    binding.layoutSinProveedores.visibility =
                        View.GONE

                } else {

                    binding.rvProveedores.visibility =
                        View.GONE

                    binding.layoutSinProveedores.visibility =
                        View.VISIBLE
                }


                // ==========================================
                // ABRIR PANEL DE PROVEEDORES
                // ==========================================

                binding.panelProveedores.visibility =
                    View.VISIBLE

                binding.panelProveedores.post {

                    // Comienza fuera de la pantalla, a la izquierda
                    binding.panelProveedores.translationX =
                        -binding.panelProveedores.width.toFloat()

                    // Entra hasta su posición original
                    binding.panelProveedores.animate()
                        .translationX(0f)
                        .setDuration(300)
                        .start()
                }


                // ==========================================
                // CARGAR PROVEEDORES
                // ==========================================

                cargarProveedores()
            }
        }
        binding.editBusqueda.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER &&
                event.action == KeyEvent.ACTION_DOWN) {
                true   // Bloquea el Enter
            } else {
                false
            }
        }
        adapter = IngredienteAdapter(emptyList()) { ingrediente ->
            moverASeleccionados(ingrediente)
            binding.rvResultados.visibility = View.GONE
            binding.panelingredietes.visibility = View.GONE
            binding.editBusqueda.setText("")
        }
        adapterResultados = AdapterResultados(
            mutableListOf()
        ) { receta ->
            abrirDetalleReceta(receta.REC_ID)
        }
        binding.rvPrincipal.layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(this)
        binding.rvPrincipal.adapter = adapterResultados
        binding.rvResultados.layoutManager =
            GridLayoutManager(this, 1, GridLayoutManager.HORIZONTAL, false)
        binding.rvResultados.adapter = adapter
        binding.cardCentro.setOnClickListener {
            cambiarSeccion()
        }
        binding.opModo.setOnClickListener {
            if (modoActual == Modo.INGREDIENTES) {
                modoActual = Modo.RECETAS
                binding.txtTitulo.text = "Recetas"
                buscarRecetas(textoBusqueda)
            } else {
                modoActual = Modo.INGREDIENTES
                binding.txtTitulo.text = "Ingredientes"
                buscarIngredientes(textoBusqueda)
            }
            actualizarModo()
            actualizarTextoBuscador()
            actualizarBusquedaCategoria()
        }
        binding.imgFlecha.setOnClickListener {
            if (!menuAbierto) {
                mostrarMenuAnimado()
            } else {
                ocultarMenuAnimado()
            }
            menuAbierto = !menuAbierto
        }
        binding.opSnack.setOnClickListener {
            categoriaSeleccionada = 1
            cambiarEncabezado("Snack", R.drawable.ic_logo_morado)
            actualizarBusquedaCategoria()

        }
        binding.opBebida.setOnClickListener {
            categoriaSeleccionada = 2
            cambiarEncabezado("Bebida", R.drawable.ic_logo_naranja)
            actualizarBusquedaCategoria()


        }
        binding.opPlatoFuerte.setOnClickListener {
            categoriaSeleccionada = 3
            cambiarEncabezado("Plato fuerte", R.drawable.ic_logo_azul)
            actualizarBusquedaCategoria()


        }
        binding.opPostre.setOnClickListener {
            categoriaSeleccionada = 4
            cambiarEncabezado("Postre", R.drawable.ic_logo_rosa)
            actualizarBusquedaCategoria()

        }
        binding.opEntrada.setOnClickListener {
            categoriaSeleccionada = 5
            cambiarEncabezado("Entrada", R.drawable.ic_logo_verde)
            actualizarBusquedaCategoria()
        }
        binding.barraExpandirBusqueda.setOnClickListener {
            if (!panelBusquedaAbierto) {
                abrirPanelBusqueda()
            } else {cerrarPanelBusqueda() }
            panelBusquedaAbierto = !panelBusquedaAbierto
        }
        binding.headerProveedores.btnbuscar.setOnClickListener {
            if (busquedaProveedorAbierta) {
                cerrarBusquedaProveedor()
            } else {
                abrirBusquedaProveedor()
            }
        }
        binding.headerProveedores.btnubicaion.setOnClickListener {

            val proveedoresMapa =
                listaProveedores.mapNotNull { proveedor ->

                    val latitud =
                        proveedor.Pro_Latitud?.toDoubleOrNull()

                    val longitud =
                        proveedor.Pro_Longitud?.toDoubleOrNull()

                    if (
                        latitud != null &&
                        longitud != null
                    ) {

                        ProveedorMapa(
                            id = proveedor.Id_Proveedor.toString(),
                            nombre = proveedor.Pro_nombre ?: "Proveedor",
                            latitud = latitud,
                            longitud = longitud,
                            fotoPerfil = proveedor.Pro_Foto_Perfil
                        )

                    } else {

                        null
                    }
                }

            val intent =
                Intent(
                    this,
                    MapaProveedoresActivity::class.java
                )

            intent.putExtra(
                MapaProveedoresActivity.EXTRA_PROVEEDORES,
                ArrayList(proveedoresMapa)
            )

            startActivity(intent)
        }
        binding.headerProveedores.btnmiscoleccionesprovedor.setOnClickListener {
            mostrarMisColeccionesProveedores()
        }
        binding.headerProveedores.btnfiltros.setOnClickListener {

            // ==========================================
            // SI LOS FILTROS YA ESTÁN ABIERTOS
            // ==========================================

            if (
                binding.panelFiltrosProveedores.visibility ==
                View.VISIBLE
            ) {

                cerrarFiltrosProveedores()

                binding.headerProveedores.btnfiltros
                    .setImageResource(
                        R.drawable.ic_filtro
                    )

            } else {

                // ==========================================
                // CERRAR MIS COLECCIONES
                // ==========================================

                if (
                    binding.rvMisColeccionesProveedores.visibility ==
                    View.VISIBLE
                ) {

                    cerrarMisColeccionesProveedores()
                }


                // ==========================================
                // CAMBIAR ICONO
                // ==========================================

                binding.headerProveedores.btnfiltros
                    .setImageResource(
                        R.drawable.ic_filtro_on
                    )


                // ==========================================
                // ABRIR FILTROS
                // ==========================================

                abrirFiltrosProveedores()
            }
        }
        binding.panelFiltrosProveedores.findViewById<ImageView>(R.id.btnCerrarFiltros).setOnClickListener {

                cerrarFiltrosProveedores()
            }
        binding.panelFiltrosProveedores.findViewById<MaterialButton>(R.id.btnLimpiarFiltros).setOnClickListener {

                limpiarFiltrosProveedores()
            }
        binding.panelFiltrosProveedores.findViewById<MaterialButton>(R.id.btnAplicarFiltros).setOnClickListener {

                aplicarFiltrosProveedores()
            }
        seleccionadosAdapter = SeleccionadosAdapter(
            ingredientesSeleccionados
        ) { ingrediente ->
            ingredientesSeleccionados.remove(ingrediente)
            seleccionadosAdapter.notifyDataSetChanged()
        }
        binding.rvSeleccionados.layoutManager = GridLayoutManager(this, 1, GridLayoutManager.HORIZONTAL, false)
        binding.rvSeleccionados.adapter = seleccionadosAdapter
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom.toFloat()
            val extraOffset = 50f // ajusta esto (16–48 suele ser ideal)
            binding.panelBusqueda.translationY = -(imeHeight - extraOffset)
            binding.panelingredietes.translationY = -(imeHeight - extraOffset)
            insets
        }
        binding.btnPerfil.setOnClickListener {
            cargarInformacionUsuario()
            binding.drawerLayout.openDrawer(GravityCompat.END)
        }

        cargarRecetasInicio()
    }
    private fun cerrarMenu() {
        menuAbierto = false
        ocultarMenuAnimado()
    }
    private fun actualizarModo() {
        if (modoActual == Modo.INGREDIENTES) {
            binding.imgModo.setImageResource(R.drawable.ic_recetas)
            binding.txtModo.text = "Recetas"
        } else {
            binding.imgModo.setImageResource(R.drawable.ic_ingredientes)
            binding.txtModo.text = "Ingrediente"
        }
    }
    private fun mostrarMenuAnimado() {
        binding.menuCategorias.visibility = View.VISIBLE
        binding.menuCategorias.alpha = 0f
        binding.menuCategorias.translationY = -50f
        binding.menuCategorias.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(250)
            .start()
        binding.imgFlecha.animate()
            .rotation(180f)
            .setDuration(250)
            .start()
    }
    private fun ocultarMenuAnimado() {
        binding.menuCategorias.animate()
            .alpha(0f)
            .translationY(-50f)
            .setDuration(250)
            .withEndAction {
                binding.menuCategorias.visibility = View.GONE
            }
            .start()
        binding.imgFlecha.animate()
            .rotation(0f)
            .setDuration(250)
            .start()
    }
    private fun cambiarSeccion() {
        when (seccionActual) {
            Seccion.BUSCADOR -> {
                seccionActual = Seccion.ALACENA
                binding.txtSeccionActual.text = "Alacena"
            }
            Seccion.ALACENA -> {
                seccionActual = Seccion.PLANEADOR
                binding.txtSeccionActual.text = "Planeador semanal"
            }
            Seccion.PLANEADOR -> {
                seccionActual = Seccion.BUSCADOR
                actualizarTextoBuscador()
            }
        }
    }
    private fun actualizarTextoBuscador() {
        if (seccionActual == Seccion.BUSCADOR) {
            if (modoActual == Modo.INGREDIENTES) {
                binding.txtSeccionActual.text = "Buscador por ingredientes"
            } else {
                binding.txtSeccionActual.text = "Buscador por recetas"
            }
        }
    }
    private fun cambiarEncabezado(titulo: String, imagen: Int) {
        binding.txtTitulo.text = titulo
        binding.imgLogo.setImageResource(imagen)
        cerrarMenu()
    }
    private fun abrirPanelBusqueda() {
        binding.cardSeleccionados.visibility = View.VISIBLE
        binding.cardSeleccionados.alpha = 0f
        binding.cardSeleccionados.translationY = 50f
        binding.cardSeleccionados.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(250)
            .start()
    }
    private fun cerrarPanelBusqueda() {
        binding.cardSeleccionados.animate()
            .alpha(0f)
            .translationY(50f)
            .setDuration(250)
            .withEndAction {
                binding.cardSeleccionados.visibility = View.GONE
            }
            .start()
    }
    private fun buscarIngredientes(texto: String) {
        lifecycleScope.launch {
            try {
                val respuesta = ApiClient.apiService.autocompleteIngredientes(texto)
                if (respuesta.success) {
                    binding.rvResultados.visibility = View.VISIBLE
                    binding.panelingredietes.visibility = View.VISIBLE
                    adapter.updateData(respuesta.ingredientes)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    private fun buscarRecetas(texto: String) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.autocompleteRecetas(texto)
                if (response.success) {
                    binding.rvResultados.visibility = View.VISIBLE
                    binding.panelingredietes.visibility = View.VISIBLE
                    val lista = response.recetas
                    recetasAdapter = RecetasAdapter(lista) { receta ->
                        moverRecetasSeleccionadas(receta)
                        binding.rvResultados.visibility = View.GONE
                        binding.panelingredietes.visibility = View.GONE
                        binding.editBusqueda.setText("")
                    }
                    binding.rvResultados.adapter = recetasAdapter
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    private fun moverRecetasSeleccionadas(receta: BuscarRecetas) {

        val yaExiste = recetasSeleccionadas.any {
            it.id == receta.id
        }
        if (!yaExiste) {
            recetasSeleccionadas.add(receta)
            recetasAdapter.notifyDataSetChanged()
            cargarRecetasPorNombre(receta.nombre)
        }
    }
    private fun moverASeleccionados(ingrediente: BuscarIngredientes) {
        val yaExiste = ingredientesSeleccionados.any {
            it.id == ingrediente.id
        }
        if (!yaExiste) {
            ingredientesSeleccionados.add(ingrediente)
            seleccionadosAdapter.notifyDataSetChanged()
            cargarRecetasPorIngredientes()
        }
    }
    private fun cargarRecetasInicio() {
        lifecycleScope.launch {
            try {
                val request = RecetasInicioRequest(categoriaId = categoriaSeleccionada ?: 0)
                val response = ApiClient.apiService.getRecetasInicio(request)
                if (response.success) {
                    val secciones = listOf(
                        SeccionResultados("Más recientes", response.coincidencia),
                        SeccionResultados("Menos calorías", response.calorias),
                        SeccionResultados("Preparación rápida", response.tiempo),
                        SeccionResultados("Más económicas", response.gasto),
                        SeccionResultados("Sin lácteos", response.sin_lacteos),
                        SeccionResultados("Sin azúcar", response.sin_azucar),
                        SeccionResultados("Difíciles", response.dificultad))
                    adapterResultados.actualizar(secciones)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    private fun cargarRecetasPorIngredientes() {
        lifecycleScope.launch {
            try {
                val request = FiltrosRecetasRequest(
                    ingredientes = ingredientesSeleccionados.map { it.id },
                    categoriaId = categoriaSeleccionada
                )
                val response = ApiClient.apiService.getFiltrosRecetas(request)
                if (response.success) {
                    val secciones = listOf(
                        SeccionResultados("Coincidencia", response.coincidencia),
                        SeccionResultados("Calorías", response.calorias),
                        SeccionResultados("Tiempo", response.tiempo),
                        SeccionResultados("Gasto", response.gasto),
                        SeccionResultados("Sin lácteos", response.sin_lacteos),
                        SeccionResultados("Sin azúcar", response.sin_azucar),
                        SeccionResultados("Dificultad", response.dificultad)
                    )
                    adapterResultados.actualizar(secciones)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    private fun cargarRecetasPorNombre(nombreReceta: String) {
        lifecycleScope.launch {
            try {
                val request =
                    FiltrosRecetasNombreRequest(
                        busqueda = nombreReceta,
                        categoriaId = categoriaSeleccionada
                    )
                val response =
                    ApiClient.apiService.getFiltrosRecetasNombre(request)
                if (response.success) {
                    val secciones = listOf(
                        SeccionResultados(
                            "Coincidencia",
                            response.coincidencia
                        ),
                        SeccionResultados(
                            "Calorías",
                            response.calorias
                        ),
                        SeccionResultados(
                            "Tiempo",
                            response.tiempo
                        ),
                        SeccionResultados(
                            "Gasto",
                            response.gasto
                        ),
                        SeccionResultados(
                            "Sin lácteos",
                            response.sin_lacteos
                        ),
                        SeccionResultados(
                            "Sin azúcar",
                            response.sin_azucar
                        ),
                        SeccionResultados(
                            "Dificultad",
                            response.dificultad
                        )
                    )
                    adapterResultados.actualizar(secciones)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    }
    fun abrirDetalleReceta(recetaId: Int) {

        // ==========================================
        // VALIDAR RECETA
        // ==========================================

        if (recetaId <= 0) {
            return
        }


        // ==========================================
        // OCULTAR TECLADO
        // ==========================================

        currentFocus?.let { view ->

            val imm =
                getSystemService(
                    Context.INPUT_METHOD_SERVICE
                ) as InputMethodManager

            imm.hideSoftInputFromWindow(
                view.windowToken,
                0
            )

            view.clearFocus()
        }


        // ==========================================
        // OCULTAR DETALLE DEL PROVEEDOR
        // ==========================================

        binding.containerDetalleProveedor.visibility =
            View.GONE


        // ==========================================
        // MOSTRAR DETALLE DE RECETA
        // ==========================================

        binding.containerDetalleReceta.visibility =
            View.VISIBLE


        // ==========================================
        // CREAR DETALLE DE RECETA
        // ==========================================

        val fragment =
            DetalleRecetaFragment.newInstance(
                recetaId
            )


        // ==========================================
        // ABRIR FRAGMENT
        // ==========================================

        supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.containerDetalleReceta,
                fragment
            )
            .addToBackStack(null)
            .commit()
    }
    private fun cerrarDetalleProveedor() {

        // ==========================================
        // OCULTAR DETALLE DEL PROVEEDOR
        // ==========================================

        binding.containerDetalleProveedor.visibility =
            View.GONE


        // ==========================================
        // MOSTRAR PANEL DE PROVEEDORES
        // ==========================================

        binding.panelProveedores.visibility =
            View.VISIBLE


        // ==========================================
        // ELIMINAR FRAGMENT DEL DETALLE
        // ==========================================

        supportFragmentManager.findFragmentById(
            R.id.containerDetalleProveedor
        )?.let { fragment ->

            supportFragmentManager.beginTransaction()
                .remove(fragment)
                .commit()
        }
    }
    private fun cargarUsuario() {

        val shared =
            getSharedPreferences("user", MODE_PRIVATE)

        val foto =
            shared.getString("foto", null)
        if (!foto.isNullOrEmpty()) {
            Glide.with(this)
                .load(foto)
                .placeholder(R.drawable.ic_icono_usuario)
                .error(R.drawable.ic_icono_usuario)
                .circleCrop()
                .into(binding.imgUsuario)
        }
    }
    private fun actualizarBusquedaCategoria() {
        if (modoActual == Modo.INGREDIENTES) {
            if (ingredientesSeleccionados.isNotEmpty()) {
                cargarRecetasPorIngredientes()
            }
        } else {
            if (recetasSeleccionadas.isNotEmpty()) {
                cargarRecetasPorNombre(
                    recetasSeleccionadas.last().nombre
                )

            }

        }
    }
    private fun cargarInformacionUsuario() {

        val encabezado = binding.navigationView.getHeaderView(0)

        val imgFotoPerfil =
            encabezado.findViewById<ImageView>(R.id.imgFotoPerfil)

        val txtNombreUsuario =
            encabezado.findViewById<TextView>(R.id.txtNombreUsuario)

        val txtNivel =
            encabezado.findViewById<TextView>(R.id.txtNivel)

        val txtExperiencia =
            encabezado.findViewById<TextView>(R.id.txtExperiencia)

        val progresoNivel =
            encabezado.findViewById<ProgressBar>(R.id.progresoNivel)

        //------------------------------------------------------

        val nombre = SesionUsuario.obtenerNombre(this)

        val nivelActual = SesionUsuario.obtenerNivel(this)

        val puntos = SesionUsuario.obtenerPuntos(this)
        Log.d("SESION", "Puntos guardados: $puntos")

        val nivel = obtenerInformacionNivel(nivelActual)

        txtNombreUsuario.text = nombre

        txtNivel.text = nivel.nombre

        if (nivelActual == 5) {

            txtExperiencia.text = "$puntos pts"

            progresoNivel.max = nivel.minimo

            progresoNivel.progress = nivel.minimo

        } else {

            txtExperiencia.text = "$puntos / ${nivel.maximo} pts"

            progresoNivel.max = nivel.maximo

            progresoNivel.progress = puntos

        }

        val foto = SesionUsuario.obtenerFoto(this)

        if (!foto.isNullOrEmpty()) {

            Glide.with(this)
                .load(foto)
                .placeholder(R.drawable.ic_icono_usuario)
                .error(R.drawable.ic_icono_usuario)
                .circleCrop()
                .into(imgFotoPerfil)

        } else {
            imgFotoPerfil.setImageResource(R.drawable.ic_icono_usuario)
        }


    }
    private fun obtenerInformacionNivel(nivel: Int): Nivel {

        return when (nivel) {

            1 -> Nivel("Novato", 0, 250)

            2 -> Nivel("Principiante", 250, 700)

            3 -> Nivel("Experto", 700, 2500)

            4 -> Nivel("Experimentado", 2500, 4000)

            else -> Nivel("Especialista", 4000, Int.MAX_VALUE)
        }

    }
    private fun inicializarMenuLateral() {

        binding.navigationView.setNavigationItemSelectedListener { item ->

            when (item.itemId) {

                R.id.menu_preferencias -> {

                    startActivity(
                        Intent(
                            this,
                            PreferenciasdeCuentaActivity::class.java
                        )
                    )

                }

                R.id.menu_administrar_hogar -> {

                    Toast.makeText(
                        this,
                        "Administrar mi hogar",
                        Toast.LENGTH_SHORT
                    ).show()

                }

                R.id.menu_mis_colecciones -> {

                    startActivity(

                        Intent(
                            this,
                            MisColeccionesActivity::class.java
                        )

                    )

                }



                R.id.menu_planeador_semanal -> {

                    Toast.makeText(
                        this,
                        "Planeador semanal",
                        Toast.LENGTH_SHORT
                    ).show()

                }

                R.id.menu_mi_alacena -> {

                    Toast.makeText(
                        this,
                        "Mi alacena",
                        Toast.LENGTH_SHORT
                    ).show()

                }

                R.id.menu_premium -> {

                    Toast.makeText(
                        this,
                        "GourMeet Premium",
                        Toast.LENGTH_SHORT
                    ).show()

                }

            }

            binding.drawerLayout.closeDrawer(GravityCompat.END)

            true

        }

    }
    private fun abrirBusquedaProveedor() {

        // ==========================================
        // CERRAR MIS COLECCIONES SI ESTÁN ABIERTAS
        // ==========================================

        if (
            binding.rvMisColeccionesProveedores.visibility ==
            View.VISIBLE
        ) {

            cerrarMisColeccionesProveedores()
        }


        val buscador =
            binding.headerProveedores.layoutBusquedaProveedor


        // ==========================================
        // OCULTAR 📍 💾 ⚙
        // ==========================================

        binding.headerProveedores.btnubicaion.visibility =
            View.GONE

        binding.headerProveedores.btnmiscoleccionesprovedor.visibility =
            View.GONE

        binding.headerProveedores.btnfiltros.visibility =
            View.GONE


        // ==========================================
        // MOSTRAR BUSCADOR
        // ==========================================

        buscador.visibility =
            View.VISIBLE


        // ==========================================
        // ANIMACIÓN
        // ==========================================

        buscador.post {

            buscador.translationX =
                -buscador.width.toFloat()

            buscador.animate()
                .translationX(0f)
                .setDuration(300)
                .start()
        }


        busquedaProveedorAbierta = true


        // ==========================================
        // ENFOCAR CAMPO
        // ==========================================

        binding.headerProveedores.edtBuscarProveedor
            .requestFocus()


        // ==========================================
        // MOSTRAR TECLADO
        // ==========================================

        val imm =
            getSystemService(
                Context.INPUT_METHOD_SERVICE
            ) as InputMethodManager

        imm.showSoftInput(
            binding.headerProveedores.edtBuscarProveedor,
            InputMethodManager.SHOW_IMPLICIT
        )
    }
    private fun cerrarBusquedaProveedor() {

        val buscador =
            binding.headerProveedores.layoutBusquedaProveedor


        // ==========================================
        // ANIMACIÓN
        // SALE HACIA LA DERECHA
        // ==========================================

        buscador.animate()
            .translationX(
                buscador.width.toFloat()
            )
            .setDuration(300)
            .withEndAction {

                // ==================================
                // OCULTAR BUSCADOR
                // ==================================

                buscador.visibility =
                    View.GONE

                buscador.translationX =
                    0f


                // ==================================
                // MOSTRAR 📍 💾 ⚙
                // ==================================

                binding.headerProveedores.btnubicaion.visibility =
                    View.VISIBLE

                binding.headerProveedores.btnmiscoleccionesprovedor.visibility =
                    View.VISIBLE

                binding.headerProveedores.btnfiltros.visibility =
                    View.VISIBLE
            }
            .start()


        busquedaProveedorAbierta = false


        // ==========================================
        // OCULTAR TECLADO
        // ==========================================

        val imm =
            getSystemService(
                Context.INPUT_METHOD_SERVICE
            ) as InputMethodManager

        imm.hideSoftInputFromWindow(
            binding.headerProveedores
                .edtBuscarProveedor
                .windowToken,
            0
        )
    }
    private fun cerrarMisColeccionesProveedores() {

        // ==========================================
        // OCULTAR COLECCIONES
        // ==========================================

        binding.rvMisColeccionesProveedores.visibility =
            View.GONE


        // ==========================================
        // MOSTRAR LISTA DE PROVEEDORES
        // ==========================================

        binding.rvProveedores.visibility =
            View.VISIBLE


        // ==========================================
        // SI NO HAY PROVEEDORES
        // ==========================================

        if (listaProveedores.isEmpty()) {

            binding.rvProveedores.visibility =
                View.GONE

            binding.layoutSinProveedores.visibility =
                View.VISIBLE

        } else {

            binding.layoutSinProveedores.visibility =
                View.GONE
        }
    }
    private fun configurarRecyclerProveedores() {

        binding.rvProveedores.apply {

            layoutManager =
                LinearLayoutManager(
                    this@Menu_principal_free,
                    LinearLayoutManager.VERTICAL,
                    false
                )

            adapter =
                adapterSeccionesProveedores

            setHasFixedSize(false)

            overScrollMode =
                View.OVER_SCROLL_NEVER
        }
    }
    private fun inicializarProveedores() {

        adapterSeccionesProveedores =
            SeccionesProveedoresAdapter(
                mutableListOf()
            ) { proveedor ->

                abrirDetalleProveedor(
                    proveedor
                )
            }

        configurarRecyclerProveedores()
    }
    private fun cargarProveedores() {

        lifecycleScope.launch {

            try {

                val response =
                    ApiClient.apiService.listarProveedores()

                if (!response.success) {
                    return@launch
                }

                listaProveedores.clear()

                listaProveedores.addAll(
                    response.proveedores
                )

                // ==========================================
                // CREAR SECCIONES
                // ==========================================

                val secciones =
                    response.categorias.mapNotNull { categoria ->

                        val proveedoresCategoria =
                            response.proveedores.filter { proveedor ->

                                // --------------------------
                                // INGREDIENTES
                                // --------------------------

                                val tieneIngrediente =
                                    proveedor.CATEGORIAS.any { cat ->

                                        cat.trim().equals(
                                            categoria.trim(),
                                            ignoreCase = true
                                        )
                                    }

                                // --------------------------
                                // RECETAS
                                // --------------------------

                                val tieneReceta =
                                    proveedor.RECETAS.isNotEmpty() &&
                                            proveedor.Pro_Des_Giro
                                                ?.trim()
                                                ?.equals(
                                                    categoria.trim(),
                                                    ignoreCase = true
                                                ) == true

                                // --------------------------
                                // PERTENECE A LA SECCIÓN
                                // --------------------------

                                tieneIngrediente || tieneReceta
                            }

                        if (proveedoresCategoria.isNotEmpty()) {

                            SeccionProveedores(
                                categoria = categoria,
                                proveedores = proveedoresCategoria
                            )

                        } else {
                            null
                        }
                    }

                adapterSeccionesProveedores.actualizar(
                    secciones
                )
                mostrarEstadoProveedores(
                        secciones.isNotEmpty()
                        )

            } catch (e: Exception) {

                Log.e(
                    "PROVEEDORES",
                    "Error cargando proveedores",
                    e
                )
            }
        }
    }
    private fun obtenerUbicacionUsuario() {

        // ==========================================
        // VERIFICAR PERMISOS
        // ==========================================

        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                1001
            )

            return
        }


        // ==========================================
        // OBTENER ÚLTIMA UBICACIÓN
        // ==========================================

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->

                if (location != null) {

                    latitudUsuario = location.latitude
                    longitudUsuario = location.longitude

                    Log.d(
                        "UBICACION",
                        "Ubicación obtenida: $latitudUsuario, $longitudUsuario"
                    )

                    adapterSeccionesProveedores.actualizarUbicacion(
                        latitudUsuario!!,
                        longitudUsuario!!
                    )
                }
            }
            // ==========================================
            // ERROR AL OBTENER UBICACIÓN
            // ==========================================
            .addOnFailureListener { error ->
                Log.e(
                    "UBICACION",
                    "Error obteniendo ubicación: ${error.message}",
                    error
                )
            }
    }
    private fun abrirFiltrosProveedores() {

        val panel = binding.panelFiltrosProveedores

        panel.visibility = View.VISIBLE

        panel.post {

            val vista =
                panel.getChildAt(0)

            vista.translationY =
                vista.height.toFloat()

            vista.animate()
                .translationY(0f)
                .setDuration(350)
                .setInterpolator(
                    DecelerateInterpolator()
                )
                .start()
        }
    }
    private fun cerrarFiltrosProveedores() {

        val panel =
            binding.panelFiltrosProveedores

        val vista =
            panel.getChildAt(0)

        vista.animate()
            .translationY(vista.height.toFloat())
            .setDuration(300)
            .setInterpolator(
                AccelerateInterpolator()
            )
            .withEndAction {

                panel.visibility =
                    View.GONE

                vista.translationY = 0f
            }
            .start()
    }
    private fun limpiarFiltrosProveedores() {

        binding.panelFiltrosProveedores
            .findViewById<ChipGroup>(
                R.id.chipGroupDistancia
            )
            .clearCheck()

        binding.panelFiltrosProveedores
            .findViewById<ChipGroup>(
                R.id.chipGroupIngredientes
            )
            .clearCheck()

        binding.panelFiltrosProveedores
            .findViewById<ChipGroup>(
                R.id.chipGroupRecetas
            )
            .clearCheck()


        val secciones =
            crearSeccionesProveedores(
                listaProveedores
            )

        adapterSeccionesProveedores.actualizar(
            secciones
        )

        // Volver a mostrar los proveedores
        mostrarEstadoProveedores(
            secciones.isNotEmpty()
        )

        Log.d(
            "FILTROS",
            "Filtros limpiados"
        )
    }
    private fun aplicarFiltrosProveedores() {

        // ==========================================
        // DISTANCIA
        // ==========================================

        val chipDistanciaSeleccionado =
            binding.panelFiltrosProveedores.findViewById<ChipGroup>(R.id.chipGroupDistancia)
                .checkedChipId


        // ==========================================
        // INGREDIENTES
        // ==========================================

        val categoriasIngredientes =
            obtenerChipsSeleccionados(
                binding.panelFiltrosProveedores.findViewById<ChipGroup>(R.id.chipGroupIngredientes)
            )


        // ==========================================
        // RECETAS
        // ==========================================

        val categoriasRecetas =
            obtenerChipsSeleccionados(
                binding.panelFiltrosProveedores.findViewById<ChipGroup>(R.id.chipGroupRecetas)

            )


        Log.d(
            "FILTROS",
            "Distancia: $chipDistanciaSeleccionado"
        )

        Log.d(
            "FILTROS",
            "Ingredientes: $categoriasIngredientes"
        )

        Log.d(
            "FILTROS",
            "Recetas: $categoriasRecetas"
        )


        // ==========================================
        // FILTRAR
        // ==========================================

        val proveedoresFiltrados =
            listaProveedores.filter { proveedor ->

                cumpleFiltroDistancia(
                    proveedor,
                    chipDistanciaSeleccionado
                ) &&
                        cumpleFiltroIngredientes(
                            proveedor,
                            categoriasIngredientes
                        ) &&
                        cumpleFiltroRecetas(
                            proveedor,
                            categoriasRecetas
                        )
            }


        // ==========================================
        // ACTUALIZAR RECYCLER
        // ==========================================

        val seccionesFiltradas =
            crearSeccionesProveedores(
                proveedoresFiltrados
            )

        adapterSeccionesProveedores.actualizar(
            seccionesFiltradas
        )

// Mostrar RecyclerView o mensaje de "sin resultados"
        mostrarEstadoProveedores(
            seccionesFiltradas.isNotEmpty()
        )

        cerrarFiltrosProveedores()


        Log.d(
            "FILTROS",
            "Proveedores encontrados: ${proveedoresFiltrados.size}"
        )
    }
    private fun obtenerChipsSeleccionados(
        chipGroup: ChipGroup
    ): List<String> {

        return chipGroup.children
            .filter { it is Chip && it.isChecked }
            .map { (it as Chip).text.toString() }
            .toList()
    }
    private fun cumpleFiltroIngredientes(
        proveedor: Proveedor,
        categoriasSeleccionadas: List<String>
    ): Boolean {

        // No hay filtro
        if (categoriasSeleccionadas.isEmpty()) {
            return true
        }

        return proveedor.INGREDIENTES.any { ingrediente ->

            categoriasSeleccionadas.any { categoria ->

                ingrediente.CATEGORIA
                    ?.trim()
                    ?.equals(
                        categoria.trim(),
                        ignoreCase = true
                    ) == true
            }
        }
    }
    private fun cumpleFiltroRecetas(
        proveedor: Proveedor,
        categoriasSeleccionadas: List<String>
    ): Boolean {

        // No hay filtro
        if (categoriasSeleccionadas.isEmpty()) {
            return true
        }

        return proveedor.RECETAS.any { receta ->

            categoriasSeleccionadas.any { categoria ->

                receta.CATEGORIA
                    ?.trim()
                    ?.equals(
                        categoria.trim(),
                        ignoreCase = true
                    ) == true
            }
        }
    }
    private fun cumpleFiltroDistancia(
        proveedor: Proveedor,
        chipDistancia: Int
    ): Boolean {

        // ==========================================
        // SIN FILTRO DE DISTANCIA
        // ==========================================

        if (chipDistancia == View.NO_ID) {
            return true
        }

        // ==========================================
        // VERIFICAR UBICACIÓN DEL USUARIO
        // ==========================================

        val latUsuario = latitudUsuario
        val lonUsuario = longitudUsuario

        if (latUsuario == null || lonUsuario == null) {

            Log.d(
                "FILTROS",
                "No hay ubicación del usuario"
            )

            return false
        }

        // ==========================================
        // UBICACIÓN DEL PROVEEDOR
        // ==========================================

        val latProveedor =
            proveedor.Pro_Latitud
                ?.toDoubleOrNull()

        val lonProveedor =
            proveedor.Pro_Longitud
                ?.toDoubleOrNull()

        if (
            latProveedor == null ||
            lonProveedor == null
        ) {

            return false
        }

        // ==========================================
        // CALCULAR DISTANCIA EN METROS
        // ==========================================

        val ubicacionUsuario =
            Location("usuario").apply {
                latitude = latUsuario
                longitude = lonUsuario
            }

        val ubicacionProveedor =
            Location("proveedor").apply {
                latitude = latProveedor
                longitude = lonProveedor
            }

        val distanciaKm =
            ubicacionUsuario.distanceTo(
                ubicacionProveedor
            ) / 1000.0

        // ==========================================
        // COMPARAR CON EL CHIP
        // ==========================================

        return when (chipDistancia) {

            R.id.chip0a5 -> {
                distanciaKm >= 0 &&
                        distanciaKm <= 5
            }

            R.id.chip5a10 -> {
                distanciaKm > 5 &&
                        distanciaKm <= 10
            }

            R.id.chip10a15 -> {
                distanciaKm > 10 &&
                        distanciaKm <= 15
            }

            R.id.chip15mas -> {
                distanciaKm > 15
            }

            else -> true
        }
    }
    private fun crearSeccionesProveedores(
        proveedores: List<Proveedor>
    ): List<SeccionProveedores> {

        val secciones = mutableListOf<SeccionProveedores>()

        // ==========================================
        // OBTENER CATEGORÍAS
        // ==========================================

        val categorias = mutableListOf<String>()

        for (proveedor in proveedores) {

            // --------------------------
            // CATEGORÍAS DE INGREDIENTES
            // --------------------------

            proveedor.CATEGORIAS.forEach { categoria ->

                val categoriaLimpia =
                    categoria.trim()

                if (
                    categoriaLimpia.isNotEmpty() &&
                    !categorias.any {
                        it.equals(
                            categoriaLimpia,
                            ignoreCase = true
                        )
                    }
                ) {

                    categorias.add(
                        categoriaLimpia
                    )
                }
            }

            // --------------------------
            // CATEGORÍAS DE RECETAS
            // --------------------------

            proveedor.CATEGORIAS_RECETAS.forEach { categoria ->

                val categoriaLimpia =
                    categoria.trim()

                if (
                    categoriaLimpia.isNotEmpty() &&
                    !categorias.any {
                        it.equals(
                            categoriaLimpia,
                            ignoreCase = true
                        )
                    }
                ) {

                    categorias.add(
                        categoriaLimpia
                    )
                }
            }
        }

        // ==========================================
        // CREAR CADA SECCIÓN
        // ==========================================

        for (categoria in categorias) {

            val proveedoresCategoria =
                proveedores.filter { proveedor ->

                    // --------------------------
                    // INGREDIENTES
                    // --------------------------

                    val tieneIngrediente =
                        proveedor.INGREDIENTES.any { ingrediente ->

                            ingrediente.CATEGORIA
                                ?.trim()
                                ?.equals(
                                    categoria.trim(),
                                    ignoreCase = true
                                ) == true
                        }

                    // --------------------------
                    // RECETAS
                    // --------------------------

                    val tieneReceta =
                        proveedor.RECETAS.any { receta ->

                            receta.CATEGORIA
                                ?.trim()
                                ?.equals(
                                    categoria.trim(),
                                    ignoreCase = true
                                ) == true
                        }

                    tieneIngrediente || tieneReceta
                }

            if (proveedoresCategoria.isNotEmpty()) {

                secciones.add(
                    SeccionProveedores(
                        categoria = categoria,
                        proveedores = proveedoresCategoria
                    )
                )
            }
        }

        return secciones
    }
    private fun mostrarEstadoProveedores(
        hayResultados: Boolean
    ) {

        if (hayResultados) {

            binding.rvProveedores.visibility =
                View.VISIBLE

            binding.layoutSinProveedores.visibility =
                View.GONE

        } else {

            binding.rvProveedores.visibility =
                View.GONE

            binding.layoutSinProveedores.visibility =
                View.VISIBLE
        }
    }
    private fun configurarBusquedaProveedores() {

        binding.headerProveedores.edtBuscarProveedor
            .doAfterTextChanged { textoEditable ->

                val texto =
                    textoEditable
                        ?.toString()
                        ?.trim()
                        ?: ""

                // ==========================================
                // SIN TEXTO
                // ==========================================

                if (texto.isEmpty()) {

                    val secciones =
                        crearSeccionesProveedores(
                            listaProveedores
                        )

                    adapterSeccionesProveedores.actualizar(
                        secciones
                    )

                    mostrarEstadoProveedores(
                        listaProveedores.isNotEmpty()
                    )

                    return@doAfterTextChanged
                }


                // ==========================================
                // BUSCAR PROVEEDORES
                // ==========================================

                val proveedoresFiltrados =
                    listaProveedores.filter { proveedor ->

                        // ----------------------------------
                        // 1. BUSCAR POR NOMBRE DEL PROVEEDOR
                        // ----------------------------------

                        val coincideNombreProveedor =
                            proveedor.Pro_nombre
                                ?.contains(
                                    texto,
                                    ignoreCase = true
                                ) == true


                        // ----------------------------------
                        // 2. BUSCAR POR INGREDIENTE
                        //    INGREDIENTES.NOMBRE
                        // ----------------------------------

                        val coincideIngrediente =
                            proveedor.INGREDIENTES.any { ingrediente ->

                                ingrediente.NOMBRE
                                    ?.contains(
                                        texto,
                                        ignoreCase = true
                                    ) == true
                            }


                        // ----------------------------------
                        // 3. BUSCAR POR CATEGORÍA
                        //    INGREDIENTES.CATEGORIA
                        // ----------------------------------

                        val coincideCategoriaIngrediente =
                            proveedor.INGREDIENTES.any { ingrediente ->

                                ingrediente.CATEGORIA
                                    ?.contains(
                                        texto,
                                        ignoreCase = true
                                    ) == true
                            }
                        // ----------------------------------
                        // 4. BUSCAR POR CATEGORÍA
                        //    Recetas.nombre
                        // ----------------------------------
                        val coincideRecetaNombre =
                            proveedor.RECETAS.any{ receta ->
                                receta.NOMBRE
                                    ?.contains(
                                        texto,
                                        ignoreCase = true
                                    )== true


                            }


                        // ----------------------------------
                        // EL PROVEEDOR COINCIDE SI CUALQUIERA
                        // DE LOS TRES CRITERIOS SE CUMPLE
                        // ----------------------------------

                        coincideNombreProveedor ||
                                coincideIngrediente ||
                                coincideCategoriaIngrediente ||
                                coincideRecetaNombre
                    }


                // ==========================================
                // LOG
                // ==========================================

                Log.d(
                    "BUSQUEDA_PROVEEDOR",
                    "Texto: $texto"
                )

                Log.d(
                    "BUSQUEDA_PROVEEDOR",
                    "Resultados: ${proveedoresFiltrados.size}"
                )


                // ==========================================
                // MOSTRAR RESULTADOS
                // ==========================================

                if (proveedoresFiltrados.isNotEmpty()) {

                    val secciones =
                        crearSeccionesProveedores(
                            proveedoresFiltrados
                        )

                    adapterSeccionesProveedores.actualizar(
                        secciones
                    )

                    mostrarEstadoProveedores(
                        true
                    )

                } else {

                    // ==========================================
                    // SIN RESULTADOS
                    // ==========================================

                    adapterSeccionesProveedores.actualizar(
                        emptyList()
                    )

                    mostrarEstadoProveedores(
                        false
                    )
                }
            }
    }
    private fun abrirDetalleProveedor(
        proveedor: Proveedor
    ) {

        // ==========================================
        // OCULTAR TECLADO
        // ==========================================

        currentFocus?.let { view ->

            val imm =
                getSystemService(
                    Context.INPUT_METHOD_SERVICE
                ) as InputMethodManager

            imm.hideSoftInputFromWindow(
                view.windowToken,
                0
            )

            view.clearFocus()
        }


        // ==========================================
        // MOSTRAR CONTENEDOR
        // ==========================================

        binding.containerDetalleProveedor.visibility =
            View.VISIBLE


        // ==========================================
        // CREAR FRAGMENT
        // ==========================================

        val fragment =
            DetalleProveedorFragment.newInstance(

                proveedor.Id_Proveedor.toString(),

                latitudUsuario,

                longitudUsuario
            )


        // ==========================================
        // MOSTRAR DETALLE
        // ==========================================

        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(
                R.id.containerDetalleProveedor,
                fragment
            )
            .addToBackStack(
                "detalle_proveedor"
            )
            .commit()
    }
    private fun mostrarMisColeccionesProveedores() {

        // ==========================================
        // CERRAR BUSCADOR SI ESTÁ ABIERTO
        // ==========================================

        if (busquedaProveedorAbierta) {

            cerrarBusquedaProveedor()
        }


        // ==========================================
        // CERRAR FILTROS SI ESTÁN ABIERTOS
        // ==========================================

        if (
            binding.panelFiltrosProveedores.visibility ==
            View.VISIBLE
        ) {

            cerrarFiltrosProveedores()

            binding.headerProveedores.btnfiltros
                .setImageResource(
                    R.drawable.ic_filtro
                )
        }


        // ==========================================
        // OCULTAR LISTA NORMAL
        // ==========================================

        binding.rvProveedores.visibility =
            View.GONE

        binding.layoutSinProveedores.visibility =
            View.GONE


        // ==========================================
        // MOSTRAR MIS COLECCIONES
        // ==========================================

        binding.rvMisColeccionesProveedores.visibility =
            View.VISIBLE


        // ==========================================
        // CONFIGURAR RECYCLER
        // ==========================================

        binding.rvMisColeccionesProveedores.apply {

            layoutManager =
                LinearLayoutManager(
                    this@Menu_principal_free,
                    LinearLayoutManager.VERTICAL,
                    false
                )

            setHasFixedSize(false)

            isNestedScrollingEnabled =
                true

            overScrollMode =
                RecyclerView.OVER_SCROLL_NEVER
        }


        // ==========================================
        // CARGAR COLECCIONES
        // ==========================================

        cargarColeccionesProveedores()
    }
    private fun cargarColeccionesProveedores() {

        // ==========================================
        // OBTENER USUARIO
        // ==========================================

        val clienteId =
            SesionUsuario.obtenerId(this)

        if (clienteId <= 0) {

            Toast.makeText(
                this,
                "Debes iniciar sesión.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        // ==========================================
        // CREAR PETICIÓN
        // ==========================================

        val datos =
            ConsultarColeccionesProveedor(
                CLI_ID = clienteId
            )


        // ==========================================
        // CONSULTAR API
        // ==========================================

        lifecycleScope.launch {

            try {

                val respuesta =
                    ApiClient.apiService
                        .listarColeccionesProveedores(
                            datos
                        )


                // ======================================
                // RESPUESTA
                // ======================================

                if (respuesta.success) {

                    val colecciones =
                        respuesta.colecciones
                            ?: emptyList()


                    // ==================================
                    // MOSTRAR COLECCIONES
                    // ==================================

                    mostrarColeccionesProveedores(
                        colecciones
                    )


                    // ==================================
                    // SI NO HAY COLECCIONES
                    // ==================================

                    if (colecciones.isEmpty()) {

                        Toast.makeText(
                            this@Menu_principal_free,
                            "Aún no tienes colecciones de proveedores.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }


                } else {

                    Toast.makeText(
                        this@Menu_principal_free,
                        respuesta.mensaje
                            ?: "No se pudieron cargar las colecciones.",
                        Toast.LENGTH_SHORT
                    ).show()
                }


            } catch (e: Exception) {

                e.printStackTrace()

                Toast.makeText(
                    this@Menu_principal_free,
                    "Error al consultar las colecciones.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    private fun mostrarColeccionesProveedores(
        colecciones: List<ColeccionProveedor>
    ) {

        val adapter =
            ColeccionesProveedoresAdapter(
                colecciones
            ) { proveedor ->

                // ======================================
                // CLICK EN PROVEEDOR
                // ======================================

                abrirDetalleProveedor(
                    proveedor
                )
            }


        binding.rvMisColeccionesProveedores.apply {

            layoutManager =
                LinearLayoutManager(
                    this@Menu_principal_free,
                    LinearLayoutManager.VERTICAL,
                    false
                )

            this.adapter =
                adapter

            setHasFixedSize(false)

            isNestedScrollingEnabled =
                true

            overScrollMode =
                View.OVER_SCROLL_NEVER
        }
    }
    fun cerrarDetalleReceta() {

        binding.containerDetalleReceta.visibility =
            View.GONE

        binding.containerDetalleProveedor.visibility =
            View.VISIBLE

        supportFragmentManager.popBackStack()
    }
}