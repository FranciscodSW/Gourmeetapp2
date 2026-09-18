package com.example.gourmeet2

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
import android.app.Dialog
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ScrollView
import androidx.core.view.children
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import androidx.core.widget.doAfterTextChanged
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ListView
import android.widget.PopupWindow
import android.widget.Toast.makeText
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.example.gourmeet2.ui.adapters.IngredienteMiniAdapter
import com.example.gourmeet2.utils.SesionUsuario.actualizarNombre
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.collections.emptyList

class Menu_principal_free : AppCompatActivity() {
    private var menuAbierto = false
    private lateinit var binding: ActivityMenuPrincipalFreeBinding
    private var modoActual = Modo.INGREDIENTES
    private var nombreUsuarioActual = ""
    enum class Modo {INGREDIENTES,RECETAS }
    private val listaProveedores = mutableListOf<Proveedor>()
    private lateinit var adapterProveedores: ProveedorAdapter
    enum class Seccion { BUSCADOR, ALACENA, PLANEADOR,LISTA_DE_COMPRAS}
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
    private val listaAlacenas = mutableListOf<Alacena>()
    private var alacenaSeleccionada: Alacena? = null
    private var ingredienteSeleccionadoId: Int? = null
    private lateinit var adapterConsumePrimero: IngredienteAlacenaAdapter
    private lateinit var adapterMisIngredientes: IngredienteAlacenaAdapter
    private lateinit var adapterCategoriasIngredientes: CategoriaIngredientesAdapter
    private enum class ModoIngrediente {
        AGREGAR,
        EDITAR,
        USAR_EN_RECETA
    }

    private val solicitarPermisosUbicacion =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permisos ->

            val fineLocation =
                permisos[
                    Manifest.permission.ACCESS_FINE_LOCATION
                ] ?: false

            val coarseLocation =
                permisos[
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ] ?: false


            Log.d(
                "UBICACION",
                "RESULTADO FINE = $fineLocation"
            )

            Log.d(
                "UBICACION",
                "RESULTADO COARSE = $coarseLocation"
            )


            // ==========================================
            // PERMISO CONCEDIDO
            // ==========================================

            if (fineLocation || coarseLocation) {

                Log.d(
                    "UBICACION",
                    "PERMISO CONCEDIDO"
                )

                obtenerUbicacionUsuario()

                return@registerForActivityResult
            }


            // ==========================================
            // PERMISO RECHAZADO
            // ==========================================

            Log.d(
                "UBICACION",
                "PERMISO RECHAZADO"
            )


