package mx.com.gourmeet.app
import mx.com.gourmeet.app.ui.applySafeDrawingInsets

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ListView
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import android.widget.Toast.makeText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import mx.com.gourmeet.app.AlacenaActivity.ModoIngrediente
import mx.com.gourmeet.app.data.api.ApiClient
import mx.com.gourmeet.app.data.models.Alacena
import mx.com.gourmeet.app.data.models.BuscarIngredientes
import mx.com.gourmeet.app.data.models.BuscarRecetasPorIngredientesRequest
import mx.com.gourmeet.app.data.models.CrearAlacenaRequest
import mx.com.gourmeet.app.data.models.EditarAlacenaRequest
import mx.com.gourmeet.app.data.models.EliminarAlacenaRequest
import mx.com.gourmeet.app.data.models.EliminarIngredienteAlacenaRequest
import mx.com.gourmeet.app.data.models.GuardarIngredienteAlacenaRequest
import mx.com.gourmeet.app.data.models.IngredienteAlacena
import mx.com.gourmeet.app.data.models.ListarAlacenasRequest
import mx.com.gourmeet.app.data.models.ListarIngredientesAlacenaRequest
import mx.com.gourmeet.app.data.models.RecetaconFiltro
import mx.com.gourmeet.app.databinding.ActivityMiAlacenaBinding
import mx.com.gourmeet.app.ui.adapters.IngredienteMiniAdapter
import mx.com.gourmeet.app.utils.SesionUsuario
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.collections.component1
import kotlin.collections.component2

class MiAlacenaActivity : AppCompatActivity() {
    private enum class ModoIngrediente {
        AGREGAR,
        EDITAR,
        USAR_EN_RECETA
    }
    private lateinit var adapterConsumePrimero: IngredienteAlacenaAdapter
    private lateinit var adapterCategoriasIngredientes: CategoriaIngredientesAdapter
    private var ingredienteSeleccionadoId: Int? = null
    private lateinit var binding: ActivityMiAlacenaBinding
    private var alacenaSeleccionada: Alacena? = null
    private val listaAlacenas = mutableListOf<Alacena>()
    var familiaIngredienteSeleccionado: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMiAlacenaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySafeDrawingInsets()

        // ==========================================
        // CONFIGURAR LISTAS
        // ==========================================
        configurarRecyclerViews()

        // ==========================================
        // BOTÓN REGRESAR
        // ==========================================
        binding.btnRegresarAlacena.setOnClickListener {
            finish()
        }

        // ==========================================
        // AGREGAR INGREDIENTE
        // ==========================================
        binding.btnAgregarIngrediente.setOnClickListener {
            if (alacenaSeleccionada == null) {
                Toast.makeText(
                    this,
                    "Primero selecciona una alacena.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                mostrarDialogAgregarIngrediente()
            }
        }

        // ==========================================
        // CONSULTAR ALACENAS DEL USUARIO
        // ==========================================
        cargarAlacenas()
    }

    private fun configurarRecyclerViews() {

        // ==========================================
        // 1. INGREDIENTES QUE DEBEMOS CONSUMIR PRIMERO
        // ==========================================
        binding.rvConsumePrimero.layoutManager =
            LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL,
                false
            )

        adapterConsumePrimero = IngredienteAlacenaAdapter(
            emptyList()
        ) { ingrediente ->
            mostrarDialogAgregarIngrediente(ingrediente)
        }

        binding.rvConsumePrimero.adapter = adapterConsumePrimero


        // ==========================================
        // 2. TODOS LOS INGREDIENTES POR CATEGORÍA
        // ==========================================
        binding.rvMisIngredientes.layoutManager =
            LinearLayoutManager(this)

        adapterCategoriasIngredientes = CategoriaIngredientesAdapter(
            emptyList()
        ) { ingrediente ->
            mostrarDialogAgregarIngrediente(ingrediente)
        }

