package com.example.gourmeet2

import android.app.DatePickerDialog
import android.content.Context
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
import com.example.gourmeet2.MiHogarActivity.ModoIngrediente
import com.example.gourmeet2.data.api.ApiClient
import com.example.gourmeet2.data.models.Alacena
import com.example.gourmeet2.data.models.BuscarIngredientes
import com.example.gourmeet2.data.models.BuscarRecetasPorIngredientesRequest
import com.example.gourmeet2.data.models.EliminarIngredienteAlacenaRequest
import com.example.gourmeet2.data.models.GuardarIngredienteAlacenaRequest
import com.example.gourmeet2.data.models.IngredienteAlacena
import com.example.gourmeet2.data.models.ListarIngredientesAlacenaRequest
import com.example.gourmeet2.data.models.ObtenerIngredientesAlacenaRequest
import com.example.gourmeet2.data.models.RecetaconFiltro
import com.example.gourmeet2.databinding.ActivityMiAlacenaBinding
import com.example.gourmeet2.ui.adapters.IngredienteMiniAdapter
import com.example.gourmeet2.utils.SesionUsuario
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AlacenaActivity : AppCompatActivity() {
    private enum class ModoIngrediente {
        AGREGAR,
        EDITAR,
        USAR_EN_RECETA
    }

    private var alacenaSeleccionada: Alacena? = null
    private lateinit var misIngredientes:
            RecyclerView
    private lateinit var adapterIngredientesTemporales:
            IngredienteAlacenaAdapter
    private val ingredientesTemporales = mutableListOf<IngredienteAlacena>()
    private val listaAlacenas = mutableListOf<Alacena>()
    private var familiaIngredienteSeleccionado: String? = null
    private var imagenIngredienteSeleccionada: String? = null
    private var ingredienteSeleccionadoId: Int? = null

    // ==========================================
    // BINDING
    // ==========================================

    private lateinit var binding: ActivityMiAlacenaBinding

    // ==========================================
    // HOGAR
    // ==========================================

    private var hogId: Int = 0

    // ==========================================
    // ALACENA
    // ==========================================

    private var alcId: Int = 0

    // ==========================================
    // ADAPTERS
    // ==========================================

    private lateinit var adapterConsumePrimero: IngredienteAlacenaAdapter

    private lateinit var adapterCategoriasIngredientes:
            CategoriaIngredientesAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding =
            ActivityMiAlacenaBinding.inflate(layoutInflater)

        setContentView(binding.root)

        // ==========================================
        // RECIBIR HOG_ID
        // ==========================================

        hogId =
            intent.getIntExtra(
                "HOG_ID",
                0
            )

        if (hogId <= 0) {

            Toast.makeText(
                this,
                "No se encontró el hogar.",
                Toast.LENGTH_SHORT
            ).show()

            finish()

            return
        }

        Log.d(
            "ALACENA_HOGAR",
            "HOG_ID recibido: $hogId"
        )

        // ==========================================
        // CONFIGURAR RECYCLERVIEWS
        // ==========================================

        configurarRecyclerViews()

        // ==========================================
        // REGRESAR
        // ==========================================

        binding.btnRegresarAlacena.setOnClickListener {

            finish()
        }
        binding.btnAgregarIngrediente.setOnClickListener {
            mostrarDialogAgregarIngrediente()
        }
        binding.actAlacena.visibility = View.GONE

        // ==========================================
        // CARGAR ALACENA DEL HOGAR
        // ==========================================

        cargarAlacenaHogar()
    }


    // ==================================================
    // CONFIGURAR RECYCLERVIEWS
    // ==================================================

    private fun configurarRecyclerViews() {

        // ==========================================
        // CONSUME PRIMERO
        // ==========================================

        binding.rvConsumePrimero.layoutManager =
            LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL,
                false
            )

        adapterConsumePrimero =
            IngredienteAlacenaAdapter(
                emptyList()
            ) { ingrediente ->

                mostrarDetalleIngrediente(
                    ingrediente
                )
            }

        binding.rvConsumePrimero.adapter =
            adapterConsumePrimero


        // ==========================================
        // MIS INGREDIENTES
        // ==========================================

        binding.rvMisIngredientes.layoutManager =
            LinearLayoutManager(
                this
            )

        adapterCategoriasIngredientes =
            CategoriaIngredientesAdapter(
                emptyList()
            ) { ingrediente ->

                mostrarDetalleIngrediente(
                    ingrediente
                )
            }

        binding.rvMisIngredientes.adapter =
            adapterCategoriasIngredientes
    }


    // ==================================================
    // BUSCAR ALACENA DEL HOGAR
    // ==================================================

    private fun cargarAlacenaHogar() {

        lifecycleScope.launch {

            try {

                val respuesta =
                    ApiClient.apiService
                        .obtenerAlacenaHogar(
                            hogId = hogId
                        )

                if (!respuesta.success) {

                    Toast.makeText(
                        this@AlacenaActivity,
                        respuesta.message,
                        Toast.LENGTH_SHORT
                    ).show()

                    return@launch
                }

                val alacena =
                    respuesta.alacena

                if (alacena == null) {

                    Toast.makeText(
                        this@AlacenaActivity,
                        "No se encontró la alacena del hogar.",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@launch
                }

                val idAlacena =
                    alacena.ALC_ID

                // Aquí ya podemos cargar los ingredientes
                cargarIngredientesAlacena(
                    idAlacena
                )

            } catch (e: Exception) {

                e.printStackTrace()

                Toast.makeText(
                    this@AlacenaActivity,
                    "Error al obtener la alacena: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    // ==================================================
    // CARGAR INGREDIENTES
    // ==================================================

    private fun cargarIngredientesAlacena(
        alcId: Int
    ) {

        lifecycleScope.launch {

            try {

                // ==========================================
                // GUARDAR ALC_ID
                // ==========================================

                this@AlacenaActivity.alcId = alcId

                Log.d(
                    "ALACENA_ING",
                    "HOG_ID=$hogId ALC_ID=$alcId"
                )

                // ==========================================
                // REQUEST
                // ==========================================

                val request =
                    ObtenerIngredientesAlacenaRequest(
                        HOG_ID = hogId,
                        ALC_ID = alcId
                    )

                // ==========================================
                // CONSULTAR API
                // ==========================================

                val respuesta =
                    ApiClient.apiService
                        .listarIngredientesAlacenaHogar(
                            request
                        )

                // ==========================================
                // VERIFICAR RESPUESTA
                // ==========================================

                if (!respuesta.success) {

                    Toast.makeText(
                        this@AlacenaActivity,
                        respuesta.message,
                        Toast.LENGTH_SHORT
                    ).show()

                    return@launch
                }

                // ==========================================
                // OBTENER INGREDIENTES
                // ==========================================

                val ingredientes =
                    respuesta.ingredientes ?: emptyList()

                Log.d(
                    "ALACENA_ING",
                    "Ingredientes encontrados: ${ingredientes.size}"
                )

                // ==========================================
                // CATEGORÍAS
                // ==========================================

                val ingredientesPorCategoria =
                    ingredientes
                        .groupBy {

                            it.categoria
                                ?.trim()
                                ?.ifEmpty {
                                    "Sin categoría"
                                }
                                ?: "Sin categoría"
                        }
                        .map { (categoria, lista) ->

                            CategoriaIngredientesAdapter
                                .CategoriaIngredientes(
                                    nombreCategoria = categoria,
                                    ingredientes = lista
                                )
                        }

                adapterCategoriasIngredientes
                    .actualizarLista(
                        ingredientesPorCategoria
                    )

                // ==========================================
                // CONSUME PRIMERO
                // ==========================================

                val consumePrimero =
                    ingredientes
                        .filter {

                            val estado =
                                it.AI_ESTADO
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

                adapterConsumePrimero
                    .actualizarLista(
                        consumePrimero
                    )

            } catch (e: Exception) {

                Log.e(
                    "ALACENA_ING",
                    "Error al cargar ingredientes",
                    e
                )

                Toast.makeText(
                    this@AlacenaActivity,
                    "Error al cargar ingredientes: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    // ==================================================
    // DETALLE DEL INGREDIENTE
    // ==================================================

    private fun mostrarDetalleIngrediente(
        ingrediente: IngredienteAlacena
    ) {

        Log.d(
            "ALACENA",
            "Ingrediente seleccionado: ${ingrediente.ING_DESCRIPCION}"
        )

        // Aquí posteriormente conectamos
        // el BottomSheet de editar ingrediente.
    }
    private fun mostrarDialogAgregarIngrediente(
        ingredienteEditar: IngredienteAlacena? = null) {
        val bottomSheet = BottomSheetDialog(this)
        val modoIngrediente =
            when {
                ingredienteEditar == null ->
                    AlacenaActivity.ModoIngrediente.AGREGAR

                ingredienteEditar.AI_ESTADO
                    ?.trim()
                    ?.equals(
                        "descompuesto",
                        ignoreCase = true
                    ) == true ->
                    AlacenaActivity.ModoIngrediente.USAR_EN_RECETA

                else ->
                    AlacenaActivity.ModoIngrediente.EDITAR
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
            if (modoIngrediente == AlacenaActivity.ModoIngrediente.USAR_EN_RECETA) {

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
            if (alcId <= 0) {

                makeText(
                    this,
                    "No se encontró la alacena del hogar.",
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

                // ==========================================
                // ALACENA DEL HOGAR
                // ==========================================

                ALC_ID = alcId,

                HOG_ID = hogId,

                ALC_CLI_ID = null,

                // ==========================================
                // INGREDIENTE
                // ==========================================

                ING_ID = ingredienteSeleccionadoId!!,

                // ==========================================
                // CANTIDAD
                // ==========================================

                AI_CANTIDAD = cantidadDouble,

                AI_UNIDAD = unidad,

                // ==========================================
                // FECHAS
                // ==========================================

                AI_FECHA_COMPRA =
                    convertirFechaMySQL(
                        txtFechaCompra.text
                            .toString()
                            .trim()
                    ),

                AI_FECHA_VENCIMIENTO =
                    convertirFechaMySQL(
                        txtFechaConsumo.text
                            .toString()
                            .trim()
                    ),

                // ==========================================
                // ESTADO
                // ==========================================

                AI_ESTADO =
                    txtTipoEstado.text
                        .toString()
                        .trim(),

                // ==========================================
                // PRECIO
                // ==========================================

                AI_PRECIO_COMPRA = precioDouble,

                // ==========================================
                // ALMACENAMIENTO
                // ==========================================

                AI_ALMACENAMIENTO =
                    txtTipoAlmacenamiento.text
                        .toString()
                        .trim(),

                // ==========================================
                // FRECUENCIA
                // ==========================================

                AI_FRECUENCIA_CONSUMO =
                    txtTipoFrecuencia.text
                        .toString()
                        .trim(),

                // ==========================================
                // ABASTECIMIENTO
                // ==========================================

                AI_TIPO_ABASTECIMIENTO =
                    txtTipoAbastecimiento.text
                        .toString()
                        .trim()
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
                            this@AlacenaActivity,
                            response.message,
                            Toast.LENGTH_SHORT
                        ).show()


                        // ======================================
                        // CERRAR BOTTOM SHEET
                        // ======================================

                        bottomSheet.dismiss()

                        cargarIngredientesAlacena(
                            alcId
                        )


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
                            this@AlacenaActivity,
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
                        this@AlacenaActivity,
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

            AlacenaActivity.ModoIngrediente.AGREGAR -> {

                btnLimpiar.text = "LIMPIAR"
                btnGuardar.text = "AGREGAR"

                vermasrecetas.visibility = View.GONE
                txtcontenedeor.visibility = View.GONE
            }

            AlacenaActivity.ModoIngrediente.EDITAR -> {

                btnLimpiar.text = "ELIMINAR"
                btnGuardar.text = "GUARDAR CAMBIOS"

                vermasrecetas.visibility = View.VISIBLE
                txtcontenedeor.visibility = View.GONE
            }

            AlacenaActivity.ModoIngrediente.USAR_EN_RECETA -> {

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
                        this@AlacenaActivity,
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
                        this@AlacenaActivity,
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
                    this@AlacenaActivity,
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
                        this@AlacenaActivity,
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
                    this@AlacenaActivity,
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
                        "ÉXITO: ${response.message}"
                    )

                    makeText(
                        this@AlacenaActivity,
                        response.message,
                        Toast.LENGTH_SHORT
                    ).show()

                    bottomSheet.dismiss()

                    // ==========================================
                    // RECARGAR ALACENA DEL HOGAR
                    // ==========================================

                    cargarIngredientesAlacena(
                        alcId
                    )
                }
            } catch (e: Exception) {
                Log.e(
                    "ALACENA_API",
                    "Error al eliminar ingrediente usado en receta",
                    e
                )
                Toast.makeText(
                    this@AlacenaActivity,
                    "Error de conexión con el servidor.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

}