            val puedeVolverAPreguntar =
                ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) ||
                        ActivityCompat.shouldShowRequestPermissionRationale(
                            this,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )


            if (puedeVolverAPreguntar) {

                // ======================================
                // TODAVÍA PODEMOS VOLVER A SOLICITAR
                // ======================================

                mostrarDialogoPermisoUbicacion()

            } else {

                // ======================================
                // ANDROID YA NO MOSTRARÁ EL PERMISO
                // ======================================

                mostrarDialogoIrConfiguracion()
            }
        }
    var familiaIngredienteSeleccionado: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding =
            ActivityMenuPrincipalFreeBinding.inflate(layoutInflater)

        setContentView(binding.root)

        // =========================================================
        // BOTÓN ATRÁS
        // =========================================================

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
                    // PREFERENCIAS DE CUENTA
                    // ======================================

                    if (
                        binding.panelPreferenciasCuenta.visibility ==
                        View.VISIBLE
                    ) {

                        cerrarPreferenciasCuenta()

                        return
                    }


                    // ======================================
                    // SI NO HAY NINGÚN PANEL ABIERTO
                    // ======================================

                    isEnabled = false

                    onBackPressedDispatcher.onBackPressed()
                }
            }
        )


        // =========================================================
        // INICIALIZACIÓN GENERAL
        // =========================================================

        cargarUsuario()

        cargarInformacionUsuario()

        inicializarMenuLateral()

        configurarPreferenciasCuenta()


        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)


        inicializarProveedores()

        configurarBusquedaProveedores()


        // =========================================================
        // UBICACIÓN
        // =========================================================

        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)


        inicializarProveedores()

        configurarBusquedaProveedores()


        if (tienePermisoUbicacion()) {

            obtenerUbicacionUsuario()
        }


        // =========================================================
        // BACK STACK
        // =========================================================

        supportFragmentManager.addOnBackStackChangedListener {

            if (
                supportFragmentManager.backStackEntryCount == 0
            ) {

                binding.containerDetalleReceta.visibility =
                    View.GONE

                binding.containerDetalleProveedor.visibility =
                    View.GONE
            }
        }


        // =========================================================
        // MODO ACTUAL
        // =========================================================

        actualizarModo()


        // =========================================================
        // BOTÓN AGREGAR INGREDIENTE
        // =========================================================

        val btnAgregarIngrediente =
            binding.panelAlacena.findViewById<MaterialButton>(
                R.id.btnAgregarIngrediente
            )


        btnAgregarIngrediente.setOnClickListener {

            mostrarDialogAgregarIngrediente()
        }


        // =========================================================
        // BUSCADOR
        // =========================================================

        binding.editBusqueda.setOnEditorActionListener { _, _, _ ->

            true
        }


        binding.editBusqueda.addTextChangedListener {

            val texto = it.toString()


            if (texto.length >= 2) {

                if (
                    modoActual == Modo.INGREDIENTES
                ) {

                    buscarIngredientes(texto)

                } else {

                    buscarRecetas(texto)
                }
            }
        }


        // =========================================================
        // BOTÓN PROVEEDORES
        // =========================================================

        binding.btnProveedores.setOnClickListener {

            // ==========================================
            // CERRAR PANEL
            // ==========================================

            if (
                binding.panelProveedores.visibility ==
                View.VISIBLE
            ) {

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

                return@setOnClickListener
            }


            // ==========================================
            // VERIFICAR UBICACIÓN
            // ==========================================

            if (!tienePermisoUbicacion()) {

                mostrarDialogoPermisoUbicacion()

                return@setOnClickListener
            }


            // ==========================================
            // CERRAR MIS COLECCIONES
            // ==========================================

            binding.rvMisColeccionesProveedores.visibility =
                View.GONE


            // ==========================================
            // MOSTRAR LISTA NORMAL
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
            // ABRIR PANEL
            // ==========================================

            binding.panelProveedores.visibility =
                View.VISIBLE


            binding.panelProveedores.post {

                binding.panelProveedores.translationX =
                    -binding.panelProveedores.width.toFloat()


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


        // =========================================================
        // BLOQUEAR ENTER
        // =========================================================

        binding.editBusqueda.setOnKeyListener { _, keyCode, event ->

            if (
                keyCode == KeyEvent.KEYCODE_ENTER &&
                event.action == KeyEvent.ACTION_DOWN
            ) {

                true

            } else {

                false
            }
        }


        // =========================================================
        // ADAPTER DE INGREDIENTES
        // =========================================================

        adapter =
            IngredienteAdapter(
                emptyList()
            ) { ingrediente ->

                moverASeleccionados(ingrediente)

                binding.rvResultados.visibility =
                    View.GONE

                binding.panelingredietes.visibility =
                    View.GONE

                binding.editBusqueda.setText("")
            }


        // =========================================================
        // ADAPTER DE RESULTADOS
        // =========================================================

        adapterResultados =
            AdapterResultados(
                mutableListOf()
            ) { receta ->

                abrirDetalleReceta(
                    receta.REC_ID
                )
            }


        // =========================================================
        // ADAPTER CONSUME PRIMERO
        // =========================================================

        adapterConsumePrimero =
            IngredienteAlacenaAdapter(
                emptyList()
            ) { ingredienteSeleccionado ->

                mostrarDialogAgregarIngrediente(
                    ingredienteSeleccionado
                )
            }


        // =========================================================
        // ADAPTER MIS INGREDIENTES
        // =========================================================

        adapterMisIngredientes =
            IngredienteAlacenaAdapter(
                emptyList()
            ) { ingredienteSeleccionado ->

                mostrarDialogAgregarIngrediente(
                    ingredienteSeleccionado
                )
            }


        // =========================================================
        // ADAPTER CATEGORÍAS
        // =========================================================

        adapterCategoriasIngredientes =
            CategoriaIngredientesAdapter(
                emptyList()
            ) { ingredienteSeleccionado ->

                mostrarDialogAgregarIngrediente(
                    ingredienteSeleccionado
                )
            }


        // =========================================================
        // RECYCLER VIEWS DE ALACENA
        // =========================================================

        val rvConsumePrimero =
            binding.panelAlacena.findViewById<RecyclerView>(
                R.id.rvConsumePrimero
            )


        val rvMisIngredientes =
            binding.panelAlacena.findViewById<RecyclerView>(
                R.id.rvMisIngredientes
            )


        // =========================================================
        // CONSUME PRIMERO
        // =========================================================

        rvConsumePrimero.layoutManager =
            LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL,
                false
            )


        rvConsumePrimero.adapter =
            adapterConsumePrimero


        // =========================================================
        // MIS INGREDIENTES POR CATEGORÍA
        // =========================================================

        rvMisIngredientes.layoutManager =
            LinearLayoutManager(
                this,
                LinearLayoutManager.VERTICAL,
                false
            )


        rvMisIngredientes.adapter =
            adapterCategoriasIngredientes


        // =========================================================
        // RECYCLER PRINCIPAL
        // =========================================================

        binding.rvPrincipal.layoutManager =
            LinearLayoutManager(this)


        binding.rvPrincipal.adapter =
            adapterResultados


        binding.rvPrincipal.layoutManager =
            LinearLayoutManager(this)


        binding.rvPrincipal.adapter =
            adapterResultados


        // =========================================================
        // RESULTADOS DE BÚSQUEDA
        // =========================================================

        binding.rvResultados.layoutManager =
            GridLayoutManager(
                this,
                1,
                GridLayoutManager.HORIZONTAL,
                false
            )


        binding.rvResultados.adapter =
            adapter


        // =========================================================
        // CARD CENTRO
        // =========================================================

        binding.cardCentro.setOnClickListener {

            cambiarSeccion()
        }


        // =========================================================
        // ALACENA
        // =========================================================

        binding.panelAlacena
            .findViewById<MaterialButton>(
                R.id.actAlacena
            )
            .setOnClickListener {

                mostrarMenuMisAlacenas()
            }


        // =========================================================
        // CAMBIAR MODO INGREDIENTES / RECETAS
        // =========================================================

        binding.opModo.setOnClickListener {

            if (
                modoActual == Modo.INGREDIENTES
            ) {

                modoActual = Modo.RECETAS

                binding.txtTitulo.text =
                    "Recetas"

                buscarRecetas(
                    textoBusqueda
                )

            } else {

                modoActual = Modo.INGREDIENTES

                binding.txtTitulo.text =
                    "Ingredientes"

                buscarIngredientes(
                    textoBusqueda
                )
            }


            actualizarModo()

            actualizarTextoBuscador()

            actualizarBusquedaCategoria()
        }


        // =========================================================
        // FLECHA DEL MENÚ
        // =========================================================

        binding.imgFlecha.setOnClickListener {

            if (!menuAbierto) {

                mostrarMenuAnimado()

            } else {

                ocultarMenuAnimado()
            }


            menuAbierto = !menuAbierto
        }


        // =========================================================
        // SNACK
        // =========================================================

        binding.opSnack.setOnClickListener {

            categoriaSeleccionada = 1

            cambiarEncabezado(
                "Snack",
                R.drawable.ic_logo_morado
            )

            actualizarBusquedaCategoria()
        }


        // =========================================================
        // BEBIDA
        // =========================================================

        binding.opBebida.setOnClickListener {

            categoriaSeleccionada = 2

            cambiarEncabezado(
                "Bebida",
                R.drawable.ic_logo_naranja
            )

            actualizarBusquedaCategoria()
        }


        // =========================================================
        // PLATO FUERTE
        // =========================================================

        binding.opPlatoFuerte.setOnClickListener {

            categoriaSeleccionada = 3

            cambiarEncabezado(
                "Plato fuerte",
                R.drawable.ic_logo_azul
            )

            actualizarBusquedaCategoria()
        }


        // =========================================================
        // POSTRE
        // =========================================================

        binding.opPostre.setOnClickListener {

            categoriaSeleccionada = 4

            cambiarEncabezado(
                "Postre",
                R.drawable.ic_logo_rosa
            )

            actualizarBusquedaCategoria()
        }


        // =========================================================
        // ENTRADA
        // =========================================================

        binding.opEntrada.setOnClickListener {

            categoriaSeleccionada = 5

            cambiarEncabezado(
                "Entrada",
                R.drawable.ic_logo_verde
            )

            actualizarBusquedaCategoria()
        }


        // =========================================================
        // EXPANDIR BÚSQUEDA
        // =========================================================

        binding.barraExpandirBusqueda.setOnClickListener {

            if (!panelBusquedaAbierto) {

                abrirPanelBusqueda()

            } else {

                cerrarPanelBusqueda()
            }


            panelBusquedaAbierto =
                !panelBusquedaAbierto
        }


        // =========================================================
        // BÚSQUEDA DE PROVEEDORES
        // =========================================================

        binding.headerProveedores.btnbuscar.setOnClickListener {

            if (busquedaProveedorAbierta) {

                cerrarBusquedaProveedor()

            } else {

                abrirBusquedaProveedor()
            }
        }


        // =========================================================
        // MAPA DE PROVEEDORES
        // =========================================================

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
                            id =
                                proveedor.Id_Proveedor.toString(),

                            nombre =
                                proveedor.Pro_nombre
                                    ?: "Proveedor",

                            latitud =
                                latitud,

                            longitud =
                                longitud,

                            fotoPerfil =
                                proveedor.Pro_Foto_Perfil
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


        // =========================================================
        // MIS COLECCIONES DE PROVEEDORES
        // =========================================================

        binding.headerProveedores.btnmiscoleccionesprovedor
            .setOnClickListener {

                mostrarMisColeccionesProveedores()
            }


        // =========================================================
        // FILTROS DE PROVEEDORES
        // =========================================================

        binding.headerProveedores.btnfiltros.setOnClickListener {

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

                if (
                    binding.rvMisColeccionesProveedores.visibility ==
                    View.VISIBLE
                ) {

                    cerrarMisColeccionesProveedores()
                }


                binding.headerProveedores.btnfiltros
                    .setImageResource(
                        R.drawable.ic_filtro_on
                    )


                abrirFiltrosProveedores()
            }
        }


        // =========================================================
        // CERRAR FILTROS
        // =========================================================

        binding.panelFiltrosProveedores
            .findViewById<ImageView>(
                R.id.btnCerrarFiltros
            )
            .setOnClickListener {

                cerrarFiltrosProveedores()
            }


        // =========================================================
        // LIMPIAR FILTROS
        // =========================================================

        binding.panelFiltrosProveedores
            .findViewById<MaterialButton>(
                R.id.btnLimpiarFiltros
            )
            .setOnClickListener {

                limpiarFiltrosProveedores()
            }


        // =========================================================
        // APLICAR FILTROS
        // =========================================================

        binding.panelFiltrosProveedores
            .findViewById<MaterialButton>(
                R.id.btnAplicarFiltros
            )
            .setOnClickListener {

                aplicarFiltrosProveedores()
            }


        // =========================================================
        // INGREDIENTES SELECCIONADOS
        // =========================================================

        seleccionadosAdapter =
            SeleccionadosAdapter(
                ingredientesSeleccionados
            ) { ingrediente ->

                ingredientesSeleccionados.remove(
                    ingrediente
                )

                seleccionadosAdapter.notifyDataSetChanged()
            }


        binding.rvSeleccionados.layoutManager =
            GridLayoutManager(
                this,
                1,
                GridLayoutManager.HORIZONTAL,
                false
            )


        binding.rvSeleccionados.adapter =
            seleccionadosAdapter


        // =========================================================
        // TECLADO
        // =========================================================

        ViewCompat.setOnApplyWindowInsetsListener(
            binding.root
        ) { _, insets ->

            val imeHeight =
                insets.getInsets(
                    WindowInsetsCompat.Type.ime()
                ).bottom.toFloat()


            val extraOffset = 50f


            binding.panelBusqueda.translationY =
                -(imeHeight - extraOffset)


            binding.panelingredietes.translationY =
                -(imeHeight - extraOffset)


            insets
        }


        // =========================================================
        // PERFIL
        // =========================================================

        binding.btnPerfil.setOnClickListener {

            cargarInformacionUsuario()

            binding.drawerLayout.openDrawer(
                GravityCompat.END
            )
        }


        // =========================================================
        // RECETAS INICIO
        // =========================================================

        cargarRecetasInicio()
    }
    companion object {

        private const val REQUEST_PERMISO_UBICACION = 1001

    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )


        if (
            requestCode ==
            REQUEST_PERMISO_UBICACION
        ) {

            if (
                grantResults.isNotEmpty() &&
                grantResults.any {
                    it == PackageManager.PERMISSION_GRANTED
                }
            ) {

                // ======================================
                // PERMISO CONCEDIDO
                // ======================================

                obtenerUbicacionUsuario()

                cargarProveedores()

            } else {

                // ======================================
                // PERMISO RECHAZADO
                // ======================================

                makeText(
                    this,
                    "Necesitamos tu ubicación para mostrar proveedores cercanos.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
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

            // ==========================================
            // BUSCADOR → ALACENA
            // ==========================================

            Seccion.BUSCADOR -> {

                seccionActual = Seccion.ALACENA

                binding.txtSeccionActual.text = "Alacena"

                mostrarSeccionAlacena()
            }


            // ==========================================
            // ALACENA → PLANEADOR
            // ==========================================

            Seccion.ALACENA -> {

                seccionActual = Seccion.PLANEADOR

                binding.txtSeccionActual.text =
                    "Planeador semanal"

                ocultarSeccionAlacena()

                mostrarSeccionBuscador()
            }


            // ==========================================
            // PLANEADOR → BUSCADOR
            // ==========================================

            Seccion.PLANEADOR -> {

                seccionActual = Seccion.BUSCADOR

                actualizarTextoBuscador()

                mostrarSeccionBuscador()
            }


            // ==========================================
            // LISTA DE COMPRAS
            // ==========================================

            Seccion.LISTA_DE_COMPRAS -> {

                seccionActual = Seccion.LISTA_DE_COMPRAS

                binding.txtSeccionActual.text =
                    "Lista de compras"
            }
        }
    }
    private fun mostrarSeccionAlacena() {

        binding.rvPrincipal.visibility = View.GONE
        binding.panelingredietes.visibility = View.GONE
        binding.panelBusqueda.visibility = View.GONE

        binding.panelAlacena.visibility = View.VISIBLE

        binding.panelAlacena.bringToFront()
        binding.barraInferior.bringToFront()

        cargarAlacenas()
    }
    private fun ocultarSeccionAlacena() {

        binding.panelAlacena.visibility =
            View.GONE
    }
    private fun mostrarSeccionBuscador() {

        // ==========================================
        // OCULTAR ALACENA
        // ==========================================

        binding.panelAlacena.visibility =
            View.GONE


        // ==========================================
        // MOSTRAR BUSCADOR
        // ==========================================

        binding.rvPrincipal.visibility =
            View.VISIBLE

        binding.panelBusqueda.visibility =
            View.VISIBLE
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

            Log.d(
                "MENU_LATERAL",
                "ITEM SELECCIONADO: ${item.itemId} - ${item.title}"
            )

            when (item.itemId) {

                R.id.menu_preferencias -> {
                    Log.d(
                        "PREFERENCIAS",
                        "ABRIENDO PREFERENCIAS"
                    )
                    abrirPreferenciasCuenta()
                    // IMPORTANTE:
                    // no cerramos el drawer
                    return@setNavigationItemSelectedListener true
                }

                R.id.menu_administrar_hogar -> {

                    makeText(
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

                    makeText(
                        this,
                        "Planeador semanal",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                R.id.menu_mi_alacena -> {

                    val intent = Intent(
                        this,
                        MiAlacenaActivity::class.java
                    )

                    startActivity(intent)

                    true
                }

                R.id.menu_premium -> {

                    startActivity(
                        Intent(
                            this,
                            PremiumActivity::class.java
                        )
                    )
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
        binding.panelPreferenciasCuenta
            .findViewById<ImageView>(
                R.id.btnRegresar
            )
            .setOnClickListener {

                cerrarPreferenciasCuenta()
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

            makeText(
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

                        makeText(
                            this@Menu_principal_free,
                            "Aún no tienes colecciones de proveedores.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }


                } else {

                    makeText(
                        this@Menu_principal_free,
                        respuesta.mensaje
                            ?: "No se pudieron cargar las colecciones.",
                        Toast.LENGTH_SHORT
                    ).show()
                }


            } catch (e: Exception) {

                e.printStackTrace()

                makeText(
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
    private fun tienePermisoUbicacion(): Boolean {

        val fineLocation =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED


        val coarseLocation =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED


        Log.d(
            "UBICACION",
            "FINE actual = $fineLocation"
        )

        Log.d(
            "UBICACION",
            "COARSE actual = $coarseLocation"
        )


        return fineLocation || coarseLocation
    }
    private fun mostrarDialogoIrConfiguracion() {

        AlertDialog.Builder(this)
            .setTitle("Permiso de ubicación")
            .setMessage(
                "Para mostrar proveedores cercanos necesitamos " +
                        "acceder a tu ubicación.\n\n" +
                        "Activa el permiso desde:\n\n" +
                        "Configuración → Aplicaciones → GourMeet → " +
                        "Permisos → Ubicación."
            )
            .setNegativeButton(
                "Cancelar",
                null
            )
            .setPositiveButton(
                "Abrir configuración"
            ) { _, _ ->

                val intent =
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    ).apply {

                        data =
                            Uri.parse(
                                "package:$packageName"
                            )
                    }

                startActivity(intent)
            }
            .show()
    }
    private fun solicitarPermisoUbicacion() {

        Log.d(
            "UBICACION",
            "INICIANDO SOLICITUD DE PERMISOS"
        )


        // ==========================================
        // ¿YA TIENE PERMISO?
        // ==========================================

        if (tienePermisoUbicacion()) {

            Log.d(
                "UBICACION",
                "EL PERMISO YA ESTÁ CONCEDIDO"
            )

            obtenerUbicacionUsuario()

            return
        }


        // ==========================================
        // SOLICITAR PERMISOS
        // ==========================================

        solicitarPermisosUbicacion.launch(

            arrayOf(

                Manifest.permission.ACCESS_FINE_LOCATION,

                Manifest.permission.ACCESS_COARSE_LOCATION

            )
        )
    }
    private fun mostrarDialogoPermisoUbicacion() {

        // ==========================================
        // CREAR DIÁLOGO
        // ==========================================

        val dialog =
            Dialog(this)


        // ==========================================
        // CARGAR DISEÑO
        // ==========================================

        val vista =
            layoutInflater.inflate(
                R.layout.dialog_permiso_ubicacion,
                null
            )


        dialog.setContentView(vista)


        // ==========================================
        // CONFIGURAR VENTANA
        // ==========================================

        dialog.window?.apply {

            setBackgroundDrawableResource(
                android.R.color.transparent
            )

            setDimAmount(0.6f)
        }


        // ==========================================
        // BOTÓN PERMITIR
        // ==========================================

        vista.findViewById<MaterialButton>(
            R.id.btnPermitirUbicacion
        ).setOnClickListener {

            // Primero cerramos nuestro diálogo
            dialog.dismiss()


            // Después solicitamos el permiso oficial
            solicitarPermisoUbicacion()
        }


        // ==========================================
        // BOTÓN CANCELAR
        // ==========================================

        vista.findViewById<MaterialButton>(
            R.id.btnCancelarUbicacion
        ).setOnClickListener {

            dialog.dismiss()
        }


        // ==========================================
        // EVITAR CERRAR TOCANDO FUERA
        // ==========================================

        dialog.setCanceledOnTouchOutside(false)


        // ==========================================
        // MOSTRAR
        // ==========================================

        dialog.show()


        // ==========================================
        // TAMAÑO
        // ==========================================

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
    private fun abrirPreferenciasCuenta() {

        Log.d("PREFERENCIAS", "ENTRANDO A abrirPreferenciasCuenta()")

        binding.navigationView.visibility = View.GONE

        binding.panelPreferenciasCuenta.visibility = View.VISIBLE

        binding.panelPreferenciasCuenta.bringToFront()

        Log.d(
            "PREFERENCIAS",
            "panelPreferenciasCuenta VISIBLE"
        )
    }
    private fun cerrarPreferenciasCuenta() {

        Log.d("PREFERENCIAS", "CERRANDO PREFERENCIAS")

        binding.panelPreferenciasCuenta.visibility = View.GONE

        binding.navigationView.visibility = View.VISIBLE
    }
    private fun configurarPreferenciasCuenta() {

        // REGRESAR
        val btnRegresar = binding.panelPreferenciasCuenta
            .findViewById<View>(R.id.btnRegresar)

        btnRegresar?.setOnClickListener {
            cerrarPreferenciasCuenta()
        }


        // CAMBIAR NOMBRE
        val btnCambiarNombre = binding.panelPreferenciasCuenta
            .findViewById<View>(R.id.btnCambiarNombre)

        btnCambiarNombre?.setOnClickListener {
            mostrarDialogoCambiarNombre()
        }


        // TÉRMINOS Y CONDICIONES
        val btnTerminos = binding.panelPreferenciasCuenta
            .findViewById<View>(R.id.btnTerminosCondiciones)

        btnTerminos?.setOnClickListener {
            mostrarTerminosCompletos()
        }


        // PREMIUM
        val btnPremium = binding.panelPreferenciasCuenta
            .findViewById<View>(R.id.btnPremium)

        btnPremium?.setOnClickListener {

            val intent = Intent(this, PremiumActivity::class.java)
            startActivity(intent)
        }


        // REPORTAR PROBLEMA
        val btnReportar = binding.panelPreferenciasCuenta
            .findViewById<View>(R.id.btnReportarproblemas)

        btnReportar?.setOnClickListener {

            val intent = Intent(this, ReportarProblemaActivity::class.java)
            startActivity(intent)
        }
    }
    private fun mostrarDialogoCambiarNombre() {

        val dialog =
            Dialog(this)


        // ======================================================
        // CARGAR DISEÑO
        // ======================================================

        val vista =
            layoutInflater.inflate(
                R.layout.dialog_cambiar_nombre,
                null
            )


        dialog.setContentView(vista)


        // ======================================================
        // FONDO TRANSPARENTE
        // ======================================================

        dialog.window?.setBackgroundDrawableResource(
            android.R.color.transparent
        )


        // ======================================================
        // ELEMENTOS
        // ======================================================

        val edtNombre =
            vista.findViewById<EditText>(
                R.id.edtNuevoNombre
            )


        val btnGuardar =
            vista.findViewById<MaterialButton>(
                R.id.btnGuardarNombre
            )


        val btnCancelar =
            vista.findViewById<MaterialButton>(
                R.id.btnCancelarNombre
            )


        // ======================================================
        // NOMBRE ACTUAL
        // ======================================================

        edtNombre.setText(
            nombreUsuarioActual
        )

        edtNombre.setSelection(
            edtNombre.text.length
        )


        // ======================================================
        // CANCELAR
        // ======================================================

        btnCancelar.setOnClickListener {

            Log.d(
                "PREFERENCIAS",
                "CANCELAR CAMBIO DE NOMBRE"
            )

            dialog.dismiss()
        }


        // ======================================================
        // GUARDAR
        // ======================================================

        btnGuardar.setOnClickListener {

            val nuevoNombre =
                edtNombre.text
                    .toString()
                    .trim()


            // ==================================================
            // VALIDAR
            // ==================================================

            if (nuevoNombre.isEmpty()) {

                edtNombre.error =
                    "Escribe un nombre"

                return@setOnClickListener
            }


            if (nuevoNombre.length < 2) {

                edtNombre.error =
                    "El nombre debe tener al menos 2 caracteres"

                return@setOnClickListener
            }


            if (nuevoNombre.length > 50) {

                edtNombre.error =
                    "El nombre no puede superar los 50 caracteres"

                return@setOnClickListener
            }


            // ==================================================
            // OBTENER ID DEL USUARIO
            // ==================================================

            val clienteId =
                SesionUsuario.obtenerId(this)


            if (clienteId <= 0) {

                makeText(
                    this,
                    "No se encontró el usuario.",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }


            // ==================================================
            // DESHABILITAR BOTÓN
            // ==================================================

            btnGuardar.isEnabled = false


            // ==================================================
            // CREAR REQUEST
            // ==================================================

            val datos =
                CambiarNombreRequest(

                    CLI_ID =
                        clienteId,

                    CLI_NOMBRE =
                        nuevoNombre
                )


            Log.d(
                "PREFERENCIAS",
                "Actualizando nombre: $nuevoNombre"
            )


            // ==================================================
            // LLAMAR API
            // ==================================================

            lifecycleScope.launch {

                try {

                    val respuesta =
                        ApiClient.apiService
                            .cambiarNombreUsuario(
                                datos
                            )


                    // ==================================================
                    // RESPUESTA EXITOSA
                    // ==================================================

                    if (respuesta.success) {

                        val nombreActualizado =
                            respuesta.CLI_NOMBRE
                                ?: nuevoNombre


                        // ==============================================
                        // ACTUALIZAR VARIABLE
                        // ==============================================

                        nombreUsuarioActual =
                            nombreActualizado


                        // ==============================================
                        // ACTUALIZAR SESIÓN
                        // ==============================================

                        actualizarNombre(
                            context = this@Menu_principal_free,
                            nombre = nombreActualizado
                        )


                        Log.d(
                            "PREFERENCIAS",
                            "Nombre actualizado: $nombreActualizado"
                        )


                        // ==============================================
                        // MENSAJE
                        // ==============================================

                        makeText(
                            this@Menu_principal_free,
                            respuesta.mensaje
                                ?: "Nombre actualizado correctamente.",
                            Toast.LENGTH_SHORT
                        ).show()


                        // ==============================================
                        // CERRAR
                        // ==============================================

                        dialog.dismiss()

                    } else {

                        makeText(
                            this@Menu_principal_free,
                            respuesta.mensaje
                                ?: "No se pudo actualizar el nombre.",
                            Toast.LENGTH_SHORT
                        ).show()


                        btnGuardar.isEnabled =
                            true
                    }

                } catch (e: Exception) {

                    Log.e(
                        "PREFERENCIAS",
                        "Error al actualizar nombre",
                        e
                    )


                    makeText(
                        this@Menu_principal_free,
                        "Error al actualizar el nombre.",
                        Toast.LENGTH_SHORT
                    ).show()


                    btnGuardar.isEnabled =
                        true
                }
            }
        }


        // ======================================================
        // MOSTRAR DIÁLOGO
        // ======================================================

        dialog.show()


        // ======================================================
        // TAMAÑO
        // ======================================================

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
    private fun mostrarTerminosCompletos() {

        val dialogView =
            layoutInflater.inflate(
                R.layout.terminos_completos,
                null
            )


        val builder =
            AlertDialog.Builder(this)


        builder.setView(dialogView)


        // ======================================================
        // ELEMENTOS
        // ======================================================

        val scrollView =
            dialogView.findViewById<ScrollView>(
                R.id.scrollView
            )


        val txtTerminosCompletos =
            dialogView.findViewById<TextView>(
                R.id.txtTerminosCompletos
            )


        val btnCerrar =
            dialogView.findViewById<Button>(
                R.id.btnCerrar
            )


        // ======================================================
        // TÉRMINOS
        // ======================================================

        val terminosTexto = """
        
        TÉRMINOS Y CONDICIONES DE USO
        
        1. TÉRMINOS Y CONDICIONES DE USO DE GOURMEET
        
        Los presentes Términos y Condiciones de Uso (en lo sucesivo, los “TÉRMINOS Y CONDICIONES”) regulan de manera integral el acceso, navegación, uso y, en su caso, la interacción del usuario con el sitio web www.gourmeet.com.mx (en lo sucesivo, el “SITIO WEB”), el cual es propiedad y está operado por GourMeet (en lo sucesivo, el “TITULAR”).
        
        Al acceder, navegar o utilizar el SITIO WEB, toda persona (en lo sucesivo, el “USUARIO”) manifiesta expresa e inequívocamente que ha leído, entendido y aceptado sujetarse a lo dispuesto en los presentes TÉRMINOS Y CONDICIONES, así como al Aviso de Privacidad, la Política de Cookies y cualesquiera otras políticas, lineamientos o avisos legales complementarios que el TITULAR publique o ponga a disposición.
        
        El uso del SITIO WEB constituye un consentimiento expreso para vincularse jurídicamente conforme a lo aquí estipulado. En caso de no estar de acuerdo, el USUARIO deberá abstenerse de acceder o utilizar el SITIO WEB y sus funcionalidades.
        
        I. OBJETO
        
        El presente documento tiene como finalidad establecer los derechos, obligaciones, limitaciones y alcances que rigen el acceso, navegación y uso del SITIO WEB, incluyendo, de forma enunciativa pero no limitativa:
        
        1. Recetas de cocina de acceso libre y gratuito para consulta pública.
        
        2. Recomendaciones personalizadas de restaurantes y establecimientos gastronómicos cercanos al USUARIO, obtenidas mediante algoritmos y criterios del TITULAR.
        
        3. Contenido editorial y multimedia relacionado con gastronomía, técnicas culinarias, cultura gastronómica y estilo de vida culinario.
        
        El acceso general al SITIO WEB es gratuito, no obstante, algunos servicios, funcionalidades o contenidos podrían requerir registro previo, autenticación de cuenta, aceptación de condiciones particulares o pago de tarifas que se informarán previamente.
        
        II. TITULARIDAD Y LEGISLACIÓN APLICABLE
        
        Titular: Job Isaac Gutierrez Hernandez
        
        Dirección:
        Francisco I. Madero 15, Delegación Santa María Totoltepec, 50240 Santa María Totoltepec, Méx.
        
        Página web:
        https://www.gourmeet.com.mx
        
        Correo electrónico:
        soporte@gourmeet.com.mx
        
        Teléfono:
        722 889 1315
        
        El SITIO WEB y sus contenidos se rigen, interpretan y ejecutan de conformidad con la legislación vigente en los Estados Unidos Mexicanos.
        
        Cualquier acto jurídico, transacción, reclamación o controversia que derive directa o indirectamente del acceso, navegación o uso del SITIO WEB se someterá, para su interpretación y cumplimiento, a las leyes mexicanas.
        
        III. CONDICIÓN DE USUARIO Y RESPONSABILIDADES
        
        El acceso y/o uso del SITIO WEB confiere la condición de USUARIO, lo que implica la aceptación plena y sin reservas de lo aquí establecido.
        
        El USUARIO se compromete a:
        
        1. Cumplir con la ley, la moral, el orden público y las buenas costumbres.
        
        2. No realizar actos ilícitos, ofensivos, difamatorios, fraudulentos o lesivos.
        
        3. Proporcionar información veraz, completa y actualizada.
        
        4. No introducir virus, malware, código malicioso o cualquier mecanismo que pueda dañar el funcionamiento del SITIO WEB.
        
        IV. FUNCIONALIDADES DEL SITIO WEB
        
        1. Recetas de cocina.
        
        Las recetas pueden ser elaboradas por el equipo del TITULAR, usuarios colaboradores o fuentes autorizadas.
        
        El TITULAR no garantiza la exactitud, seguridad o resultados de la preparación.
        
        2. Recomendaciones gastronómicas.
        
        Las sugerencias de restaurantes u otros establecimientos son meramente informativas.
        
        V. USO DE UBICACIÓN Y DATOS
        
        El SITIO WEB podrá solicitar acceso a la ubicación del USUARIO para ofrecer recomendaciones personalizadas.
        
        Este acceso es opcional y puede deshabilitarse en la configuración del dispositivo.
        
        El tratamiento de datos personales se realizará conforme a lo establecido en el Aviso de Privacidad y en la Ley Federal de Protección de Datos Personales en Posesión de los Particulares.
        
        VI. CONTENIDO GENERADO POR USUARIOS
        
        El SITIO WEB podrá permitir que los USUARIOS publiquen recetas, comentarios, fotografías u otros materiales.
        
        El USUARIO garantiza que posee todos los derechos necesarios sobre dicho CONTENIDO.
        
        VII. PROPIEDAD INTELECTUAL E INDUSTRIAL
        
        Todos los elementos del SITIO WEB, incluidos textos, recetas, fotografías, videos, diseños, logotipos, marcas, nombres comerciales y código fuente son propiedad del TITULAR o de terceros con licencia.
        
        Queda estrictamente prohibida su reproducción, distribución, modificación o explotación con fines comerciales sin autorización previa.
        
        VIII. DISPONIBILIDAD Y LIMITACIÓN DE RESPONSABILIDAD
        
        El TITULAR realizará esfuerzos razonables para mantener el SITIO WEB disponible y operativo, sin embargo, no garantiza disponibilidad ininterrumpida ni libre de errores.
        
        IX. POLÍTICA DE ENLACES
        
        El SITIO WEB puede contener enlaces a sitios externos.
        
        El TITULAR no es responsable de su contenido, seguridad o disponibilidad.
        
        X. USO DE COOKIES
        
        El SITIO WEB utiliza cookies y tecnologías similares para mejorar la experiencia del USUARIO.
        
        XI. MODIFICACIONES
        
        El TITULAR podrá modificar en cualquier momento los presentes TÉRMINOS Y CONDICIONES.
        
        XII. CONTACTO
        
        Para dudas, aclaraciones o comentarios:
        
        Correo electrónico:
        soporte@gourmeet.com.mx
        
        Ubicación:
        Francisco I. Madero 15, Delegación Santa María Totoltepec, 50240 Santa María Totoltepec, Méx.
        
        Teléfono:
        722 889 1315
        
        Última actualización:
        14 de agosto de 2025
        
        """.trimIndent()


        txtTerminosCompletos.text =
            terminosTexto


        // ======================================================
        // CREAR DIÁLOGO
        // ======================================================

        val dialog =
            builder.create()


        // ======================================================
        // CERRAR
        // ======================================================

        btnCerrar.setOnClickListener {

            dialog.dismiss()
        }


        // ======================================================
        // MOSTRAR
        // ======================================================

        dialog.show()


        // ======================================================
        // TAMAÑO
        // ======================================================

        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (resources.displayMetrics.heightPixels * 0.85).toInt()
        )
    }
    private fun cargarAlacenas() {

        val clienteId = SesionUsuario.obtenerId(this)

        if (clienteId <= 0) {

            makeText(
                this,
                "No se pudo obtener el usuario.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        lifecycleScope.launch {

            try {

                val request = ListarAlacenasRequest(
                    ALC_CLI_ID = clienteId
                )

                Log.d(
                    "ALACENA",
                    "Consultando alacenas del cliente: $clienteId"
                )

                val respuesta = ApiClient.apiService
                    .listarAlacenas(request)

                Log.d(
                    "ALACENA",
                    "Respuesta: $respuesta"
                )

                if (respuesta.success) {

                    listaAlacenas.clear()
                    listaAlacenas.addAll(respuesta.alacenas)

                    if (listaAlacenas.isEmpty()) {

                        Log.d(
                            "ALACENA",
                            "El usuario no tiene alacenas."
                        )

                        mostrarSinAlacenas()

                    } else {

                        Log.d(
                            "ALACENA",
                            "Alacenas encontradas: ${listaAlacenas.size}"
                        )

                        mostrarAlacenas()

                        // =================================
                        // PRIMERA ALACENA SELECCIONADA
                        // =================================

                        val alacenaSeleccionada =
                            listaAlacenas.first()

                        // =================================
                        // ACTUALIZAR ESTADOS Y CARGAR
                        // INGREDIENTES
                        // =================================

                        actualizarEstadosYCargarAlacena(
                            alacenaSeleccionada.ALC_ID,
                            clienteId
                        )
                    }

                } else {

                    makeText(
                        this@Menu_principal_free,
                        "No fue posible cargar las alacenas.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {

                Log.e(
                    "ALACENA",
                    "Error al cargar las alacenas",
                    e
                )

                makeText(
                    this@Menu_principal_free,
                    "Error al cargar las alacenas.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    private fun mostrarSinAlacenas() {

        val panel = binding.panelAlacena
            .findViewById<View>(R.id.panelSinAlacenas)

        val selector = binding.panelAlacena
            .findViewById<View>(R.id.tilAlacena)

        val consumePrimero = binding.panelAlacena
            .findViewById<View>(R.id.rvConsumePrimero)

        val tituloConsume = binding.panelAlacena
            .findViewById<View>(R.id.txtConsumePrimero)

        val ingredientes = binding.panelAlacena
            .findViewById<View>(R.id.rvMisIngredientes)

        val tituloIngredientes = binding.panelAlacena
            .findViewById<View>(R.id.txtMisIngredientes)

        val btnAgregar = binding.panelAlacena
            .findViewById<View>(R.id.btnAgregarIngrediente)

        val btnCrear = binding.panelAlacena
            .findViewById<View>(R.id.btnCrearPrimeraAlacena)


        // ==========================================
        // OCULTAR CONTENIDO DE ALACENA
        // ==========================================

        selector.visibility = View.GONE

        tituloConsume.visibility = View.GONE
        consumePrimero.visibility = View.GONE

        tituloIngredientes.visibility = View.GONE
        ingredientes.visibility = View.GONE

        btnAgregar.visibility = View.GONE


        // ==========================================
        // MOSTRAR PANEL SIN ALACENAS
        // ==========================================

        panel.visibility = View.VISIBLE


        // ==========================================
        // BOTÓN CREAR
        // ==========================================

        btnCrear.setOnClickListener {

            mostrarDialogCrearAlacena()
        }
    }
    private fun mostrarDialogCrearAlacena() {

        val bottomSheet = BottomSheetDialog(this)

        val view = layoutInflater.inflate(
            R.layout.dialog_crear_alacena,
            null
        )

        bottomSheet.setContentView(view)

        val edtNombre = view.findViewById<TextInputEditText>(
            R.id.edtNombreAlacena
        )

        val btnCasa = view.findViewById<ImageButton>(
            R.id.btnIconoCasa
        )

        val btnOficina = view.findViewById<ImageButton>(
            R.id.btnIconoOficina
        )

        val btnRefrigerador = view.findViewById<ImageButton>(
            R.id.btnIconoRefrigerador
        )

        val btnCancelar = view.findViewById<MaterialButton>(
            R.id.btnCancelarAlacena
        )

        val btnGuardar = view.findViewById<MaterialButton>(
            R.id.btnGuardarAlacena
        )

        var iconoSeleccionado = "CASA"

        // CASA seleccionada inicialmente
        seleccionarIcono(
            btnSeleccionado = btnCasa,
            btnCasa,
            btnOficina,
            btnRefrigerador
        )

        btnCasa.setOnClickListener {

            iconoSeleccionado = "CASA"

            seleccionarIcono(
                btnCasa,
                btnCasa,
                btnOficina,
                btnRefrigerador
            )
        }

        btnOficina.setOnClickListener {

            iconoSeleccionado = "OFICINA"

            seleccionarIcono(
                btnOficina,
                btnCasa,
                btnOficina,
                btnRefrigerador
            )
        }

        btnRefrigerador.setOnClickListener {

            iconoSeleccionado = "REFRIGERADOR"

            seleccionarIcono(
                btnRefrigerador,
                btnCasa,
                btnOficina,
                btnRefrigerador
            )
        }

        btnCancelar.setOnClickListener {
            bottomSheet.dismiss()
        }

        btnGuardar.setOnClickListener {

            val nombre = edtNombre.text
                ?.toString()
                ?.trim()
                .orEmpty()

            if (nombre.isEmpty()) {
                edtNombre.error = "Ingresa un nombre"
                edtNombre.requestFocus()
                return@setOnClickListener
            }

            crearAlacena(
                nombre = nombre,
                icono = iconoSeleccionado,
                dialog = bottomSheet
            )
        }

        bottomSheet.setOnShowListener {

            val dialog = it as BottomSheetDialog

            val bottomSheetView =
                dialog.findViewById<View>(
                    com.google.android.material.R.id.design_bottom_sheet
                )

            bottomSheetView?.let { sheet ->

                val behavior = BottomSheetBehavior.from(sheet)

                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true

                sheet.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.bg_bottom_sheet_alacena
                )
            }
        }

        bottomSheet.show()
    }
    private fun seleccionarIcono(
        btnSeleccionado: ImageButton,
        btnCasa: ImageButton,
        btnOficina: ImageButton,
        btnRefrigerador: ImageButton
    ) {

        // Quitar selección
        btnCasa.background =
            ContextCompat.getDrawable(
                this,
                R.drawable.bg_icono_alacena_normal
            )

        btnOficina.background =
            ContextCompat.getDrawable(
                this,
                R.drawable.bg_icono_alacena_normal
            )

        btnRefrigerador.background =
            ContextCompat.getDrawable(
                this,
                R.drawable.bg_icono_alacena_normal
            )

        // Poner selección
        btnSeleccionado.background =
            ContextCompat.getDrawable(
                this,
                R.drawable.bg_icono_alacena_seleccionado
            )
    }
    private fun crearAlacena(
        nombre: String,
        icono: String,
        dialog: BottomSheetDialog
    ) {

        val clienteId = SesionUsuario.obtenerId(this)

        if (clienteId <= 0) {
            makeText(
                this,
                "No se pudo obtener el usuario.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        lifecycleScope.launch {

            try {

                val request = CrearAlacenaRequest(
                    ALC_CLI_ID = clienteId,
                    ALC_NOMBRE = nombre,
                    ALC_ICONO = icono
                )

                Log.d("ALACENA", "Creando alacena: $request")

                val respuesta = ApiClient.apiService.crearAlacena(request)

                Log.d("ALACENA", "Respuesta: $respuesta")

                if (respuesta.success) {

                    makeText(
                        this@Menu_principal_free,
                        respuesta.message ?: "Alacena creada correctamente",
                        Toast.LENGTH_SHORT
                    ).show()

                    dialog.dismiss()

                    // Volvemos a cargar las alacenas
                    cargarAlacenas()

                } else {

                    makeText(
                        this@Menu_principal_free,
                        respuesta.message ?: "No se pudo crear la alacena.",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {

                Log.e(
                    "ALACENA",
                    "Error al crear la alacena",
                    e
                )

                makeText(
                    this@Menu_principal_free,
                    "Error de conexión con el servidor.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    private fun mostrarAlacenas() {

        val panel = binding.panelAlacena
            .findViewById<View>(R.id.panelSinAlacenas)

        val selector = binding.panelAlacena
            .findViewById<View>(R.id.tilAlacena)

        val tituloConsume = binding.panelAlacena
            .findViewById<View>(R.id.txtConsumePrimero)

        val consumePrimero = binding.panelAlacena
            .findViewById<View>(R.id.rvConsumePrimero)

        val tituloIngredientes = binding.panelAlacena
            .findViewById<View>(R.id.txtMisIngredientes)

        val ingredientes = binding.panelAlacena
            .findViewById<View>(R.id.rvMisIngredientes)

        val btnAgregar = binding.panelAlacena
            .findViewById<View>(R.id.btnAgregarIngrediente)
        val btnRegresar = binding.panelAlacena.findViewById<View>(R.id.btnRegresarAlacena)


        // ==========================================
        // MOSTRAR CONTENIDO
        // ==========================================

        panel.visibility = View.GONE
        btnRegresar.visibility = View.GONE
        selector.visibility = View.VISIBLE

        tituloConsume.visibility = View.VISIBLE
        consumePrimero.visibility = View.VISIBLE


        tituloIngredientes.visibility = View.VISIBLE
        ingredientes.visibility = View.VISIBLE

        btnAgregar.visibility = View.VISIBLE


        // ==========================================
        // CONFIGURAR ALACENA SELECCIONADA
        // ==========================================

        configurarSelectorAlacenas()
    }
    private fun configurarSelectorAlacenas() {

        val btnAlacena =
            binding.panelAlacena.findViewById<MaterialButton>(
                R.id.actAlacena
            )

        // ==========================================
        // VERIFICAR QUE EXISTAN ALACENAS
        // ==========================================

        if (listaAlacenas.isEmpty()) {
            btnAlacena.text = "Seleccionar alacena"
            return
        }


        // ==========================================
        // SELECCIONAR LA PRIMERA ALACENA
        // ==========================================

        if (alacenaSeleccionada == null) {

            alacenaSeleccionada = listaAlacenas[0]
        }


        // ==========================================
        // MOSTRAR ALACENA ACTUAL
        // ==========================================

        btnAlacena.text =
            alacenaSeleccionada?.ALC_NOMBRE
                ?: "Seleccionar alacena"


        Log.d(
            "ALACENA",
            "Alacena seleccionada: ${alacenaSeleccionada?.ALC_ID}"
        )


        // ==========================================
        // ABRIR MIS ALACENAS
        // ==========================================

        btnAlacena.setOnClickListener {

            mostrarMenuMisAlacenas()
        }
    }
    private fun mostrarMenuMisAlacenas() {

        val bottomSheet = BottomSheetDialog(this)

        val view = layoutInflater.inflate(
            R.layout.dialog_mis_alacenas,
            null
        )

        bottomSheet.setContentView(view)

        val rvMisAlacenas =
            view.findViewById<RecyclerView>(
                R.id.rvMisAlacenas
            )

        rvMisAlacenas.layoutManager =
            LinearLayoutManager(this)

        val adapter = AlacenaMenuAdapter(

            lista = listaAlacenas,

            // ======================================
            // SELECCIONAR
            // ======================================

            onSeleccionar = { alacena ->

                alacenaSeleccionada = alacena

                val btnAlacena =
                    binding.panelAlacena
                        .findViewById<MaterialButton>(
                            R.id.actAlacena
                        )

                btnAlacena.text =
                    alacena.ALC_NOMBRE

                Log.d(
                    "ALACENA",
                    "Nueva alacena seleccionada: ${alacena.ALC_ID}"
                )

                bottomSheet.dismiss()

                // Más adelante:
                // cargarIngredientesAlacena(alacena.ALC_ID)
            },

            // ======================================
            // EDITAR
            // ======================================

            onEditar = { alacena ->

                bottomSheet.dismiss()

                mostrarDialogEditarAlacena(alacena)
            },

            // ======================================
            // ELIMINAR
            // ======================================

            onEliminar = { alacena ->

                bottomSheet.dismiss()

                mostrarConfirmacionEliminarAlacena(alacena)
            }
        )

        rvMisAlacenas.adapter = adapter


        // ==========================================
        // CONFIGURAR BOTTOM SHEET
        // ==========================================

        bottomSheet.setOnShowListener {

            val dialog = it as BottomSheetDialog

            val sheet =
                dialog.findViewById<View>(
                    com.google.android.material.R.id.design_bottom_sheet
                )

            sheet?.let { bottomSheetView ->

                val behavior =
                    BottomSheetBehavior.from(
                        bottomSheetView
                    )

                behavior.state =
                    BottomSheetBehavior.STATE_EXPANDED

                behavior.skipCollapsed = true

                bottomSheetView.background =
                    ContextCompat.getDrawable(
                        this,
                        R.drawable.bg_bottom_sheet_alacena
                    )
            }
        }

        bottomSheet.show()
    }
    private fun mostrarDialogEditarAlacena(
        alacena: Alacena
    ) {

        val bottomSheet = BottomSheetDialog(this)

        val view = layoutInflater.inflate(
            R.layout.dialog_crear_alacena,
            null
        )

        bottomSheet.setContentView(view)

        val txtTitulo =
            view.findViewById<TextView>(
                R.id.txtTituloAlacena
            )

        val edtNombre =
            view.findViewById<TextInputEditText>(
                R.id.edtNombreAlacena
            )

        val btnCasa =
            view.findViewById<ImageButton>(
                R.id.btnIconoCasa
            )

        val btnOficina =
            view.findViewById<ImageButton>(
                R.id.btnIconoOficina
            )

        val btnRefrigerador =
            view.findViewById<ImageButton>(
                R.id.btnIconoRefrigerador
            )

        val btnCancelar =
            view.findViewById<MaterialButton>(
                R.id.btnCancelarAlacena
            )

        val btnGuardar =
            view.findViewById<MaterialButton>(
                R.id.btnGuardarAlacena
            )

        // Titulo
        txtTitulo.text = "EDITAR ALACENA"

        // Cargar nombre
        edtNombre.setText(
            alacena.ALC_NOMBRE
        )

        // Icono actual
        var iconoSeleccionado =
            alacena.ALC_ICONO.uppercase()

        when (iconoSeleccionado) {

            "CASA" -> seleccionarIconoAlacena(
                btnCasa,
                btnCasa,
                btnOficina,
                btnRefrigerador
            )

            "OFICINA" -> seleccionarIconoAlacena(
                btnOficina,
                btnCasa,
                btnOficina,
                btnRefrigerador
            )

            "REFRIGERADOR" -> seleccionarIconoAlacena(
                btnRefrigerador,
                btnCasa,
                btnOficina,
                btnRefrigerador
            )
        }

        // CASA
        btnCasa.setOnClickListener {

            iconoSeleccionado = "CASA"

            seleccionarIconoAlacena(
                btnCasa,
                btnCasa,
                btnOficina,
                btnRefrigerador
            )
        }

        // OFICINA
        btnOficina.setOnClickListener {

            iconoSeleccionado = "OFICINA"

            seleccionarIconoAlacena(
                btnOficina,
                btnCasa,
                btnOficina,
                btnRefrigerador
            )
        }

        // REFRIGERADOR
        btnRefrigerador.setOnClickListener {

            iconoSeleccionado = "REFRIGERADOR"

            seleccionarIconoAlacena(
                btnRefrigerador,
                btnCasa,
                btnOficina,
                btnRefrigerador
            )
        }

        // CANCELAR
        btnCancelar.setOnClickListener {
            bottomSheet.dismiss()
        }

        // GUARDAR
        btnGuardar.setOnClickListener {

            val nombre =
                edtNombre.text
                    ?.toString()
                    ?.trim()
                    .orEmpty()

            if (nombre.isEmpty()) {

                edtNombre.error =
                    "Ingresa un nombre"

                edtNombre.requestFocus()

                return@setOnClickListener
            }

            editarAlacena(
                alacena = alacena,
                nombre = nombre,
                icono = iconoSeleccionado,
                dialog = bottomSheet
            )
        }

        bottomSheet.setOnShowListener {

            val dialog = it as BottomSheetDialog

            val sheet =
                dialog.findViewById<View>(
                    com.google.android.material.R.id.design_bottom_sheet
                )

            sheet?.let {

                val behavior =
                    BottomSheetBehavior.from(it)

                behavior.state =
                    BottomSheetBehavior.STATE_EXPANDED

                behavior.skipCollapsed = true

                it.background =
                    ContextCompat.getDrawable(
                        this,
                        R.drawable.bg_bottom_sheet_alacena
                    )
            }
        }

        bottomSheet.show()
    }
    private fun editarAlacena(
        alacena: Alacena,
        nombre: String,
        icono: String,
        dialog: BottomSheetDialog
    ) {

        val clienteId =
            SesionUsuario.obtenerId(this)

        if (clienteId <= 0) {

            makeText(
                this,
                "No se pudo obtener el usuario.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        lifecycleScope.launch {

            try {

                val request =
                    EditarAlacenaRequest(
                        ALC_ID = alacena.ALC_ID,
                        ALC_CLI_ID = clienteId,
                        ALC_NOMBRE = nombre,
                        ALC_ICONO = icono
                    )

                Log.d(
                    "ALACENA",
                    "Editando: $request"
                )

                val respuesta =
                    ApiClient.apiService.editarAlacena(
                        request
                    )

                Log.d(
                    "ALACENA",
                    "Respuesta: $respuesta"
                )

                if (respuesta.success) {

                    makeText(
                        this@Menu_principal_free,
                        respuesta.message
                            ?: "Alacena actualizada correctamente.",
                        Toast.LENGTH_SHORT
                    ).show()

                    dialog.dismiss()

                    cargarAlacenas()

                } else {

                    makeText(
                        this@Menu_principal_free,
                        respuesta.message
                            ?: "No se pudo actualizar la alacena.",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {

                Log.e(
                    "ALACENA",
                    "Error al editar alacena",
                    e
                )

                makeText(
                    this@Menu_principal_free,
                    "Error de conexión con el servidor.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    private fun mostrarConfirmacionEliminarAlacena(
        alacena: Alacena
    ) {

        AlertDialog.Builder(this)
            .setTitle("ELIMINAR ALACENA")
            .setMessage(
                "¿Quieres eliminar la alacena " +
                        "\"${alacena.ALC_NOMBRE}\"?\n\n" +
                        "Esta acción no se puede deshacer."
            )
            .setNegativeButton("CANCELAR", null)
            .setPositiveButton("ELIMINAR") { _, _ ->

                eliminarAlacena(alacena)
            }
            .show()
    }
    private fun eliminarAlacena(
        alacena: Alacena
    ) {

        val clienteId =
            SesionUsuario.obtenerId(this)

        if (clienteId <= 0) {

            makeText(
                this,
                "No se pudo obtener el usuario.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        lifecycleScope.launch {

            try {

                val request =
                    EliminarAlacenaRequest(
                        ALC_ID = alacena.ALC_ID,
                        ALC_CLI_ID = clienteId
                    )

                Log.d(
                    "ALACENA",
                    "Eliminando: $request"
                )

                val respuesta =
                    ApiClient.apiService.eliminarAlacena(
                        request
                    )

                Log.d(
                    "ALACENA",
                    "Respuesta: $respuesta"
                )

                if (respuesta.success) {

                    makeText(
                        this@Menu_principal_free,
                        respuesta.message
                            ?: "Alacena eliminada correctamente.",
                        Toast.LENGTH_SHORT
                    ).show()

                    cargarAlacenas()

                } else {

                    makeText(
                        this@Menu_principal_free,
                        respuesta.message
                            ?: "No se pudo eliminar la alacena.",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {

                Log.e(
                    "ALACENA",
                    "Error al eliminar alacena",
                    e
                )

                makeText(
                    this@Menu_principal_free,
                    "Error de conexión con el servidor.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    private fun seleccionarIconoAlacena(
        btnSeleccionado: ImageButton,
        btnCasa: ImageButton,
        btnOficina: ImageButton,
        btnRefrigerador: ImageButton
    ) {

        // Quitar el círculo azul de todos
        btnCasa.background =
            ContextCompat.getDrawable(
                this,
                R.drawable.bg_icono_alacena_normal
            )

        btnOficina.background =
            ContextCompat.getDrawable(
                this,
                R.drawable.bg_icono_alacena_normal
            )

        btnRefrigerador.background =
            ContextCompat.getDrawable(
                this,
                R.drawable.bg_icono_alacena_normal
            )

        // Colocar el círculo azul al icono seleccionado
        btnSeleccionado.background =
            ContextCompat.getDrawable(
                this,
                R.drawable.bg_icono_alacena_seleccionado
            )
    }
    private fun mostrarDialogAgregarIngrediente(
        ingredienteEditar: IngredienteAlacena? = null) {
        val bottomSheet = BottomSheetDialog(this)
        val modoIngrediente =
            when {
                ingredienteEditar == null ->
                    ModoIngrediente.AGREGAR

                ingredienteEditar.AI_ESTADO
                    ?.trim()
                    ?.equals(
                        "descompuesto",
                        ignoreCase = true
                    ) == true ->
                    ModoIngrediente.USAR_EN_RECETA

                else ->
                    ModoIngrediente.EDITAR
            }

        val view = layoutInflater.inflate(
            R.layout.dialog_agregar_ingrediente,
            null
        )
        val flechaSeleccionarAlacena =
            view.findViewById<ImageView>(
                R.id.flechaseleccionaralacena
            )
        val txtTituloIngrediente =
            view.findViewById<TextView>(
                R.id.txtTituloIngrediente
            )
        val edtIngrediente =
            view.findViewById<EditText>(
                R.id.edtIngrediente
            )
        var cargandoIngredienteEditar = ingredienteEditar != null

        val txtEmojiIngrediente =
            view.findViewById<ImageView>(
                R.id.txtEmojiIngrediente
            )

        val txtCantidad =
            view.findViewById<EditText>(
                R.id.txtCantidad
            )

        val actUnidadCantidad =
            view.findViewById<MaterialAutoCompleteTextView>(
                R.id.actUnidadCantidad
            )

        val txtPrecioCompra =
            view.findViewById<EditText>(
                R.id.txtPrecioCompra
            )

        val txtAlacenaSeleccionada =
            view.findViewById<TextView>(
                R.id.txtAlacenaSeleccionada
            )

        val imgIconoAlacenaSeleccionada =
            view.findViewById<ImageView>(
                R.id.imgIconoAlacenaSeleccionada
            )

        if (ingredienteEditar != null) {
            txtTituloIngrediente.text = "EDITAR INGREDIENTE"
        } else {
            txtTituloIngrediente.text = "AGREGAR INGREDIENTE"
        }

        flechaSeleccionarAlacena.setOnClickListener {

            val txtAlacenaSeleccionada =
                view.findViewById<TextView>(
                    R.id.txtAlacenaSeleccionada
                )

            val flechaSeleccionarAlacena =
                view.findViewById<ImageView>(
                    R.id.flechaseleccionaralacena
                )

            flechaSeleccionarAlacena.setOnClickListener {

                mostrarMenuMisAlacenas2(
                    txtAlacenaSeleccionada
                )
            }

        }
        bottomSheet.setContentView(view)
        val txtFechaCompra =
            view.findViewById<TextView>(R.id.txtFechadecompra)

        val txtFechaConsumo =
            view.findViewById<TextView>(
                R.id.txtFechadecon
            )

        val txtTipoEstado =
            view.findViewById<TextView>(
                R.id.txtTipodeestado
            )

        val txtTipoAlmacenamiento =
            view.findViewById<TextView>(
                R.id.txtTipodealmacenamiento
            )
        val txtcontenedeor =
            view.findViewById<TextView>(
                R.id.txtcontenedeor
            )
        val cardFechadecon =
            view.findViewById<MaterialCardView>(
                R.id.cardFechadecon
            )

        val cardCantidad =
            view.findViewById<MaterialCardView>(
                R.id.cardCantidad
            )

        val vermasrecetas =
            view.findViewById<MaterialButton>(
                R.id.vermasrecetas
            )
        vermasrecetas.setOnClickListener {

            if (ingredienteEditar == null) {
                return@setOnClickListener
            }

            val ingredienteId =
                ingredienteEditar.ING_ID

            val nombreIngrediente =
                ingredienteEditar.ING_DESCRIPCION

            buscarRecetasDelIngrediente(
                ingredienteId,
                nombreIngrediente
            )
        }



        bottomSheet.setOnShowListener {

            // Evitar que el teclado mueva o redimensione el BottomSheet
            bottomSheet.window?.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
            )

            val dialog = it as BottomSheetDialog

            val sheet = dialog.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet
            )

            sheet?.let { bottomSheetView ->

                val behavior =
                    BottomSheetBehavior.from(bottomSheetView)

                behavior.state =
                    BottomSheetBehavior.STATE_EXPANDED

                behavior.skipCollapsed = true
            }
        }

        // =========================================================
        // FECHA DE COMPRA
        // =========================================================



        txtFechaCompra.setOnClickListener {

            val calendario = Calendar.getInstance()

            val year = calendario.get(Calendar.YEAR)
            val month = calendario.get(Calendar.MONTH)
            val day = calendario.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(
                this,
                R.style.TemaCalendarioGourMeet,
                { _, selectedYear, selectedMonth, selectedDay ->

                    val fechaSeleccionada = String.format(
                        "%02d/%02d/%04d",
                        selectedDay,
                        selectedMonth + 1,
                        selectedYear
                    )

                    txtFechaCompra.text = fechaSeleccionada
                    actualizarFechaCaducidad(
                        txtFechaCompra,
                        txtTipoEstado,
                        txtTipoAlmacenamiento,
                        txtFechaConsumo,
                        edtIngrediente
                    )
                },
                year,
                month,
                day
            )

            datePicker.show()
            datePicker.getButton(DatePickerDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(this, R.color.azulgourmeet))

            datePicker.getButton(DatePickerDialog.BUTTON_NEGATIVE)
                .setTextColor(ContextCompat.getColor(this, R.color.azulgourmeet))
        }

        // =========================================================
        // FECHA DE CONSUMO
        // =========================================================



        txtFechaConsumo.setOnClickListener {

            val calendario = Calendar.getInstance()

            val year = calendario.get(Calendar.YEAR)
            val month = calendario.get(Calendar.MONTH)
            val day = calendario.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(
                this,
                R.style.TemaCalendarioGourMeet,
                { _, selectedYear, selectedMonth, selectedDay ->

                    val fechaSeleccionada = String.format(
                        "%02d/%02d/%04d",
                        selectedDay,
                        selectedMonth + 1,
                        selectedYear
                    )

                    txtFechaConsumo.text = fechaSeleccionada
                },
                year,
                month,
                day
            )

            datePicker.show()
            datePicker.getButton(DatePickerDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(this, R.color.azulgourmeet))

            datePicker.getButton(DatePickerDialog.BUTTON_NEGATIVE)
                .setTextColor(ContextCompat.getColor(this, R.color.azulgourmeet))
        }


        // =========================================================
        // SELECTOR DE UNIDAD
        // =========================================================



        val imgFlechaUnidad =
            view.findViewById<ImageView>(
                R.id.imgFlechaUnidad
            )


        // =========================================================
        // LISTA DE UNIDADES
        // =========================================================

        val unidades = listOf(
            // PESO
            "⚖️ Peso",
            "g",          // gramos
            "kg",         // kilogramos

            // VOLUMEN
            "🥛 Volumen",
            "tza",        // taza // tazas
            "L",          // litros
            "mL",         // mililitros

            // PEQUEÑAS
            "🥄 Pequeñas",
            "cdta",       // cucharadita // cucharaditas
            "cda",        // cucharada // cucharadas
            "pizca",
            "pizcas",
            "al gusto",

            // PORCIONES
            "🍽️ Porciones",
            "pza",        // pieza // piezas
            "diente",
            "dientes",
            "rama",
            "ramas"
        )


        // =========================================================
        // TÍTULOS DE LAS CATEGORÍAS
        // NO SON SELECCIONABLES
        // =========================================================

        val titulos = setOf(
            "🥄 Pequeñas",
            "🥛 Volumen",
            "⚖️ Peso",
            "🍽️ Porciones"
        )


        // =========================================================
        // ADAPTER PERSONALIZADO
        // =========================================================

        val adapterUnidades = UnidadAdapter(
            this,
            unidades,
            titulos
        ) { unidadSeleccionada ->

            // Colocar la unidad seleccionada
            actUnidadCantidad.setText(
                unidadSeleccionada,
                false
            )

            // Cerrar el menú
            actUnidadCantidad.dismissDropDown()
        }

        actUnidadCantidad.setAdapter(
            adapterUnidades
        )


        // =========================================================
        // ANCHO DEL MENÚ
        // =========================================================

        val anchoMenu =
            (300 * resources.displayMetrics.density).toInt()

        actUnidadCantidad.dropDownWidth = anchoMenu

        actUnidadCantidad.setDropDownBackgroundDrawable(
            ContextCompat.getDrawable(
                this,
                R.drawable.bg_rectangulo_blanco
            )
        )


        // =========================================================
        // SELECCIÓN DE UNIDAD
        // =========================================================

        actUnidadCantidad.setOnItemClickListener {
                _,
                _,
                position,
                _ ->

            val seleccion = unidades[position]

            // Solo se permite seleccionar unidades,
            // no los títulos de categoría.
            if (seleccion !in titulos) {

                actUnidadCantidad.setText(
                    seleccion,
                    false
                )
            }
        }


        // =========================================================
        // BOTÓN FLECHA DE UNIDAD
        // =========================================================

        imgFlechaUnidad.setOnClickListener {

            actUnidadCantidad.requestFocus()

            actUnidadCantidad.showDropDown()
        }


        // =========================================================
        // CLIC SOBRE LA UNIDAD
        // =========================================================

        actUnidadCantidad.setOnClickListener {

            actUnidadCantidad.showDropDown()
        }


        // =========================================================
        // BOTÓN CERRAR
        // =========================================================

        val btnCerrar =
            view.findViewById<ImageButton>(
                R.id.btnCerrarIngrediente
            )

        btnCerrar.setOnClickListener {

            bottomSheet.dismiss()
        }


        // =========================================================
        // BOTÓN LIMPIAR
        // =========================================================

        val btnLimpiar =
            view.findViewById<MaterialButton>(
                R.id.btnCancelarIngrediente
            )

        btnLimpiar.setOnClickListener {

            // ==========================================
            // MODO EDICIÓN
            // ==========================================

            if (ingredienteEditar != null) {

                // ======================================
                // CONFIRMAR ELIMINACIÓN
                // ======================================

                val dialog = AlertDialog.Builder(this)
                    .setTitle("Eliminar ingrediente")
                    .setMessage(
                        "¿Estás seguro de que deseas eliminar " +
                                "\"${ingredienteEditar.ING_DESCRIPCION}\" " +
                                "de esta alacena?"
                    )
                    .setNegativeButton("CANCELAR", null)
                    .setPositiveButton("ELIMINAR") { _, _ ->

                        eliminarIngredienteDeAlacena(
                            ingredienteEditar.ALC_ID,
                            ingredienteEditar.ING_ID,
                            bottomSheet
                        )
                    }
                    .create()
                dialog.setOnShowListener {
                    // BOTÓN CANCELAR
                    dialog.getButton(
                        AlertDialog.BUTTON_NEGATIVE
                    ).setTextColor(
                        ContextCompat.getColor(
                            this,
                            R.color.azulgourmeet
                        )
                    )
                    // BOTÓN ELIMINAR
                    dialog.getButton(
                        AlertDialog.BUTTON_POSITIVE
                    ).setTextColor(
                        ContextCompat.getColor(
                            this,
                            R.color.azulgourmeet
                        )
                    )
                }

                dialog.show()

                return@setOnClickListener
            }


            // ==========================================
            // MODO AGREGAR
            // ==========================================

            val edtIngrediente =
                view.findViewById<TextInputEditText>(
                    R.id.edtIngrediente
                )

            val txtCantidad =
                view.findViewById<EditText>(
                    R.id.txtCantidad
                )

            val txtPrecioCompra =
                view.findViewById<EditText>(
                    R.id.txtPrecioCompra
                )


            edtIngrediente.text?.clear()

            txtCantidad.setText("500")

            actUnidadCantidad.setText(
                "gr",
                false
            )

            txtPrecioCompra.text?.clear()

            txtFechaCompra.text = "09/10/2026"

            txtFechaConsumo.text = "09/10/2026"
        }


        // =========================================================
        // BOTÓN AGREGAR
        // =========================================================

        val btnGuardar =
            view.findViewById<MaterialButton>(
                R.id.btnGuardarIngrediente
            )


        btnGuardar.setOnClickListener {
            if (modoIngrediente == ModoIngrediente.USAR_EN_RECETA) {

                eliminarIngredienteUsadoEnReceta(
                    ingredienteEditar!!,
                    bottomSheet
                )

                return@setOnClickListener
            }


            // ==========================================
            // CAMPOS
            // ==========================================

            val edtIngrediente =
                view.findViewById<EditText>(
                    R.id.edtIngrediente
                )

            val txtCantidad =
                view.findViewById<EditText>(
                    R.id.txtCantidad
                )

            val txtPrecioCompra =
                view.findViewById<EditText>(
                    R.id.txtPrecioCompra
                )

            val txtTipoEstado =
                view.findViewById<TextView>(
                    R.id.txtTipodeestado
                )

            val txtTipoAlmacenamiento =
                view.findViewById<TextView>(
                    R.id.txtTipodealmacenamiento
                )

            val txtTipoFrecuencia =
                view.findViewById<TextView>(
                    R.id.txtTipodeFrecuencia
                )

            val txtTipoAbastecimiento =
                view.findViewById<TextView>(
                    R.id.txtTipodeAbastecimiento
                )



            // ==========================================
            // OBTENER DATOS
            // ==========================================

            val ingrediente =
                edtIngrediente.text
                    ?.toString()
                    ?.trim()
                    .orEmpty()

            val cantidad =
                txtCantidad.text
                    ?.toString()
                    ?.trim()
                    .orEmpty()

            val unidad =
                actUnidadCantidad.text
                    ?.toString()
                    ?.trim()
                    .orEmpty()

            val precio =
                txtPrecioCompra.text
                    ?.toString()
                    ?.trim()
                    .orEmpty()


            // ==========================================
            // VALIDAR INGREDIENTE
            // ==========================================

            if (ingrediente.isEmpty()) {

                edtIngrediente.error =
                    "Ingresa un ingrediente"

                edtIngrediente.requestFocus()

                return@setOnClickListener
            }


            // ==========================================
            // VALIDAR INGREDIENTE SELECCIONADO
            // ==========================================

            if (ingredienteSeleccionadoId == null) {

                makeText(
                    this,
                    "Selecciona un ingrediente de la lista.",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }


            // ==========================================
            // VALIDAR ALACENA
            // ==========================================

            if (alacenaSeleccionada == null) {

                makeText(
                    this,
                    "Selecciona una alacena.",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }


            // ==========================================
            // VALIDAR CANTIDAD
            // ==========================================

            if (cantidad.isEmpty()) {

                txtCantidad.error =
                    "Ingresa una cantidad"

                txtCantidad.requestFocus()

                return@setOnClickListener
            }

            val cantidadDouble =
                cantidad.toDoubleOrNull()

            if (cantidadDouble == null) {

                txtCantidad.error =
                    "Ingresa una cantidad válida"

                txtCantidad.requestFocus()

                return@setOnClickListener
            }


            // ==========================================
            // VALIDAR UNIDAD
            // ==========================================

            if (unidad.isEmpty()) {

                makeText(
                    this,
                    "Selecciona una unidad.",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }


            // ==========================================
            // PRECIO
            // ==========================================

            val precioDouble =
                if (precio.isEmpty()) {

                    null

                } else {

                    precio.toDoubleOrNull()
                }


            // ==========================================
            // CREAR REQUEST
            // ==========================================

            val request =
                GuardarIngredienteAlacenaRequest(

                    ALC_ID =
                        alacenaSeleccionada!!.ALC_ID,

                    ING_ID =
                        ingredienteSeleccionadoId!!,

                    AI_CANTIDAD =
                        cantidadDouble,

                    AI_UNIDAD =
                        unidad,

                    AI_FECHA_COMPRA =
                        convertirFechaMySQL(
                            txtFechaCompra.text.toString()
                        ),

                    AI_FECHA_VENCIMIENTO =
                        convertirFechaMySQL(
                            txtFechaConsumo.text.toString()
                        ),

                    AI_ESTADO =
                        txtTipoEstado.text
                            .toString()
                            .trim()
                            .ifEmpty { null },

                    AI_PRECIO_COMPRA =
                        precioDouble,

                    AI_ALMACENAMIENTO =
                        txtTipoAlmacenamiento.text
                            .toString()
                            .trim()
                            .ifEmpty { null },

                    AI_FRECUENCIA_CONSUMO =
                        txtTipoFrecuencia.text
                            .toString()
                            .trim()
                            .ifEmpty { null },

                    AI_TIPO_ABASTECIMIENTO =
                        txtTipoAbastecimiento.text
                            .toString()
                            .trim()
                            .ifEmpty { null }
                )


            // ==========================================
            // MOSTRAR DATOS
            // ==========================================

            Log.d(
                "ALACENA_API",
                """
===== DATOS A ENVIAR =====
MODO: ${
                    if (ingredienteEditar == null)
                        "AGREGAR"
                    else
                        "EDITAR"
                }
ALC_ID: ${request.ALC_ID}
ING_ID: ${request.ING_ID}
CANTIDAD: ${request.AI_CANTIDAD}
UNIDAD: ${request.AI_UNIDAD}
FECHA COMPRA: ${request.AI_FECHA_COMPRA}
FECHA VENCIMIENTO: ${request.AI_FECHA_VENCIMIENTO}
ESTADO: ${request.AI_ESTADO}
PRECIO: ${request.AI_PRECIO_COMPRA}
ALMACENAMIENTO: ${request.AI_ALMACENAMIENTO}
FRECUENCIA: ${request.AI_FRECUENCIA_CONSUMO}
ABASTECIMIENTO: ${request.AI_TIPO_ABASTECIMIENTO}
==========================
""".trimIndent()
            )


            // ==========================================
            // CONECTAR CON LA API
            // ==========================================

            lifecycleScope.launch {

                try {

                    val response =

                        if (ingredienteEditar == null) {

                            // ==================================
                            // AGREGAR NUEVO INGREDIENTE
                            // ==================================

                            Log.d(
                                "ALACENA_API",
                                "Agregando ingrediente..."
                            )

                            ApiClient.apiService
                                .guardarIngredienteAlacena(
                                    request
                                )

                        } else {

                            // ==================================
                            // EDITAR INGREDIENTE EXISTENTE
                            // ==================================

                            Log.d(
                                "ALACENA_API",
                                "Editando ingrediente..."
                            )

                            ApiClient.apiService
                                .editarIngredienteAlacena(
                                    request
                                )
                        }


                    // ==========================================
                    // RESPUESTA EXITOSA
                    // ==========================================

                    if (response.success) {

                        Log.d(
                            "ALACENA_API",
                            "ÉXITO: ${response.message}"
                        )

                        makeText(
                            this@Menu_principal_free,
                            response.message,
                            Toast.LENGTH_SHORT
                        ).show()


                        // ======================================
                        // CERRAR BOTTOM SHEET
                        // ======================================

                        bottomSheet.dismiss()


                        // ======================================
                        // RECARGAR ALACENA
                        // ======================================

                        alacenaSeleccionada?.let {
                            actualizarEstadosYCargarAlacena(
                                alacenaSeleccionada!!.ALC_ID,
                                alacenaSeleccionada!!.ALC_CLI_ID
                            )
                        }


                    } else {

                        // ======================================
                        // ERROR DE LA API
                        // ======================================

                        Log.e(
                            "ALACENA_API",
                            "ERROR: ${response.message}"
                        )

                        makeText(
                            this@Menu_principal_free,
                            response.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                } catch (e: Exception) {

                    // ==========================================
                    // ERROR DE CONEXIÓN
                    // ==========================================

                    Log.e(
                        "ALACENA_API",
                        "Error al conectar con la API",
                        e
                    )

                    makeText(
                        this@Menu_principal_free,
                        "Error de conexión con el servidor.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        // =========================================================
// CONFIGURACIÓN DE BOTONES SEGÚN EL MODO
// =========================================================

// =========================================================
// AGREGAR NUEVO INGREDIENTE
// =========================================================

        when (modoIngrediente) {

            ModoIngrediente.AGREGAR -> {

                btnLimpiar.text = "LIMPIAR"
                btnGuardar.text = "AGREGAR"

                vermasrecetas.visibility = View.GONE
                txtcontenedeor.visibility = View.GONE
            }

            ModoIngrediente.EDITAR -> {

                btnLimpiar.text = "ELIMINAR"
                btnGuardar.text = "GUARDAR CAMBIOS"

                vermasrecetas.visibility = View.VISIBLE
                txtcontenedeor.visibility = View.GONE
            }

            ModoIngrediente.USAR_EN_RECETA -> {

                vermasrecetas.visibility = View.GONE
                txtcontenedeor.visibility = View.VISIBLE

                btnLimpiar.text = "ELIMINAR"

                btnLimpiar.setTextColor(
                    ContextCompat.getColor(
                        this,
                        android.R.color.white
                    )
                )
                cardFechadecon.setCardBackgroundColor(
                    ContextCompat.getColor(
                        this,
                        R.color.rojo
                    )
                )
                btnLimpiar.backgroundTintList =
                    ContextCompat.getColorStateList(
                        this,
                        R.color.rojo
                    )

                btnGuardar.text = "LO USÉ EN UNA RECETA"
            }
        }

        // =========================================================
        // MOSTRAR BOTTOM SHEET
        // =========================================================


        // =========================================================
// SELECTOR DE TIPO DE ALMACENAMIENTO
// =========================================================



        val imgFlechaAlmacenamiento =
            view.findViewById<ImageView>(
                R.id.imgFlechaalmacenamiento
            )


// =========================================================
// OPCIONES DE ALMACENAMIENTO
// =========================================================

        val tiposAlmacenamiento = mutableListOf(
            "Refrigerador",
            "Ambiente",
            "Congelador"
        )


// =========================================================
// ADAPTER
// =========================================================

        val adapterAlmacenamiento = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            tiposAlmacenamiento
        )


// =========================================================
// POPUP
// =========================================================

        val popupAlmacenamiento = PopupWindow(
            view.context
        )

        val listaAlmacenamiento =
            ListView(view.context)

        listaAlmacenamiento.adapter =
            adapterAlmacenamiento

        listaAlmacenamiento.divider = null

        listaAlmacenamiento.setPadding(
            0,
            8,
            0,
            8
        )

        popupAlmacenamiento.contentView =
            listaAlmacenamiento

        popupAlmacenamiento.width =
            (220 * resources.displayMetrics.density).toInt()

        WindowManager.LayoutParams.WRAP_CONTENT.also { popupAlmacenamiento.height = it }

        popupAlmacenamiento.isFocusable = true
        popupAlmacenamiento.isOutsideTouchable = true

        popupAlmacenamiento.setBackgroundDrawable(
            ContextCompat.getDrawable(
                this,
                R.drawable.bg_rectangulo_blanco
            )
        )


// =========================================================
// SELECCIONAR ALMACENAMIENTO
// =========================================================
        // =========================================================
// CONFIGURAR ALMACENAMIENTO SEGÚN LA FAMILIA
// =========================================================

        fun actualizarTipoAlmacenamiento() {

            val familia =
                familiaIngredienteSeleccionado
                    ?.trim()
                    ?.lowercase()


            // ==================================================
            // IDENTIFICAR TIPO DE FAMILIA
            // ==================================================
            val esLicorODestilado =
                familia == "licores y destilados"

            val esNoPerecedero =
                familia == "cereales leguminosas" ||
                        familia == "especias" ||
                        familia == "pastas" ||
                        familia == "semillas"

            val esIndustrializadoOAbarrote =
                familia == "industrializados" ||
                        familia == "abarrotes"

            // ==================================================
// LICORES Y DESTILADOS
// ==================================================

            if (esLicorODestilado) {

                // Por defecto
                if (
                    txtTipoAlmacenamiento.text
                        .toString()
                        .trim()
                        .isEmpty()
                ) {

                    txtTipoAlmacenamiento.text =
                        "Ambiente"
                }


                txtTipoAlmacenamiento.isClickable =
                    true

                txtTipoAlmacenamiento.isFocusable =
                    true


                imgFlechaAlmacenamiento.isEnabled =
                    true

                imgFlechaAlmacenamiento.alpha =
                    1.0f


                // Solo Ambiente y Refrigerador
                adapterAlmacenamiento.clear()

                adapterAlmacenamiento.addAll(
                    listOf(
                        "Ambiente",
                        "Refrigerador"
                    )
                )

                adapterAlmacenamiento.notifyDataSetChanged()

                return
            }


            // ==================================================
            // NO PERECEDEROS
            // SOLO AMBIENTE
            // ==================================================

            if (esNoPerecedero) {

                txtTipoAlmacenamiento.text =
                    "Ambiente"


                // Bloquear texto
                txtTipoAlmacenamiento.isClickable =
                    false

                txtTipoAlmacenamiento.isFocusable =
                    false


                // Bloquear flecha
                imgFlechaAlmacenamiento.isEnabled =
                    false

                imgFlechaAlmacenamiento.alpha =
                    0.4f


                // Cerrar popup
                popupAlmacenamiento.dismiss()


                // Solo Ambiente
                adapterAlmacenamiento.clear()

                adapterAlmacenamiento.add(
                    "Ambiente"
                )

                adapterAlmacenamiento.notifyDataSetChanged()


                return
            }


            // ==================================================
            // INDUSTRIALIZADOS / ABARROTES
            // AMBIENTE O CONGELADOR
            // ==================================================

            if (esIndustrializadoOAbarrote) {

                // Si actualmente tiene Refrigerador,
                // cambiarlo automáticamente a Ambiente.

                if (
                    txtTipoAlmacenamiento.text
                        .toString()
                        .trim()
                        .equals(
                            "Refrigerador",
                            ignoreCase = true
                        )
                ) {

                    txtTipoAlmacenamiento.text =
                        "Ambiente"
                }


                // Permitir seleccionar
                txtTipoAlmacenamiento.isClickable =
                    true

                txtTipoAlmacenamiento.isFocusable =
                    true


                imgFlechaAlmacenamiento.isEnabled =
                    true

                imgFlechaAlmacenamiento.alpha =
                    1.0f


                // ==============================================
                // OPCIONES
                // ==============================================

                adapterAlmacenamiento.clear()

                adapterAlmacenamiento.addAll(
                    listOf(
                        "Ambiente",
                        "Congelador"
                    )
                )

                adapterAlmacenamiento.notifyDataSetChanged()


                return
            }


            // ==================================================
            // RESTO DE FAMILIAS
            // REFRIGERADOR / AMBIENTE / CONGELADOR
            // ==================================================

            txtTipoAlmacenamiento.isClickable =
                true

            txtTipoAlmacenamiento.isFocusable =
                true


            imgFlechaAlmacenamiento.isEnabled =
                true

            imgFlechaAlmacenamiento.alpha =
                1.0f


            // ==============================================
            // RESTAURAR TODAS LAS OPCIONES
            // ==============================================

            adapterAlmacenamiento.clear()

            adapterAlmacenamiento.addAll(
                listOf(
                    "Refrigerador",
                    "Ambiente",
                    "Congelador"
                )
            )

            adapterAlmacenamiento.notifyDataSetChanged()
        }

        listaAlmacenamiento.setOnItemClickListener {
                _,
                _,
                position,
                _ ->

            val seleccion =
                tiposAlmacenamiento[position]

            txtTipoAlmacenamiento.text =
                seleccion
            actualizarFechaCaducidad(
                txtFechaCompra,
                txtTipoEstado,
                txtTipoAlmacenamiento,
                txtFechaConsumo,
                edtIngrediente
            )

            popupAlmacenamiento.dismiss()
        }


// =========================================================
// ABRIR CON LA FLECHA
// =========================================================

        imgFlechaAlmacenamiento.setOnClickListener {

            val familia =
                familiaIngredienteSeleccionado
                    ?.trim()
                    ?.lowercase()

            val esNoPerecedero =
                familia == "cereales leguminosas" ||
                        familia == "especias" ||
                        familia == "pastas" ||
                        familia == "semillas"

            if (esNoPerecedero) {
                return@setOnClickListener
            }

            ocultarTeclado(imgFlechaAlmacenamiento)

            popupAlmacenamiento.showAsDropDown(
                imgFlechaAlmacenamiento,
                -180,
                5
            )
        }


// =========================================================
// TAMBIÉN ABRIR AL TOCAR EL TEXTO
// =========================================================

        txtTipoAlmacenamiento.setOnClickListener {

            val familia =
                familiaIngredienteSeleccionado
                    ?.trim()
                    ?.lowercase()

            val esNoPerecedero =
                familia == "cereales leguminosas" ||
                        familia == "especias" ||
                        familia == "pastas" ||
                        familia == "semillas"

            if (esNoPerecedero) {
                return@setOnClickListener
            }

            popupAlmacenamiento.showAsDropDown(
                txtTipoAlmacenamiento,
                -180,
                5
            )
        }
        // =========================================================
// SELECTOR DE ESTADO
// =========================================================



        val imgFlechaEstado =
            view.findViewById<ImageView>(
                R.id.imgFlechaestado
            )


// =========================================================
// OPCIONES DE ESTADO
// =========================================================
        val estados = mutableListOf(
            "Fresco",
            "Maduro",
            "Pasado",
            "Sellado",
            "Descompuesto"
        )

// =========================================================
// ADAPTER
// =========================================================

        val adapterEstados = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            estados
        )


        fun actualizarListaEstados() {

            val familia =
                familiaIngredienteSeleccionado
                    ?.trim()
                    ?.lowercase()

            Log.d(
                "ESTADOS",
                "Familia recibida: [$familia]"
            )

            estados.clear()


            // ==================================================
            // FRUTAS Y VERDURAS
            // ==================================================

            if (
                familia == "fruta" ||
                familia == "frutas" ||
                familia == "verdura" ||
                familia == "verduras"
            ) {

                estados.addAll(
                    listOf(
                        "Verde",
                        "Fresco",
                        "Maduro",
                        "Pasado",
                        "Sellado",
                        "Descompuesto"
                    )
                )

                txtTipoEstado.text = "Verde"
            }


            // ==================================================
            // NO PERECEDEROS
            // ==================================================

            else if (
                familia == "cereales leguminosas" ||
                familia == "especias" ||
                familia == "pastas" ||
                familia == "semillas"
            ) {

                estados.addAll(
                    listOf(
                        "No perecedero",
                        "Pasado",
                        "Sellado",
                        "Descompuesto"
                    )
                )

                txtTipoEstado.text = "Sellado"
            }

            else if (
                familia == "embutidos"
            ) {
                estados.addAll(
                    listOf(
                        "Verde",
                        "Maduro",
                        "Pasado",
                        "Sellado",
                        "Descompuesto"
                    )
                )

                txtTipoEstado.text = "Verde"
            }


            // ==================================================
            // INDUSTRIALIZADOS Y HIERBAS AROMÁTICAS
            // ==================================================

            else if (
                familia == "industrializados" ||
                familia == "hierbas aromatica"
            ) {

                estados.addAll(
                    listOf(
                        "Fresco",
                        "Maduro",
                        "Pasado",
                        "Sellado",
                        "Descompuesto"
                    )
                )

                txtTipoEstado.text = "Sellado"
            }

            // ==================================================
// LICORES Y DESTILADOS
// ==================================================

            else if (
                familia == "licores y destilados"
            ) {

                val nombreIngrediente =
                    edtIngrediente.text
                        .toString()
                        .trim()
                        .lowercase()


                // ==================================================
                // VINO
                // ==================================================

                if (nombreIngrediente == "vino") {

                    estados.addAll(
                        listOf(
                            "Fresco",
                            "Maduro",
                            "Pasado",
                            "Sellado",
                            "Descompuesto"
                        )
                    )

                    txtTipoEstado.text = "Sellado"
                }


                // ==================================================
                // DEMÁS LICORES Y DESTILADOS
                // ==================================================

                else {

                    estados.add(
                        "Sellado"
                    )

                    txtTipoEstado.text =
                        "Sellado"
                }
            }

            // ==================================================
            // LÁCTEOS
            // ==================================================

            else if (
                familia == "lacteos"
            ) {

                estados.addAll(
                    listOf(
                        "Fresco",
                        "Maduro",
                        "Pasado",
                        "Sellado",
                        "Descompuesto"
                    )
                )

                txtTipoEstado.text = "Fresco"
            }// ==================================================
// ABARROTES
// ==================================================

            else if (
                familia == "abarrotes"
            ) {

                estados.addAll(
                    listOf(
                        "Fresco",
                        "Maduro",
                        "Pasado",
                        "Sellado",
                        "Descompuesto"
                    )
                )

                // Estado inicial
                txtTipoEstado.text = "Sellado"
            }




            // ==================================================
            // DEMÁS FAMILIAS
            // ==================================================

            else {

                estados.addAll(
                    listOf(
                        "Fresco",
                        "Maduro",
                        "Pasado",
                        "Sellado",
                        "Descompuesto"
                    )
                )

                txtTipoEstado.text = "Fresco"
            }


            Log.d(
                "ESTADOS",
                "Estados actuales: $estados"
            )

            Log.d(
                "ESTADOS",
                "Estado por defecto: ${txtTipoEstado.text}"
            )

            adapterEstados.notifyDataSetChanged()
        }

// =========================================================
// LISTA DESPLEGABLE
// =========================================================

        val listaEstados = ListView(this)

        listaEstados.adapter = adapterEstados

        listaEstados.divider = null

        listaEstados.setPadding(
            0,
            5,
            0,
            5
        )


// =========================================================
// POPUP
// =========================================================

        val popupEstado = PopupWindow(
            listaEstados,
            (180 * resources.displayMetrics.density).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT,
            true
        )

        popupEstado.setBackgroundDrawable(
            ContextCompat.getDrawable(
                this,
                R.drawable.bg_rectangulo_blanco
            )
        )

        popupEstado.isOutsideTouchable = true
        popupEstado.elevation = 8f


// =========================================================
// SELECCIONAR ESTADO
// =========================================================

        listaEstados.setOnItemClickListener {
                _,
                _,
                position,
                _ ->

            val estadoSeleccionado = estados[position]

            txtTipoEstado.text =
                estadoSeleccionado

            if (estadoSeleccionado == "Sellado") {

                // El usuario tendrá que introducir
                // manualmente la fecha de caducidad.

            } else {

                actualizarFechaCaducidad(
                    txtFechaCompra,
                    txtTipoEstado,
                    txtTipoAlmacenamiento,
                    txtFechaConsumo,
                    edtIngrediente
                )
            }

            popupEstado.dismiss()
        }


// =========================================================
// ABRIR CON LA FLECHA
// =========================================================

        imgFlechaEstado.setOnClickListener {
            ocultarTeclado(imgFlechaEstado)

            popupEstado.showAsDropDown(
                imgFlechaEstado,
                -150,
                5
            )
        }


// =========================================================
// ABRIR AL TOCAR EL TEXTO
// =========================================================

        txtTipoEstado.setOnClickListener {

            popupEstado.showAsDropDown(
                txtTipoEstado,
                -150,
                5
            )
        }
        // =========================================================
// SELECTOR DE FRECUENCIA
// =========================================================

        val txtTipoFrecuencia =
            view.findViewById<TextView>(
                R.id.txtTipodeFrecuencia
            )

        val imgFlechaFrecuencia =
            view.findViewById<ImageView>(
                R.id.imgFlechaFrecuencia
            )


// =========================================================
// OPCIONES DE FRECUENCIA
// =========================================================

        val frecuencias = listOf(
            "Diariamente",
            "De vez en cuando",
            "+ de 2 veces por semana",
            "Ocasionalmente — 2 a 3 por semana",
            "Poco habitual — 1 vez por semana"
        )


// =========================================================
// ADAPTER
// =========================================================

        val adapterFrecuencia = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            frecuencias
        )


// =========================================================
// LISTA DESPLEGABLE
// =========================================================

        val listaFrecuencia = ListView(this)

        listaFrecuencia.adapter = adapterFrecuencia

        listaFrecuencia.divider = null

        listaFrecuencia.setPadding(
            0,
            5,
            0,
            5
        )


// =========================================================
// POPUP
// =========================================================

        val popupFrecuencia = PopupWindow(
            listaFrecuencia,
            (250 * resources.displayMetrics.density).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT,
            true
        )

        popupFrecuencia.setBackgroundDrawable(
            ContextCompat.getDrawable(
                this,
                R.drawable.bg_rectangulo_blanco
            )
        )

        popupFrecuencia.isOutsideTouchable = true
        popupFrecuencia.elevation = 8f


// =========================================================
// SELECCIONAR FRECUENCIA
// =========================================================

        listaFrecuencia.setOnItemClickListener {
                _,
                _,
                position,
                _ ->

            val frecuenciaSeleccionada =
                frecuencias[position]

            txtTipoFrecuencia.text =
                frecuenciaSeleccionada

            popupFrecuencia.dismiss()
        }


// =========================================================
// ABRIR CON LA FLECHA
// =========================================================

        imgFlechaFrecuencia.setOnClickListener {
            ocultarTeclado(imgFlechaFrecuencia)
            popupFrecuencia.showAsDropDown(
                imgFlechaFrecuencia,
                -220,
                5
            )
        }


// =========================================================
// ABRIR AL TOCAR EL TEXTO
// =========================================================

        txtTipoFrecuencia.setOnClickListener {

            popupFrecuencia.showAsDropDown(
                txtTipoFrecuencia,
                -220,
                5
            )
        }
        // =========================================================
// SELECTOR DE ABASTECIMIENTO
// =========================================================

        val txtTipoAbastecimiento =
            view.findViewById<TextView>(
                R.id.txtTipodeAbastecimiento
            )

        val imgFlechaAbastecimiento =
            view.findViewById<ImageView>(
                R.id.imgFlechaAbastecimiento
            )


// =========================================================
// OPCIONES DE ABASTECIMIENTO
// =========================================================

        val abastecimientos = listOf(
            "De un solo uso",
            "Compra minorista (200 gr a 900 gr)",
            "Por porción grande (1 kg a 4 kg)",
            "A granel",
            "Mayoreo (+ 5 kg)"
        )


// =========================================================
// ADAPTER
// =========================================================

        val adapterAbastecimiento = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            abastecimientos
        )


// =========================================================
// LISTA DESPLEGABLE
// =========================================================

        val listaAbastecimiento = ListView(this)

        listaAbastecimiento.adapter =
            adapterAbastecimiento

        listaAbastecimiento.divider = null

        listaAbastecimiento.setPadding(
            0,
            5,
            0,
            5
        )


// =========================================================
// POPUP
// =========================================================

        val popupAbastecimiento = PopupWindow(
            listaAbastecimiento,
            (300 * resources.displayMetrics.density).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT,
            true
        )

        popupAbastecimiento.setBackgroundDrawable(
            ContextCompat.getDrawable(
                this,
                R.drawable.bg_rectangulo_blanco
            )
        )

        popupAbastecimiento.isOutsideTouchable = true
        popupAbastecimiento.elevation = 8f


// =========================================================
// SELECCIONAR ABASTECIMIENTO
// =========================================================

        listaAbastecimiento.setOnItemClickListener {
                _,
                _,
                position,
                _ ->

            val abastecimientoSeleccionado =
                abastecimientos[position]

            txtTipoAbastecimiento.text =
                abastecimientoSeleccionado

            popupAbastecimiento.dismiss()
        }


// =========================================================
// ABRIR CON LA FLECHA
// =========================================================

        imgFlechaAbastecimiento.setOnClickListener {
            ocultarTeclado(imgFlechaAbastecimiento)
            popupAbastecimiento.showAsDropDown(
                imgFlechaAbastecimiento,
                -270,
                5
            )
        }


// =========================================================
// ABRIR AL TOCAR EL TEXTO
// =========================================================

        txtTipoAbastecimiento.setOnClickListener {

            popupAbastecimiento.showAsDropDown(
                txtTipoAbastecimiento,
                -270,
                5
            )
        }
        val rvResultadosIngrediente =
            view.findViewById<RecyclerView>(
                R.id.rvResultadosIngrediente
            )


        val contenedorResultados =
            view.findViewById<View>(
                R.id.contenedorResultadosIngrediente
            )
        rvResultadosIngrediente.layoutManager =
            LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL,
                false
            )
        var ingredienteSeleccionadoManualmente = false


        val adapterIngredientes =
            IngredienteMiniAdapter(
                emptyList()
            ) { ingredienteSeleccionado ->

                // ==========================================
                // INDICAR QUE SE SELECCIONÓ MANUALMENTE
                // ==========================================

                ingredienteSeleccionadoManualmente = true


                // ==========================================
                // COLOCAR NOMBRE
                // ==========================================

                edtIngrediente.setText(
                    ingredienteSeleccionado.nombre
                )
                ocultarTeclado(edtIngrediente)


                // ==========================================
                // COLOCAR IMAGEN
                // ==========================================

                if (
                    !ingredienteSeleccionado
                        .imagen_url
                        .isNullOrEmpty()
                ) {

                    Glide.with(this)
                        .load(
                            ingredienteSeleccionado.imagen_url
                        )
                        .placeholder(
                            R.drawable.ic_ingredientes
                        )
                        .error(
                            R.drawable.ic_ingredientes
                        )
                        .into(
                            txtEmojiIngrediente
                        )

                } else {

                    txtEmojiIngrediente.setImageResource(
                        R.drawable.ic_ingredientes
                    )
                }


                // ==========================================
                // GUARDAR ID DEL INGREDIENTE
                // ==========================================

                ingredienteSeleccionadoId =
                    ingredienteSeleccionado.id

                familiaIngredienteSeleccionado =
                    ingredienteSeleccionado.categoria

                actualizarListaEstados()
                actualizarTipoAlmacenamiento()


                // ==========================================
                // OCULTAR RESULTADOS
                // ==========================================

                contenedorResultados.visibility =
                    View.GONE


                // ==========================================
                // LIMPIAR RESULTADOS
                // ==========================================

                /*adapterIngredientes.actualizarLista(
                    emptyList()
                )*/
            }

        rvResultadosIngrediente.adapter =
            adapterIngredientes
        edtIngrediente.addTextChangedListener(

            object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {

                    // ==========================================
                    // CARGANDO INGREDIENTE DEL MODO EDICIÓN
                    // ==========================================

                    if (cargandoIngredienteEditar) {

                        return
                    }


                    // ==========================================
                    // SI ACABA DE SELECCIONAR UN INGREDIENTE
                    // ==========================================

                    if (ingredienteSeleccionadoManualmente) {

                        ingredienteSeleccionadoManualmente =
                            false

                        return
                    }


                    // ==========================================
                    // TEXTO ESCRITO POR EL USUARIO
                    // ==========================================

                    val busqueda =
                        s?.toString()
                            ?.trim()
                            .orEmpty()


                    // ==========================================
                    // CAMPO VACÍO
                    // ==========================================

                    if (busqueda.isEmpty()) {

                        contenedorResultados.visibility =
                            View.GONE

                        adapterIngredientes.actualizarLista(
                            emptyList()
                        )

                        ingredienteSeleccionadoId =
                            null

                        return
                    }


                    // ==========================================
                    // BUSCAR INGREDIENTES
                    // ==========================================

                    buscarIngredientes(
                        busqueda,
                        adapterIngredientes,
                        contenedorResultados
                    )
                }


                override fun afterTextChanged(
                    s: Editable?
                ) {
                }
            }
        )
        // =========================================================
// MODO EDICIÓN
// =========================================================

        if (ingredienteEditar != null) {

            val contenedorResultadosIngrediente =
                view.findViewById<View>(
                    R.id.contenedorResultadosIngrediente
                )

            contenedorResultadosIngrediente.visibility =
                View.GONE
            familiaIngredienteSeleccionado =
                ingredienteEditar.categoria
            actualizarListaEstados()
            actualizarTipoAlmacenamiento()

            // =====================================================
            // TÍTULO
            // =====================================================

            txtTituloIngrediente.text =
                "EDITAR INGREDIENTE"


            // =====================================================
            // ID DEL INGREDIENTE
            // =====================================================

            ingredienteSeleccionadoId =
                ingredienteEditar.ING_ID


            // =====================================================
            // NOMBRE DEL INGREDIENTE
            // =====================================================

            edtIngrediente.setText(
                ingredienteEditar.ING_DESCRIPCION
            )

            // =====================================================
            // IMAGEN DEL INGREDIENTE
            // =====================================================

            if (
                !ingredienteEditar.Foto_Ingrediente
                    .isNullOrEmpty()
            ) {

                Glide.with(this)
                    .load(
                        ingredienteEditar.Foto_Ingrediente
                    )
                    .placeholder(
                        R.drawable.ic_ingredientes
                    )
                    .error(
                        R.drawable.ic_ingredientes
                    )
                    .into(
                        txtEmojiIngrediente
                    )

            } else {

                txtEmojiIngrediente.setImageResource(
                    R.drawable.ic_ingredientes
                )
            }


            // =====================================================
            // CANTIDAD
            // =====================================================

            val cantidad =
                ingredienteEditar.AI_CANTIDAD

            txtCantidad.setText(
                if (cantidad % 1.0 == 0.0) {
                    cantidad.toInt().toString()
                } else {
                    cantidad.toString()
                }
            )


            // =====================================================
            // UNIDAD
            // =====================================================

            actUnidadCantidad.setText(
                ingredienteEditar.AI_UNIDAD,
                false
            )


            // =====================================================
            // PRECIO
            // =====================================================

            if (
                ingredienteEditar.AI_PRECIO_COMPRA != null
            ) {

                val precio =
                    ingredienteEditar.AI_PRECIO_COMPRA

                txtPrecioCompra.setText(
                    if (precio % 1.0 == 0.0) {
                        precio.toInt().toString()
                    } else {
                        precio.toString()
                    }
                )

            } else {

                txtPrecioCompra.setText("")
            }


            // =====================================================
            // FECHA DE COMPRA
            // =====================================================

            txtFechaCompra.text =
                formatearFechaDialogo(
                    ingredienteEditar.AI_FECHA_COMPRA
                )


            // =====================================================
            // FECHA DE CADUCIDAD
            // =====================================================

            txtFechaConsumo.text =
                formatearFechaDialogo(
                    ingredienteEditar.AI_FECHA_VENCIMIENTO
                )


            // =====================================================
            // ESTADO
            // =====================================================

            txtTipoEstado.text =
                ingredienteEditar.AI_ESTADO
                    ?: ""


            // =====================================================
            // ALMACENAMIENTO
            // =====================================================

            txtTipoAlmacenamiento.text =
                ingredienteEditar.AI_ALMACENAMIENTO
                    ?: ""


            // =====================================================
            // FRECUENCIA
            // =====================================================

            val txtTipoFrecuencia =
                view.findViewById<TextView>(
                    R.id.txtTipodeFrecuencia
                )

            txtTipoFrecuencia.text =
                ingredienteEditar.AI_FRECUENCIA_CONSUMO
                    ?: ""


            // =====================================================
            // ABASTECIMIENTO
            // =====================================================

            val txtTipoAbastecimiento =
                view.findViewById<TextView>(
                    R.id.txtTipodeAbastecimiento
                )

            txtTipoAbastecimiento.text =
                ingredienteEditar.AI_TIPO_ABASTECIMIENTO
                    ?: ""


            // =====================================================
            // ALACENA
            // =====================================================

            val alacenaEditar =
                listaAlacenas.find {
                    it.ALC_ID ==
                            ingredienteEditar.ALC_ID
                }

            if (alacenaEditar != null) {

                txtAlacenaSeleccionada.text =
                    alacenaEditar.ALC_NOMBRE


                // =================================================
                // ICONO DE LA ALACENA
                // =================================================

                when (
                    alacenaEditar.ALC_ICONO
                        ?.trim()
                        ?.uppercase()
                ) {

                    "CASA" -> {

                        imgIconoAlacenaSeleccionada
                            .setImageResource(
                                R.drawable.ic_casa2
                            )
                    }

                    "OFICINA" -> {

                        imgIconoAlacenaSeleccionada
                            .setImageResource(
                                R.drawable.ic_casa2
                            )
                    }

                    "REFRIGERADOR" -> {

                        imgIconoAlacenaSeleccionada
                            .setImageResource(
                                R.drawable.ic_casa_azul
                            )
                    }
                }
            }
        }
        cargandoIngredienteEditar = false
        bottomSheet.show()
    }
    private fun buscarIngredientes(
        busqueda: String,
        adapter: IngredienteMiniAdapter,
        contenedorResultados: View
    ) {

        lifecycleScope.launch {

            try {

                Log.d(
                    "INGREDIENTE",
                    "Buscando clasificación: $busqueda"
                )

                val respuesta =
                    ApiClient.apiService
                        .buscarIngredientesClasificacion(
                            busqueda
                        )

                if (
                    respuesta.success &&
                    !respuesta.ingredientes.isNullOrEmpty()
                ) {

                    adapter.actualizarLista(
                        respuesta.ingredientes.map { ingrediente ->

                            BuscarIngredientes(
                                id = ingrediente.id,
                                nombre = ingrediente.nombre,
                                imagen_url = ingrediente.imagen_url,
                                categoria = ingrediente.categoria,
                                familia = ingrediente.familia
                            )
                        }
                    )

                    contenedorResultados.visibility =
                        View.VISIBLE

                    Log.d(
                        "INGREDIENTE",
                        "Resultados: ${respuesta.count}"
                    )

                } else {

                    adapter.actualizarLista(
                        emptyList()
                    )

                    contenedorResultados.visibility =
                        View.GONE
                }

            } catch (e: Exception) {

                Log.e(
                    "INGREDIENTE",
                    "Error al buscar ingredientes",
                    e
                )

                adapter.actualizarLista(
                    emptyList()
                )

                contenedorResultados.visibility =
                    View.GONE
            }
        }
    }
    private fun ocultarTeclado(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE)
                as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
        // Quitar el foco del campo que estaba escribiendo
        view.clearFocus()
    }
    private fun mostrarMenuMisAlacenas2(
        txtAlacena: TextView
    ) {

        val bottomSheet = BottomSheetDialog(this)

        val view = layoutInflater.inflate(
            R.layout.dialog_mis_alacenas,
            null
        )

        bottomSheet.setContentView(view)

        // ==========================================
        // RECYCLERVIEW
        // ==========================================

        val rvMisAlacenas =
            view.findViewById<RecyclerView>(
                R.id.rvMisAlacenas
            )

        rvMisAlacenas.layoutManager =
            LinearLayoutManager(this)

        // ==========================================
        // ADAPTER
        // ==========================================

        val adapter = AlacenaMenuAdapter(

            lista = listaAlacenas,

            // ======================================
            // SELECCIONAR ALACENA
            // ======================================

            onSeleccionar = { alacena ->

                // Guardar alacena seleccionada
                alacenaSeleccionada = alacena

                // Cambiar nombre visualmente
                txtAlacena.text =
                    alacena.ALC_NOMBRE

                // Mostrar ID seleccionado en Log
                Log.d(
                    "ALACENA",
                    "Nueva alacena seleccionada: ${alacena.ALC_ID}"
                )

                // Cerrar menú
                bottomSheet.dismiss()
            },

            // ======================================
            // EDITAR ALACENA
            // ======================================

            onEditar = { alacena ->

                bottomSheet.dismiss()

                mostrarDialogEditarAlacena(alacena)
            },

            // ======================================
            // ELIMINAR ALACENA
            // ======================================

            onEliminar = { alacena ->

                bottomSheet.dismiss()

                mostrarConfirmacionEliminarAlacena(alacena)
            }
        )

        rvMisAlacenas.adapter = adapter

        // ==========================================
        // CONFIGURAR BOTTOM SHEET
        // ==========================================

        bottomSheet.setOnShowListener {

            val dialog = it as BottomSheetDialog

            val sheet =
                dialog.findViewById<View>(
                    com.google.android.material.R.id.design_bottom_sheet
                )

            sheet?.let { bottomSheetView ->

                val behavior =
                    BottomSheetBehavior.from(
                        bottomSheetView
                    )

                behavior.state =
                    BottomSheetBehavior.STATE_EXPANDED

                behavior.skipCollapsed = true

                bottomSheetView.background =
                    ContextCompat.getDrawable(
                        this,
                        R.drawable.bg_bottom_sheet_alacena
                    )
            }
        }

        // ==========================================
        // MOSTRAR
        // ==========================================

        bottomSheet.show()
    }
    private fun convertirFechaMySQL(
        fecha: String
    ): String? {

        return try {

            val formatoEntrada =
                SimpleDateFormat(
                    "dd/MM/yyyy",
                    Locale.getDefault()
                )

            val formatoSalida =
                SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.getDefault()
                )

            val date =
                formatoEntrada.parse(fecha)

            if (date != null) {
                formatoSalida.format(date)
            } else {
                null
            }

        } catch (e: Exception) {

            null
        }
    }
    private fun cargarIngredientesAlacena(
        alcId: Int,
        clienteId: Int
    ) {
        lifecycleScope.launch {
            try {

                Log.d(
                    "ALACENA_ING",
                    "Consultando ingredientes. ALC_ID=$alcId, CLI_ID=$clienteId"
                )

                val request = ListarIngredientesAlacenaRequest(
                    ALC_ID = alcId,
                    ALC_CLI_ID = clienteId
                )

                val respuesta =
                    ApiClient.apiService.listarIngredientesAlacena(request)

                Log.d(
                    "ALACENA_ING",
                    "Respuesta: success=${respuesta.success}, total=${respuesta.total}"
                )

                if (respuesta.success) {

                    val todosLosIngredientes = respuesta.ingredientes

                    Log.d(
                        "ALACENA_ING",
                        "Ingredientes recibidos: ${todosLosIngredientes.size}"
                    )

                    // ==========================================
                    // MIS INGREDIENTES
                    // ==========================================

                    // ==========================================
// MIS INGREDIENTES POR CATEGORÍA
// ==========================================

                    val ingredientesPorCategoria =
                        todosLosIngredientes
                            .groupBy {
                                it.categoria
                                    ?.trim()
                                    ?.ifEmpty { "Sin categoría" }
                                    ?: "Sin categoría"
                            }
                            .map { (categoria, ingredientes) ->

                                CategoriaIngredientesAdapter.CategoriaIngredientes(
                                    nombreCategoria = categoria,
                                    ingredientes = ingredientes
                                )
                            }

// Actualizar adapter de categorías
                    adapterCategoriasIngredientes.actualizarLista(
                        ingredientesPorCategoria
                    )

                    // ==========================================
                    // CONSUME PRIMERO
                    // ==========================================

                    val consumePrimero = todosLosIngredientes
                        .filter {
                            val estado = it.AI_ESTADO
                                ?.trim()
                                ?.lowercase()

                            estado == "pasado" || estado == "descompuesto"
                        }
                        .sortedWith(
                            compareBy<IngredienteAlacena> {
                                when (
                                    it.AI_ESTADO
                                        ?.trim()
                                        ?.lowercase()
                                ) {
                                    "descompuesto" -> 0
                                    "pasado" -> 1
                                    else -> 2
                                }
                            }.thenBy {
                                it.AI_FECHA_VENCIMIENTO
                            }
                        )

                    adapterConsumePrimero.actualizarLista(
                        consumePrimero
                    )

                    // ==========================================
                    // LOGS
                    // ==========================================

                    Log.d(
                        "ALACENA_ING",
                        "Mis ingredientes: ${todosLosIngredientes.size}"
                    )

                    Log.d(
                        "ALACENA_ING",
                        "Categorías: ${ingredientesPorCategoria.size}"
                    )



                    Log.d(
                        "ALACENA_ING",
                        "Consume primero: ${consumePrimero.size}"
                    )

                } else {

                    Log.e(
                        "ALACENA_ING",
                        "La API respondió success=false: ${respuesta.message}"
                    )
                }

            } catch (e: Exception) {

                Log.e(
                    "ALACENA_ING",
                    "Error al cargar ingredientes",
                    e
                )
            }
        }
    }
    private fun calcularFechaCaducidadCarne(
        fechaCompra: String,
        estado: String,
        almacenamiento: String
    ): String? {

        try {

            val formato =
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

            formato.isLenient = false

            val fecha = formato.parse(fechaCompra) ?: return null

            /*
             * Días que tarda en llegar a DESCOMPUESTO
             * dependiendo del estado actual.
             */
            val diasEstado = when (estado) {

                "Fresco" -> 9

                "Maduro" -> 9 - 5

                "Pasado" -> 9 - 9

                "Descompuesto" -> 0

                "Sellado" -> return null

                else -> return null
            }

            val calendario = Calendar.getInstance()
            calendario.time = fecha

            calendario.add(
                Calendar.DAY_OF_MONTH,
                diasEstado
            )

            /*
             * Modificador según almacenamiento
             */
            val modificadorAlmacenamiento = when (almacenamiento) {

                "Refrigerador" -> 2

                "Ambiente" -> -1

                "Congelador" -> 3

                else -> 0
            }

            calendario.add(
                Calendar.DAY_OF_MONTH,
                modificadorAlmacenamiento
            )

            return formato.format(calendario.time)

        } catch (e: Exception) {

            Log.e(
                "CADUCIDAD",
                "Error calculando fecha de caducidad",
                e
            )

            return null
        }
    }

    private fun calcularFechaCaducidadFrutasVerduras(
        fechaCompra: String,
        estado: String,
        almacenamiento: String
    ): String? {

        try {

            val formato =
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

            formato.isLenient = false

            val fecha =
                formato.parse(fechaCompra) ?: return null

            /*
             * Días que ya tiene el ingrediente
             * dependiendo del estado actual.
             *
             * Verde       = día 0
             * Fresco      = día 3
             * Maduro      = día 6
             * Pasado      = día 9
             * Descompuesto = día 10
             */
            val diasEstado = when (estado) {

                "Verde" -> 10

                "Fresco" -> 10 - 3

                "Maduro" -> 10 - 6

                "Pasado" -> 10 - 9

                "Descompuesto" -> 0

                "Sellado" -> return null

                else -> return null
            }

            val calendario = Calendar.getInstance()
            calendario.time = fecha

            calendario.add(
                Calendar.DAY_OF_MONTH,
                diasEstado
            )

            /*
             * Modificador según almacenamiento
             */
            val modificadorAlmacenamiento = when (almacenamiento) {

                "Refrigerador" -> 1

                "Ambiente" -> -1

                "Congelador" -> 5

                else -> 0
            }

            calendario.add(
                Calendar.DAY_OF_MONTH,
                modificadorAlmacenamiento
            )

            return formato.format(calendario.time)

        } catch (e: Exception) {

            Log.e(
                "CADUCIDAD",
                "Error calculando fecha de caducidad de frutas y verduras",
                e
            )

            return null
        }
    }
    private fun calcularFechaCaducidadLacteos(
        fechaCompra: String,
        estado: String,
        almacenamiento: String
    ): String? {

        try {

            val formato =
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

            formato.isLenient = false

            val fecha =
                formato.parse(fechaCompra) ?: return null

            /*
             * Días que ya tiene el ingrediente
             * dependiendo del estado actual.
             *
             * Fresco        = día 0
             * Maduro        = día 3
             * Pasado        = día 4
             * Descompuesto  = día 5
             *
             * La función calcula cuántos días faltan
             * para llegar al día 5 (descompuesto).
             */
            val diasEstado = when (estado) {

                "Fresco" -> 5

                "Maduro" -> 5 - 3

                "Pasado" -> 5 - 4

                "Descompuesto" -> 0

                "Sellado" -> return null

                else -> return null
            }

            val calendario = Calendar.getInstance()

            calendario.time = fecha

            calendario.add(
                Calendar.DAY_OF_MONTH,
                diasEstado
            )

            /*
             * Modificador según almacenamiento
             */
            val modificadorAlmacenamiento = when (almacenamiento) {

                "Refrigerador" -> 2

                "Ambiente" -> -1

                "Congelador" -> 3

                else -> 0
            }

            calendario.add(
                Calendar.DAY_OF_MONTH,
                modificadorAlmacenamiento
            )

            return formato.format(calendario.time)

        } catch (e: Exception) {

            Log.e(
                "CADUCIDAD",
                "Error calculando fecha de caducidad de lácteos",
                e
            )

            return null
        }
    }
    private fun calcularFechaCaducidadNoPerecedero(
        fechaCompra: String,
        fechaCaducidadActual: String
    ): String? {

        try {

            val formato =
                SimpleDateFormat(
                    "dd/MM/yyyy",
                    Locale.getDefault()
                )

            formato.isLenient = false


            // ==============================================
            // FECHA DE CADUCIDAD POR DEFECTO
            // ==============================================

            val fechaCaducidadPorDefecto = "09/10/2026"


            // ==============================================
            // VERIFICAR SI EL USUARIO CAMBIÓ LA FECHA
            // ==============================================

            if (
                fechaCaducidadActual.isNotEmpty() &&
                fechaCaducidadActual != fechaCaducidadPorDefecto
            ) {

                // El usuario cambió la fecha.
                // Se conserva exactamente la fecha introducida.

                return fechaCaducidadActual
            }


            // ==============================================
            // EL USUARIO NO CAMBIÓ LA FECHA
            //
            // Se calcula:
            //
            // FECHA COMPRA + 2 AÑOS
            // ==============================================

            val fecha =
                formato.parse(fechaCompra)
                    ?: return null


            val calendario =
                Calendar.getInstance()

            calendario.time = fecha


            calendario.add(
                Calendar.YEAR,
                2
            )


            // ==============================================
            // DEVOLVER FECHA CALCULADA
            // ==============================================

            return formato.format(
                calendario.time
            )

        } catch (e: Exception) {

            Log.e(
                "CADUCIDAD",
                "Error calculando fecha de caducidad de no perecedero",
                e
            )

            return null
        }
    }
    private fun calcularFechaCaducidadAbarrotes(
        fechaCompra: String,
        estado: String
    ): String? {

        try {

            val formato =
                SimpleDateFormat(
                    "dd/MM/yyyy",
                    Locale.getDefault()
                )

            formato.isLenient = false

            val fecha =
                formato.parse(fechaCompra)
                    ?: return null

            val calendario =
                Calendar.getInstance()

            calendario.time = fecha

            when (estado) {

                "Sellado" -> {

                    calendario.add(
                        Calendar.YEAR,
                        1
                    )
                }

                "Pasado" -> {

                    calendario.add(
                        Calendar.YEAR,
                        1
                    )
                }

                "Descompuesto" -> {

                    return formato.format(
                        calendario.time
                    )
                }

                else -> {
                    return null
                }
            }

            return formato.format(
                calendario.time
            )

        } catch (e: Exception) {

            Log.e(
                "CADUCIDAD",
                "Error calculando caducidad de abarrotes",
                e
            )

            return null
        }
    }

    private fun calcularFechaCaducidadVino(
        fechaCompra: String,
        estado: String,
        almacenamiento: String
    ): String? {

        try {

            val formato =
                SimpleDateFormat(
                    "dd/MM/yyyy",
                    Locale.getDefault()
                )

            formato.isLenient = false

            val fecha =
                formato.parse(fechaCompra)
                    ?: return null


            // ==================================================
            // DÍAS HASTA DESCOMPUESTO
            // ==================================================

            val diasEstado = when (estado) {

                "Fresco" -> 13

                "Maduro" -> 13 - 2

                "Pasado" -> 13 - 8

                "Descompuesto" -> 0

                "Sellado" -> return null

                else -> return null
            }


            val calendario =
                Calendar.getInstance()

            calendario.time =
                fecha


            calendario.add(
                Calendar.DAY_OF_MONTH,
                diasEstado
            )


            // ==================================================
            // ALMACENAMIENTO
            // ==================================================

            val modificadorAlmacenamiento =
                when (almacenamiento) {

                    "Refrigerador" -> 1

                    "Ambiente" -> 0

                    else -> 0
                }


            calendario.add(
                Calendar.DAY_OF_MONTH,
                modificadorAlmacenamiento
            )


            return formato.format(
                calendario.time
            )

        } catch (e: Exception) {

            Log.e(
                "CADUCIDAD",
                "Error calculando caducidad de vino",
                e
            )

            return null
        }
    }
    private fun calcularFechaCaducidadEmbutidos(
        fechaCompra: String,
        estado: String,
        almacenamiento: String
    ): String? {
        try {
            val formato =
                SimpleDateFormat(
                    "dd/MM/yyyy",
                    Locale.getDefault()
                )
            formato.isLenient = false

            val fecha =
                formato.parse(fechaCompra)
                    ?: return null

            val diasEstado = when (estado) {
                "Verde" -> 7
                "Maduro" -> 7 - 5
                "Pasado" -> 7 - 6
                "Descompuesto" -> 0
                "Sellado" -> return null
                else -> return null
            }

            val calendario =
                Calendar.getInstance()

            calendario.time = fecha

            calendario.add(
                Calendar.DAY_OF_MONTH,
                diasEstado
            )

            val modificadorAlmacenamiento =
                when (almacenamiento) {
                    "Refrigerador" -> 2
                    "Ambiente" -> -1
                    "Congelador" -> 3
                    else -> 0
                }

            calendario.add(
                Calendar.DAY_OF_MONTH,
                modificadorAlmacenamiento
            )

            return formato.format(calendario.time)

        } catch (e: Exception) {
            Log.e(
                "CADUCIDAD",
                "Error calculando caducidad de embutidos",
                e
            )
            return null
        }
    }

    private fun actualizarFechaCaducidad(
        txtFechaCompra: TextView,
        txtTipoEstado: TextView,
        txtTipoAlmacenamiento: TextView,
        txtFechaConsumo: TextView,
        edtIngrediente : TextView? = null

    ) {
        val nombreIngrediente =
            edtIngrediente
                ?.text
                ?.toString()
                ?.trim()
                ?.lowercase()
                .orEmpty()

        val fechaCompra =
            txtFechaCompra.text
                .toString()
                .trim()

        val estado =
            txtTipoEstado.text
                .toString()
                .trim()

        val almacenamiento =
            txtTipoAlmacenamiento.text
                .toString()
                .trim()


        // ==========================================
        // VALIDAR FECHA DE COMPRA
        // ==========================================

        if (fechaCompra.isEmpty()) {
            return
        }


        // ==========================================
        // VALIDAR ESTADO
        // ==========================================

        if (estado.isEmpty()) {
            return
        }


        // ==========================================
        // SELLADO
        // ==========================================

        // Sellado utiliza la fecha que proporciona
        // el usuario. No hacemos cálculo automático.
        if (estado == "Sellado") {
            return
        }


        // ==========================================
        // CATEGORÍA CARNE
        // ==========================================
        // ==========================================
// CALCULAR SEGÚN LA FAMILIA
// ==========================================

        val familia =
            familiaIngredienteSeleccionado
                ?.trim()
                ?.lowercase()

        val fechaCaducidad: String?

        when (familia) {
            "cereales leguminosas",
            "especias",
            "pastas",
            "semillas",
            "industrializados",
            "hierbas aromatica"-> {

                fechaCaducidad =
                    calcularFechaCaducidadNoPerecedero(
                        fechaCompra = fechaCompra,
                        fechaCaducidadActual = txtFechaConsumo.text
                            .toString()
                            .trim()
                    )
            }
            "abarrotes"->{

                fechaCaducidad =
                    calcularFechaCaducidadAbarrotes(
                        fechaCompra = fechaCompra,
                        estado = estado
                    )
            }
            "licores y destilados" -> {
                if (nombreIngrediente == "vino") {

                    fechaCaducidad =
                        calcularFechaCaducidadVino(
                            fechaCompra = fechaCompra,
                            estado = estado,
                            almacenamiento = almacenamiento
                        )

                } else {

                    // Los demás licores son Sellados
                    // y no tienen cálculo automático.
                    return
                }
            }
            "embutidos" -> {
                fechaCaducidad =
                    calcularFechaCaducidadEmbutidos(
                        fechaCompra = fechaCompra,
                        estado = estado,
                        almacenamiento = almacenamiento
                    )
            }


            "lacteos" -> {

                fechaCaducidad =
                    calcularFechaCaducidadLacteos(
                        fechaCompra = fechaCompra,
                        estado = estado,
                        almacenamiento = almacenamiento
                    )
            }

            "carne" -> {

                fechaCaducidad =
                    calcularFechaCaducidadCarne(
                        fechaCompra = fechaCompra,
                        estado = estado,
                        almacenamiento = almacenamiento
                    )
            }

            "fruta",
            "frutas",
            "verdura",
            "verduras" -> {

                fechaCaducidad =
                    calcularFechaCaducidadFrutasVerduras(
                        fechaCompra = fechaCompra,
                        estado = estado,
                        almacenamiento = almacenamiento
                    )
            }

            else -> {

                Log.d(
                    "CADUCIDAD",
                    "No existen reglas para la familia: $familia"
                )

                return
            }
        }

        // ==========================================
        // MOSTRAR RESULTADO
        // ==========================================

        if (fechaCaducidad != null) {

            txtFechaConsumo.text =
                fechaCaducidad
        }
    }
    private fun actualizarEstadosYCargarAlacena(
        alcId: Int,clienteId: Int
    ) {

        actualizarEstadosAlacena(alcId) {

            cargarIngredientesAlacena(alcId,clienteId)
        }
    }
    private fun actualizarEstadosAlacena(
        alcId: Int,
        onComplete: (() -> Unit)? = null
    ) {

        lifecycleScope.launch {

            try {

                Log.d(
                    "ESTADOS_ALACENA",
                    "Actualizando estados de alacena: $alcId"
                )

                val respuesta =
                    ApiClient.apiService
                        .actualizarEstadosAlacena(alcId)

                if (respuesta.success) {

                    Log.d(
                        "ESTADOS_ALACENA",
                        "Estados actualizados correctamente"
                    )

                    Log.d(
                        "ESTADOS_ALACENA",
                        "Procesados: ${respuesta.procesados}"
                    )

                    Log.d(
                        "ESTADOS_ALACENA",
                        "Actualizados: ${respuesta.actualizados}"
                    )

                    Log.d(
                        "ESTADOS_ALACENA",
                        "Sin cambios: ${respuesta.sin_cambios}"
                    )

                    Log.d(
                        "ESTADOS_ALACENA",
                        "No aplican: ${respuesta.no_aplican}"
                    )

                    onComplete?.invoke()

                } else {

                    Log.e(
                        "ESTADOS_ALACENA",
                        "Error: ${respuesta.message}"
                    )

                    // Aunque la API haya respondido con error,
                    // intentamos cargar la alacena para no dejar
                    // la pantalla vacía.
                    onComplete?.invoke()
                }

            } catch (e: Exception) {

                Log.e(
                    "ESTADOS_ALACENA",
                    "Error conectando con API",
                    e
                )

                // Si falla la conexión también cargamos
                // los datos existentes.
                onComplete?.invoke()
            }
        }
    }
    private fun formatearFechaDialogo(
        fecha: String?
    ): String {

        if (fecha.isNullOrBlank()) {
            return ""
        }

        return try {

            val formatoEntrada =
                SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.getDefault()
                )

            val formatoSalida =
                SimpleDateFormat(
                    "dd/MM/yyyy",
                    Locale.getDefault()
                )

            val fechaConvertida =
                formatoEntrada.parse(fecha)

            if (fechaConvertida != null) {
                formatoSalida.format(fechaConvertida)
            } else {
                ""
            }

        } catch (e: Exception) {

            Log.e(
                "ALACENA",
                "Error convirtiendo fecha: $fecha",
                e
            )

            ""
        }
    }
    private fun eliminarIngredienteDeAlacena(
        alcId: Int,
        ingId: Int,
        bottomSheet: BottomSheetDialog
    ) {

        lifecycleScope.launch {

            try {

                Log.d(
                    "ALACENA_ELIMINAR",
                    """
                ===== ELIMINAR INGREDIENTE =====
                ALC_ID: $alcId
                ING_ID: $ingId
                ================================
                """.trimIndent()
                )


                val request =
                    EliminarIngredienteAlacenaRequest(
                        ALC_ID = alcId,
                        ING_ID = ingId
                    )


                val response =
                    ApiClient.apiService
                        .eliminarIngredienteAlacena(
                            request
                        )


                if (response.success) {

                    Log.d(
                        "ALACENA_ELIMINAR",
                        "ÉXITO: ${response.message}"
                    )


                    makeText(
                        this@Menu_principal_free,
                        response.message,
                        Toast.LENGTH_SHORT
                    ).show()


                    bottomSheet.dismiss()


                    // ======================================
                    // RECARGAR ALACENA
                    // ======================================

                    val clienteId =
                        alacenaSeleccionada?.ALC_CLI_ID

                    if (clienteId != null) {

                        actualizarEstadosYCargarAlacena(
                            alcId,
                            clienteId
                        )
                    }


                } else {

                    Log.e(
                        "ALACENA_ELIMINAR",
                        "ERROR: ${response.message}"
                    )

                    makeText(
                        this@Menu_principal_free,
                        response.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }


            } catch (e: Exception) {

                Log.e(
                    "ALACENA_ELIMINAR",
                    "Error al eliminar ingrediente",
                    e
                )

                makeText(
                    this@Menu_principal_free,
                    "Error de conexión con el servidor.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    private fun buscarRecetasDelIngrediente(
        ingredienteId: Int,
        nombreIngrediente: String
    ) {

        lifecycleScope.launch {

            try {

                val request =
                    BuscarRecetasPorIngredientesRequest(
                        ingredientes =
                            listOf(ingredienteId)
                    )

                val response =
                    ApiClient.apiService
                        .buscarRecetasPorIngredientes(
                            request
                        )

                if (response.success) {

                    mostrarVentanaRecetas(
                        nombreIngrediente,
                        response.recetas
                    )

                } else {

                    Toast.makeText(
                        this@Menu_principal_free,
                        "No se encontraron recetas.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {

                Log.e(
                    "RECETAS_INGREDIENTE",
                    "Error buscando recetas",
                    e
                )

                Toast.makeText(
                    this@Menu_principal_free,
                    "Error de conexión.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    private fun mostrarVentanaRecetas(
        nombreIngrediente: String,
        recetas: List<RecetaconFiltro>
    ) {

        val dialog = BottomSheetDialog(this)

        val view = layoutInflater.inflate(
            R.layout.dialog_recetas_ingrediente,
            null
        )

        val txtTituloRecetasIngrediente =
            view.findViewById<TextView>(
                R.id.txtTituloRecetasIngrediente
            )

        val rvRecetasIngrediente =
            view.findViewById<RecyclerView>(
                R.id.rvRecetasIngrediente
            )

        // -----------------------------------
        // TÍTULO
        // -----------------------------------

        txtTituloRecetasIngrediente.text =
            "RECETAS CON: ${nombreIngrediente.uppercase()}"

        // -----------------------------------
        // CONFIGURAR RECYCLERVIEW
        // -----------------------------------

        rvRecetasIngrediente.layoutManager =
            LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL,
                false
            )

        // -----------------------------------
        // ADAPTER
        // -----------------------------------

        val adapter = RecetasPorIngredienteAdapter(
            recetas
        ) { receta ->

            abrirDetalleReceta2(receta.REC_ID)
        }

        rvRecetasIngrediente.adapter = adapter

        // -----------------------------------
        // MOSTRAR
        // -----------------------------------

        dialog.setContentView(view)

        dialog.show()
    }
    private fun abrirDetalleReceta2(recetaId: Int) {

        if (recetaId <= 0) {

            Toast.makeText(
                this,
                "Receta no válida.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val intent =
            Intent(
                this,
                DetalleRecetaActivity::class.java
            )

        intent.putExtra(
            "REC_ID",
            recetaId
        )

        startActivity(intent)
    }
    private fun eliminarIngredienteUsadoEnReceta(
        ingrediente: IngredienteAlacena,
        bottomSheet: BottomSheetDialog
    ) {

        lifecycleScope.launch {

            try {

                // ==========================================
                // CREAR REQUEST
                // ==========================================

                val request =
                    EliminarIngredienteAlacenaRequest(
                        ALC_ID = ingrediente.ALC_ID,
                        ING_ID = ingrediente.ING_ID
                    )


                // ==========================================
                // LOG
                // ==========================================

                Log.d(
                    "ALACENA_API",
                    """
                ===== ELIMINAR USADO EN RECETA =====
                ALC_ID: ${request.ALC_ID}
                ING_ID: ${request.ING_ID}
                ====================================
                """.trimIndent()
                )


                // ==========================================
                // LLAMAR API
                // ==========================================

                val response =
                    ApiClient.apiService
                        .eliminarIngredienteAlacena(
                            request
                        )


                // ==========================================
                // RESPUESTA
                // ==========================================

                if (response.success) {

                    Log.d(
                        "ALACENA_API",
                        "Ingrediente eliminado: ${response.message}"
                    )


                    // ======================================
                    // MOSTRAR MENSAJE
                    // ======================================

                    Toast.makeText(
                        this@Menu_principal_free,
                        response.message,
                        Toast.LENGTH_SHORT
                    ).show()


                    // ======================================
                    // CERRAR BOTTOM SHEET
                    // ======================================

                    bottomSheet.dismiss()


                    // ======================================
                    // RECARGAR ALACENA
                    // ======================================

                    alacenaSeleccionada?.let {

                        actualizarEstadosYCargarAlacena(
                            it.ALC_ID,
                            it.ALC_CLI_ID
                        )
                    }

                } else {

                    Log.e(
                        "ALACENA_API",
                        "Error al eliminar: ${response.message}"
                    )

                    Toast.makeText(
                        this@Menu_principal_free,
                        response.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {

                Log.e(
                    "ALACENA_API",
                    "Error al eliminar ingrediente usado en receta",
                    e
                )

                Toast.makeText(
                    this@Menu_principal_free,
                    "Error de conexión con el servidor.",
                    Toast.LENGTH_SHORT
                ).show()
            }
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