        binding.rvMisIngredientes.adapter = adapterCategoriasIngredientes
    }


    private fun cargarAlacenas() {

        val clienteId = SesionUsuario.obtenerId(this)

        if (clienteId <= 0) {
            Toast.makeText(
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

                val respuesta = ApiClient.apiService.listarAlacenas(request)

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
                        // SELECCIONAR LA PRIMERA ALACENA
                        // =================================

                        val alacenaInicial = listaAlacenas.first()

                        alacenaSeleccionada = alacenaInicial

                        Log.d(
                            "ALACENA",
                            """
                        Primera alacena seleccionada:
                        ALC_ID: ${alacenaInicial.ALC_ID}
                        ALC_NOMBRE: ${alacenaInicial.ALC_NOMBRE}
                        ALC_CLI_ID: ${alacenaInicial.ALC_CLI_ID}
                        HOG_ID: ${alacenaInicial.HOG_ID}
                        """.trimIndent()
                        )

                        // =================================
                        // ACTUALIZAR ESTADOS Y CARGAR
                        // INGREDIENTES
                        // =================================

                        actualizarEstadosYCargarAlacena(
                            alcId = alacenaInicial.ALC_ID,
                            clienteId = clienteId,
                            hogId = alacenaInicial.HOG_ID,
                            alcCliId = alacenaInicial.ALC_CLI_ID
                        )
                    }

                } else {

                    Toast.makeText(
                        this@MiAlacenaActivity,
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

                Toast.makeText(
                    this@MiAlacenaActivity,
                    "Error al cargar las alacenas.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    private fun mostrarSinAlacenas() {

        // ==========================================
        // OCULTAR CONTENIDO DE ALACENA
        // ==========================================

        binding.tilAlacena.visibility = View.GONE
        binding.txtConsumePrimero.visibility = View.GONE
        binding.rvConsumePrimero.visibility = View.GONE
        binding.txtMisIngredientes.visibility = View.GONE
        binding.rvMisIngredientes.visibility = View.GONE
        binding.btnAgregarIngrediente.visibility = View.GONE

        // ==========================================
        // MOSTRAR PANEL SIN ALACENAS
        // ==========================================

        binding.panelSinAlacenas.visibility = View.VISIBLE

        // ==========================================
        // BOTÓN CREAR PRIMERA ALACENA
        // ==========================================

        binding.btnCrearPrimeraAlacena.setOnClickListener {
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
                        this@MiAlacenaActivity,
                        respuesta.message ?: "Alacena creada correctamente",
                        Toast.LENGTH_SHORT
                    ).show()

                    dialog.dismiss()

                    // Volvemos a cargar las alacenas
                    cargarAlacenas()

                } else {

                    makeText(
                        this@MiAlacenaActivity,
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
                    this@MiAlacenaActivity,
                    "Error de conexión con el servidor.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    private fun mostrarAlacenas() {

        // ==========================================
        // OCULTAR PANEL SIN ALACENAS
        // ==========================================
        binding.panelSinAlacenas.visibility = View.GONE

        // ==========================================
        // MOSTRAR CONTENIDO DE LA ALACENA
        // ==========================================
        binding.tilAlacena.visibility = View.VISIBLE

        binding.txtConsumePrimero.visibility = View.VISIBLE
        binding.rvConsumePrimero.visibility = View.VISIBLE

        binding.txtMisIngredientes.visibility = View.VISIBLE
        binding.rvMisIngredientes.visibility = View.VISIBLE

        binding.btnAgregarIngrediente.visibility = View.VISIBLE

        // ==========================================
        // CONFIGURAR BOTÓN REGRESAR
        // ==========================================
        binding.btnRegresarAlacena.visibility = View.VISIBLE

        // ==========================================
        // CONFIGURAR SELECTOR DE ALACENAS
        // ==========================================
        configurarSelectorAlacenas()
    }
    private fun configurarSelectorAlacenas() {

        // ==========================================
        // OBTENER BOTÓN DEL SELECTOR
        // ==========================================
        val btnAlacena = binding.actAlacena

        // ==========================================
        // VERIFICAR QUE EXISTAN ALACENAS
        // ==========================================
        if (listaAlacenas.isEmpty()) {
            btnAlacena.text = "Seleccionar alacena"
            return
        }

        // ==========================================
        // SELECCIONAR LA PRIMERA ALACENA
        // SI TODAVÍA NO HAY UNA SELECCIONADA
        // ==========================================
        if (alacenaSeleccionada == null) {
            alacenaSeleccionada = listaAlacenas[0]
        }

        // ==========================================
        // MOSTRAR ALACENA ACTUAL
        // ==========================================
        btnAlacena.text = alacenaSeleccionada?.ALC_NOMBRE
            ?: "Seleccionar alacena"

        // ==========================================
        // REGISTRAR ALACENA SELECCIONADA
        // ==========================================
        Log.d(
            "ALACENA",
            "Alacena seleccionada: ${alacenaSeleccionada?.ALC_ID}"
        )

        // ==========================================
        // ABRIR MENÚ DE ALACENAS
        // ==========================================
        btnAlacena.setOnClickListener {
            mostrarMenuMisAlacenas()
        }
    }
    private fun mostrarMenuMisAlacenas() {

        // ==========================================
        // CREAR BOTTOM SHEET
        // ==========================================
        val bottomSheet = BottomSheetDialog(this)

        val view = layoutInflater.inflate(
            R.layout.dialog_mis_alacenas,
            null
        )

        bottomSheet.setContentView(view)

        // ==========================================
        // CONFIGURAR RECYCLERVIEW
        // ==========================================
        val rvMisAlacenas =
            view.findViewById<RecyclerView>(R.id.rvMisAlacenas)

        rvMisAlacenas.layoutManager = LinearLayoutManager(this)

        // ==========================================
        // CONFIGURAR ADAPTER
        // ==========================================
        val adapter = AlacenaMenuAdapter(

            lista = listaAlacenas,

            // ======================================
            // SELECCIONAR ALACENA
            // ======================================
            onSeleccionar = { alacena ->

                val alacenaAnterior = alacenaSeleccionada

                // Evitar recargar si selecciona la misma alacena
                if (alacenaAnterior?.ALC_ID == alacena.ALC_ID) {

                    bottomSheet.dismiss()

                } else {

                    // 1. Actualizar alacena seleccionada
                    alacenaSeleccionada = alacena

                    // 2. Actualizar nombre del botón
                    binding.actAlacena.text = alacena.ALC_NOMBRE

                    // 3. Registrar el cambio
                    Log.d(
                        "ALACENA",
                        """
                    Cambio de alacena:
                    Anterior: ${alacenaAnterior?.ALC_ID}
                    Nueva: ${alacena.ALC_ID}
                    Nombre: ${alacena.ALC_NOMBRE}
                    Hogar: ${alacena.HOG_ID}
                    """.trimIndent()
                    )

                    // 4. Cerrar el menú
                    bottomSheet.dismiss()

                    // 5. Actualizar estados y cargar ingredientes
                    actualizarEstadosYCargarAlacena(
                        alcId = alacena.ALC_ID,
                        clienteId = alacena.ALC_CLI_ID ?: 0,
                        hogId = alacena.HOG_ID,
                        alcCliId = alacena.ALC_CLI_ID
                    )
                }
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
        bottomSheet.setOnShowListener { dialogInterface ->

            val dialog = dialogInterface as BottomSheetDialog

            val sheet = dialog.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet
            )

            sheet?.let { bottomSheetView ->

                val behavior = BottomSheetBehavior.from(bottomSheetView)

                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true

                bottomSheetView.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.bg_bottom_sheet_alacena
                )
            }
        }

        // ==========================================
        // MOSTRAR BOTTOM SHEET
        // ==========================================
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
                        this@MiAlacenaActivity,
                        respuesta.message
                            ?: "Alacena actualizada correctamente.",
                        Toast.LENGTH_SHORT
                    ).show()

                    dialog.dismiss()

                    cargarAlacenas()

                } else {

                    makeText(
                        this@MiAlacenaActivity,
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
                    this@MiAlacenaActivity,
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
                        this@MiAlacenaActivity,
                        respuesta.message
                            ?: "Alacena eliminada correctamente.",
                        Toast.LENGTH_SHORT
                    ).show()

                    cargarAlacenas()

                } else {

                    makeText(
                        this@MiAlacenaActivity,
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
                    this@MiAlacenaActivity,
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

    private fun mostrarDialogAgregarIngrediente(ingredienteEditar: IngredienteAlacena? = null) {
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
// Referencias a las fechas del XML
        val txtFechaCompra =
            view.findViewById<TextView>(R.id.txtFechadecompra)

        val txtFechaConsumo =
            view.findViewById<TextView>(R.id.txtFechadecon)

// Inicializar las fechas al abrir la ventana
        val calendario = Calendar.getInstance()
        val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

// Fecha de compra: fecha actual
        txtFechaCompra.text = formatoFecha.format(calendario.time)

// Fecha de consumo: 10 días después de hoy
        calendario.add(Calendar.DAY_OF_YEAR, 10)
        txtFechaConsumo.text = formatoFecha.format(calendario.time)

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


            edtIngrediente.text?.clear()

            txtCantidad.setText("500")

            actUnidadCantidad.setText(
                "g",
                false
            )

            txtPrecioCompra.text?.clear()
            val calendario = Calendar.getInstance()

// Formato de fecha: dd/MM/yyyy
            val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

// Fecha de compra: hoy
            txtFechaCompra.text = formatoFecha.format(calendario.time)

// Fecha de consumo: 10 días después de hoy
            calendario.add(Calendar.DAY_OF_YEAR, 10)
            txtFechaConsumo.text = formatoFecha.format(calendario.time)


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
            val request = GuardarIngredienteAlacenaRequest(

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
                        .ifEmpty { null },

                // IDENTIFICADORES DE LA ALACENA
                HOG_ID =
                    alacenaSeleccionada!!.HOG_ID,

                ALC_CLI_ID =
                    alacenaSeleccionada!!.ALC_CLI_ID
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
                            this@MiAlacenaActivity,
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


                        alacenaSeleccionada?.let { alacena ->

                            val clienteId = SesionUsuario.obtenerId(
                                this@MiAlacenaActivity
                            )

                            Log.d(
                                "ALACENA_API",
                                """
        Recargando alacena después de guardar:
        ALC_ID: ${alacena.ALC_ID}
        ALC_CLI_ID: ${alacena.ALC_CLI_ID}
        HOG_ID: ${alacena.HOG_ID}
        CLI_ID: $clienteId
        """.trimIndent()
                            )

                            actualizarEstadosYCargarAlacena(
                                alcId = alacena.ALC_ID,
                                clienteId = clienteId,
                                hogId = alacena.HOG_ID,
                                alcCliId = alacena.ALC_CLI_ID
                            )

                        } ?: run {

                            Log.e(
                                "ALACENA_API",
                                "No se encontró una alacena seleccionada para recargar."
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
                            this@MiAlacenaActivity,
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
                        this@MiAlacenaActivity,
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
        val imm = getSystemService(INPUT_METHOD_SERVICE)
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

                val alacenaAnterior = alacenaSeleccionada

                // Evitar recargar si selecciona la misma alacena.
                if (alacenaAnterior?.ALC_ID == alacena.ALC_ID) {
                    bottomSheet.dismiss()
                    return@AlacenaMenuAdapter
                }

                // Guardar la nueva alacena seleccionada.
                alacenaSeleccionada = alacena

                // Actualizar el nombre visible.
                txtAlacena.text = alacena.ALC_NOMBRE

                Log.d(
                    "ALACENA",
                    """
        ===== CAMBIO DE ALACENA =====
        ALC_ID: ${alacena.ALC_ID}
        ALC_CLI_ID: ${alacena.ALC_CLI_ID}
        HOG_ID: ${alacena.HOG_ID}
        NOMBRE: ${alacena.ALC_NOMBRE}
        =============================
        """.trimIndent()
                )

                bottomSheet.dismiss()

                // Recargar estados e ingredientes de la nueva alacena.
                // La función debe adaptarse para aceptar alacenas personales
                // y colaborativas.
                actualizarEstadosYCargarAlacena(
                    alcId = alacena.ALC_ID,
                    clienteId = alacena.ALC_CLI_ID ?: 0,
                    hogId = alacena.HOG_ID,
                    alcCliId = alacena.ALC_CLI_ID
                )
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
        clienteId: Int,
        hogId: Int? = null,
        alcCliId: Int? = null
    ) {
        lifecycleScope.launch {
            try {
                Log.d(
                    "ALACENA_ING",
                    """
                Consultando ingredientes:
                ALC_ID=$alcId
                CLI_ID=$clienteId
                HOG_ID=$hogId
                ALC_CLI_ID=$alcCliId
                """.trimIndent()
                )

                // Solicitud compatible con alacena personal y de hogar
                val request = ListarIngredientesAlacenaRequest(
                    ALC_ID = alcId,
                    ALC_CLI_ID = if (hogId != null) {
                        null
                    } else {
                        alcCliId ?: clienteId
                    },
                    HOG_ID = hogId
                )

                Log.d(
                    "ALACENA_ING",
                    "JSON lógico de solicitud: $request"
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

                            estado == "pasado" ||
                                    estado == "descompuesto"
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
        alcId: Int,
        clienteId: Int,
        hogId: Int? = null,
        alcCliId: Int? = null
    ) {
        actualizarEstadosAlacena(alcId) {
            cargarIngredientesAlacena(
                alcId = alcId,
                clienteId = clienteId,
                hogId = hogId,
                alcCliId = alcCliId
            )
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

                // ======================================
                // REGISTRAR SOLICITUD
                // ======================================

                Log.d(
                    "ALACENA_ELIMINAR",
                    """
                ===== ELIMINAR INGREDIENTE =====
                ALC_ID: $alcId
                ING_ID: $ingId
                ================================
                """.trimIndent()
                )

                // ======================================
                // CREAR REQUEST
                // ======================================

                val request = EliminarIngredienteAlacenaRequest(
                    ALC_ID = alcId,
                    ING_ID = ingId
                )

                // ======================================
                // LLAMAR API
                // ======================================

                val response = ApiClient.apiService
                    .eliminarIngredienteAlacena(request)

                // ======================================
                // PROCESAR RESPUESTA
                // ======================================

                if (response.success) {

                    Log.d(
                        "ALACENA_ELIMINAR",
                        "ÉXITO: ${response.message}"
                    )

                    Toast.makeText(
                        this@MiAlacenaActivity,
                        response.message,
                        Toast.LENGTH_SHORT
                    ).show()

                    bottomSheet.dismiss()

                    // ======================================
                    // RECARGAR ALACENA PERSONAL O DEL HOGAR
                    // ======================================

                    val alacena = alacenaSeleccionada

                    if (alacena != null && alacena.ALC_ID == alcId) {

                        val clienteId = SesionUsuario.obtenerId(
                            this@MiAlacenaActivity
                        )

                        Log.d(
                            "ALACENA_ELIMINAR",
                            """
                        Recargando alacena:
                        ALC_ID: ${alacena.ALC_ID}
                        CLI_ID: $clienteId
                        ALC_CLI_ID: ${alacena.ALC_CLI_ID}
                        HOG_ID: ${alacena.HOG_ID}
                        """.trimIndent()
                        )

                        actualizarEstadosYCargarAlacena(
                            alcId = alacena.ALC_ID,
                            clienteId = clienteId,
                            hogId = alacena.HOG_ID,
                            alcCliId = alacena.ALC_CLI_ID
                        )

                    } else {

                        Log.e(
                            "ALACENA_ELIMINAR",
                            "No coincide la alacena seleccionada con ALC_ID=$alcId"
                        )

                        Toast.makeText(
                            this@MiAlacenaActivity,
                            "Ingrediente eliminado, pero no se pudo recargar la alacena.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                } else {

                    Log.e(
                        "ALACENA_ELIMINAR",
                        "ERROR: ${response.message}"
                    )

                    Toast.makeText(
                        this@MiAlacenaActivity,
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

                Toast.makeText(
                    this@MiAlacenaActivity,
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

                    makeText(
                        this@MiAlacenaActivity,
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

                makeText(
                    this@MiAlacenaActivity,
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

            makeText(
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

                val request = EliminarIngredienteAlacenaRequest(
                    ALC_ID = ingrediente.ALC_ID,
                    ING_ID = ingrediente.ING_ID
                )

                // ==========================================
                // LOG DE LA SOLICITUD
                // ==========================================

                Log.d(
                    "ALACENA_API",
                    """
                ===== ELIMINAR INGREDIENTE USADO EN RECETA =====
                ALC_ID: ${request.ALC_ID}
                ING_ID: ${request.ING_ID}
                ===============================================
                """.trimIndent()
                )

                // ==========================================
                // LLAMAR API
                // ==========================================

                val response = ApiClient.apiService
                    .eliminarIngredienteAlacena(request)

                // ==========================================
                // PROCESAR RESPUESTA
                // ==========================================

                if (response.success) {

                    Log.d(
                        "ALACENA_API",
                        "Ingrediente eliminado: ${response.message}"
                    )

                    Toast.makeText(
                        this@MiAlacenaActivity,
                        response.message,
                        Toast.LENGTH_SHORT
                    ).show()

                    // ======================================
                    // CERRAR BOTTOM SHEET
                    // ======================================

                    bottomSheet.dismiss()

                    // ======================================
                    // RECARGAR LA ALACENA SELECCIONADA
                    // PERSONAL O DEL HOGAR
                    // ======================================

                    alacenaSeleccionada?.let { alacena ->

                        Log.d(
                            "ALACENA_API",
                            """
                        Recargando alacena:
                        ALC_ID: ${alacena.ALC_ID}
                        ALC_CLI_ID: ${alacena.ALC_CLI_ID}
                        HOG_ID: ${alacena.HOG_ID}
                        """.trimIndent()
                        )

                        actualizarEstadosYCargarAlacena(
                            alcId = alacena.ALC_ID,
                            clienteId = SesionUsuario.obtenerId(
                                this@MiAlacenaActivity
                            ),
                            hogId = alacena.HOG_ID,
                            alcCliId = alacena.ALC_CLI_ID
                        )

                    } ?: run {

                        Log.e(
                            "ALACENA_API",
                            "No hay una alacena seleccionada para recargar."
                        )
                    }

                } else {

                    Log.e(
                        "ALACENA_API",
                        "Error al eliminar: ${response.message}"
                    )

                    Toast.makeText(
                        this@MiAlacenaActivity,
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
                    this@MiAlacenaActivity,
                    "Error de conexión con el servidor.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}