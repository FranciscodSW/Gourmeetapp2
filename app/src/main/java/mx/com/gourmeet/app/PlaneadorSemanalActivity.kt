package mx.com.gourmeet.app

import android.R.attr.singleLine
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mx.com.gourmeet.app.data.api.ApiClient
import mx.com.gourmeet.app.data.models.*
import mx.com.gourmeet.app.databinding.ItemPlaneadorSemanalBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import mx.com.gourmeet.app.data.models.EliminarRecetaEspacioResponse
import mx.com.gourmeet.app.data.models.EliminarIngredienteEspacioResponse
@Suppress("DEPRECATION")
class PlaneadorSemanalActivity : AppCompatActivity() {
    private var hogUsuIdSolicitante: Int = -1
    private var preparacionColacion: String = "Sin especificar"
    private val preparacionesColacion = listOf("Picado","Con", "Y", "Hervido", "Al vapor", "Molido", "Rallado", "Licuado con", "Untado con", "Horneado", "Frito")
    private var porcionesSeleccionadas: Int? = null
    private val recetasSeleccionadasColacion = mutableListOf<RecetaconFiltro>()
    private var unidadColacion1: String = "gramos"
    private var unidadColacion2: String = "gramos"
    private var fechasComidaRepetida: Set<String> = emptySet()
    // Adaptador para seleccionar días en los que repetir una receta
    private var ingredienteColacionEnEdicion = 0
    private var adapterDiasRepeticion: DiasRepeticionAdapter? = null
    // Adaptador para mostrar los días seleccionados en el resumen
    private var adapterResumenDias: AdapterDiasSeleccionados? = null
    private lateinit var binding: ItemPlaneadorSemanalBinding
    private lateinit var adapterDia: AdapterDiaPlaneador
    private var cliId: Int = -1
    private var hogId: Int = -1
    private var plaId: Int = -1
    private data class RecetaPendientePlaneador(
        val receta: RecetaconFiltro,
        val tipoComida: Int,
        val categoriaId: Int,
        val fechas: Set<String>,
        val porciones: Int,
        val numeroColacion: Int = 0
    )
    private val recetasPendientesPlaneador = mutableListOf<RecetaPendientePlaneador>()
    private var ingredienteColacion1: BuscarIngredientes? = null
    private var ingredienteColacion2: BuscarIngredientes? = null
    private val recetasSeleccionadas = mutableListOf<RecetaconFiltro>()
    private var recetaSeleccionada: RecetaconFiltro? = null
    private var busquedaRecetasJob: Job? = null
    private var busquedaRecetasColacionJob: kotlinx.coroutines.Job? = null
    private var busquedaIngredientesJob: Job? = null
    private var numeroColacionSeleccionada: Int = 1
    private var recetaSeleccionadaColacion: RecetaconFiltro? = null
    private val formatoFecha = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val formatoDia = SimpleDateFormat("EEEE d", Locale("es", "MX"))
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ItemPlaneadorSemanalBinding.inflate(layoutInflater)
        setContentView(binding.root)
        cliId = intent.getIntExtra("CLI_ID", -1)
        hogId = intent.getIntExtra("HOG_ID", -1)
        hogUsuIdSolicitante = intent.getIntExtra("HOG_USU_ID", -1)
        if (cliId <= 0 || hogId <= 0) {
            Toast.makeText(
                this,
                "No se recibieron los datos del hogar.",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }
        configurarRecyclerView()
        configurarDiasQueVamosACocinar()
        obtenerPlaneadorSemanal()
    }

    // =========================================================
    // CONFIGURAR RECYCLERVIEW PRINCIPAL
    // =========================================================

    private fun configurarRecyclerView() {
        binding.diasDeCocina.layoutManager =
            LinearLayoutManager(this)

        adapterDia = AdapterDiaPlaneador(
            dias = mutableListOf(),

            onSeleccionarDia = { dia, estadoAnterior ->
                guardarEstadoDia(dia, estadoAnterior)
            },

            onAgregarComida = { dia, tipoComida ->
                if (dia.seleccionado) {
                    mostrarVentanaAgregarComidas(
                        dia = dia,
                        tipoComida = tipoComida
                    )
                }
            },

            onAgregarColacion = { dia, numeroColacion ->
                if (dia.seleccionado && numeroColacion in 1..2) {
                    numeroColacionSeleccionada = numeroColacion

                    mostrarVentanaAgregarColacion(
                        dia = dia,
                        numeroColacion = numeroColacion
                    )
                }
            },

            onAsignarHora = { espacio ->
                mostrarSelectorHora(espacio)
            },

            onEliminarElemento = { espacio, elemento ->
                if (
                    espacio.TPC_ID == 4 ||
                    espacio.TPC_ID in 1..3
                ) {
                    eliminarElementoDelEspacio(
                        espacio = espacio,
                        elemento = elemento
                    )
                }
            },

            // NUEVO: asignar cocinero al día seleccionado.
            onAsignarCocinero = { dia ->
                mostrarDialogoBuscarCocinero(dia)
            }
        )

        binding.diasDeCocina.adapter = adapterDia
    }


    // =========================================================
    // OBTENER PLANEADOR SEMANAL
    // =========================================================
    private fun obtenerPlaneadorSemanal() {
        lifecycleScope.launch {
            try {
                val request = PlaneadorRequest(
                    HOG_ID = hogId,
                    CLI_ID = cliId
                )

                val response =
                    ApiClient.apiService.obtenerOCrearPlaneador(request)

                if (!response.success) {
                    Toast.makeText(
                        this@PlaneadorSemanalActivity,
                        response.message,
                        Toast.LENGTH_LONG
                    ).show()

                    return@launch
                }

                // 1. Guardar el ID del planeador
                plaId = response.PLA_ID ?: -1

                if (plaId <= 0) {
                    Toast.makeText(
                        this@PlaneadorSemanalActivity,
                        "La API no devolvió un ID válido del planeador.",
                        Toast.LENGTH_LONG
                    ).show()

                    return@launch
                }

                // 2. Obtener los espacios y los días de cocina
                val espacios = response.espacios.orEmpty()
                val diasCocina = response.dias_cocina.orEmpty()

                // 3. Diagnóstico: verificar los identificadores recibidos
                diasCocina.forEach { dia ->
                    android.util.Log.d(
                        "DEBUG_IDS_PLANEADOR",
                        """
                    DÍA:
                    PLD_ID = ${dia.pldId}
                    PLA_ID = ${dia.plaId}
                    FECHA = ${dia.pldFecha}
                    PLD_COCINAR = ${dia.pldCocinar}
                    """.trimIndent()
                    )

                    if (dia.pldId <= 0) {
                        android.util.Log.e(
                            "DEBUG_IDS_PLANEADOR",
                            "ERROR: El día ${dia.pldFecha} llegó con PLD_ID=${dia.pldId}"
                        )
                    }
                }

                espacios.forEach { espacio ->
                    android.util.Log.d(
                        "DEBUG_IDS_PLANEADOR",
                        """
                    ESPACIO:
                    ECM_ID = ${espacio.ECM_ID}
                    TPC_ID = ${espacio.TPC_ID}
                    FECHA = ${espacio.ECM_FECHA}
                    """.trimIndent()
                    )

                    espacio.recetas.forEach { receta ->
                        android.util.Log.d(
                            "DEBUG_IDS_PLANEADOR",
                            """
                        RECETA:
                        NOMBRE = ${receta.REC_NOMBRE}
                        ECR_ID = ${receta.ECR_ID}
                        REC_ID = ${receta.REC_ID}
                        """.trimIndent()
                        )

                        if (receta.ECR_ID <= 0) {
                            android.util.Log.e(
                                "DEBUG_IDS_PLANEADOR",
                                "ERROR: La receta ${receta.REC_NOMBRE} " +
                                        "llegó con ECR_ID=${receta.ECR_ID}"
                            )
                        }
                    }

                    espacio.ingredientes.forEach { ingrediente ->
                        android.util.Log.d(
                            "DEBUG_IDS_PLANEADOR",
                            """
                        INGREDIENTE:
                        NOMBRE = ${ingrediente.ING_DESCRIPCION}
                        ECI_ID = ${ingrediente.ECI_ID}
                        ING_ID = ${ingrediente.ING_ID}
                        """.trimIndent()
                        )

                        if (ingrediente.ECI_ID <= 0) {
                            android.util.Log.e(
                                "DEBUG_IDS_PLANEADOR",
                                "ERROR: El ingrediente ${ingrediente.ING_DESCRIPCION} " +
                                        "llegó con ECI_ID=${ingrediente.ECI_ID}"
                            )
                        }
                    }
                }

                // 4. Validar las fechas del planeador
                val fechaInicio = response.PLA_FECHA_INICIO
                val fechaFin = response.PLA_FECHA_FIN

                if (
                    fechaInicio.isNullOrBlank() ||
                    fechaFin.isNullOrBlank()
                ) {
                    Toast.makeText(
                        this@PlaneadorSemanalActivity,
                        "La API no devolvió las fechas del planeador.",
                        Toast.LENGTH_LONG
                    ).show()

                    return@launch
                }

                // 5. Generar los días incluyendo su PLD_ID
                val dias = generarDias(
                    fechaInicio = fechaInicio,
                    fechaFin = fechaFin,
                    espacios = espacios,
                    diasCocina = diasCocina
                )

                // 6. Actualizar el RecyclerView
                adapterDia.actualizarDias(dias)

                // 7. Actualizar el resumen
                actualizarResumenDiasSeleccionados()

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Toast.makeText(
                    this@PlaneadorSemanalActivity,
                    "Error al cargar el planeador: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()

                android.util.Log.e(
                    "DEBUG_IDS_PLANEADOR",
                    "Error al cargar el planeador semanal",
                    e
                )
            }
        }
    }

    private fun generarDias(
        fechaInicio: String,
        fechaFin: String,
        espacios: List<EspacioComida>,
        diasCocina: List<DiaCocinaResponse>
    ): List<DiaPlaneador> {

        val inicio = formatoFecha.parse(fechaInicio)
            ?: return emptyList()

        val fin = formatoFecha.parse(fechaFin)
            ?: return emptyList()

        val espaciosPorFecha = espacios.groupBy {
            it.ECM_FECHA.take(10)
        }

        // Asociar cada fecha con su información del planeador.
        val diasCocinaPorFecha = diasCocina.associateBy {
            it.pldFecha.take(10)
        }

        val resultado = mutableListOf<DiaPlaneador>()

        val calendario = Calendar.getInstance().apply {
            time = inicio
        }

        val calendarioFin = Calendar.getInstance().apply {
            time = fin
        }

        while (!calendario.after(calendarioFin)) {

            val fechaActual = formatoFecha.format(calendario.time)

            val nombreDia = formatoDia.format(calendario.time)
                .replaceFirstChar {
                    if (it.isLowerCase()) {
                        it.titlecase(Locale("es", "MX"))
                    } else {
                        it.toString()
                    }
                }

            val espaciosDelDia =
                espaciosPorFecha[fechaActual].orEmpty()

            // Obtener el registro real del día.
            val diaCocina = diasCocinaPorFecha[fechaActual]

            val estadoCocinar = diaCocina?.pldCocinar
                ?: espaciosDelDia.firstOrNull()?.PLD_COCINAR
                ?: 1

            resultado.add(
                DiaPlaneador(
                    fecha = fechaActual,
                    nombreDia = nombreDia,
                    espacios = espaciosDelDia.sortedWith(
                        compareBy(
                            { it.TPC_ID },
                            { it.ECM_NUMERO_COLACION }
                        )
                    ),
                    seleccionado = estadoCocinar == 1,
                    pldId = diaCocina?.pldId ?: 0,
                    hogUsuIdCocinero = diaCocina?.hogUsuIdCocinero,
                    nombreCocinero = diaCocina?.hogUsuNombre
                )
            )

            calendario.add(Calendar.DAY_OF_MONTH, 1)
        }

        return resultado
    }

    // =========================================================
    // VENTANA DE CATEGORÍAS
    // =========================================================

    private fun mostrarVentanaAgregarComidas(
        dia: DiaPlaneador,
        tipoComida: Int
    ) {
        val dialog = BottomSheetDialog(this)
        if (tipoComida == 4) {
            mostrarVentanaAgregarColacion(
                dia = dia,
                numeroColacion = numeroColacionSeleccionada
            )
            return
        }

        val vista = LayoutInflater.from(this).inflate(
            R.layout.activity_agregar_comidas_a_dia,
            null,
            false
        )

        dialog.setContentView(vista)

        vista.findViewById<ImageButton>(
            R.id.btnCerrarIngrediente
        ).setOnClickListener {
            dialog.dismiss()
        }

        val categorias = listOf(
            R.id.btnEntrada to 5,
            R.id.btnPlatoFuerte to 3,
            R.id.btnPostre to 4,
            R.id.btnBebida to 2,
            R.id.btnAnadirExtra to 0
        )

        // Mostrar las recetas pendientes en sus botones.
        categorias.forEach { (botonId, categoriaId) ->
            val boton = vista.findViewById<MaterialButton>(botonId)

            val nombres = recetasPendientesPlaneador
                .filter {
                    it.tipoComida == tipoComida &&
                            it.categoriaId == categoriaId
                }
                .map { it.receta.REC_NOMBRE }

            if (nombres.isNotEmpty()) {
                boton.text = nombres.joinToString(", ")
            }

            boton.setOnClickListener {
                dialog.dismiss()

                mostrarVentanaBuscarRecetas(
                    dia = dia,
                    tipoComida = tipoComida,
                    categoriaId = categoriaId
                )
            }
        }

        // Guardar todas las recetas únicamente al confirmar.
        vista.findViewById<MaterialButton>(
            R.id.btnGuardarIngrediente
        ).setOnClickListener {
            if (recetasPendientesPlaneador.isEmpty()) {
                Toast.makeText(
                    this,
                    "Primero agrega al menos una receta.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            dialog.dismiss()
            guardarRecetasPendientes()
        }

        // Limpiar las selecciones temporales.
        vista.findViewById<MaterialButton>(
            R.id.btnCancelarIngrediente
        ).setOnClickListener {
            recetasPendientesPlaneador.clear()
            dialog.dismiss()

            Toast.makeText(
                this,
                "Selecciones temporales eliminadas.",
                Toast.LENGTH_SHORT
            ).show()
        }

        dialog.show()
    }


    // =========================================================
    // VENTANA DE BÚSQUEDA DE RECETAS
    // No se selecciona horario en esta ventana.
    // =========================================================


    private fun mostrarVentanaBuscarRecetas(
        dia: DiaPlaneador,
        tipoComida: Int,
        categoriaId: Int
    ) {
        val dialog = BottomSheetDialog(this)

        val vista = LayoutInflater.from(this).inflate(
            R.layout.activity_agregar_recetas_a_mi_comida,
            null,
            false
        )

        dialog.setContentView(vista)

        recetaSeleccionada = null
        recetasSeleccionadas.clear()
        porcionesSeleccionadas = null
        fechasComidaRepetida = setOf(dia.fecha)

        vista.findViewById<ImageButton>(
            R.id.btnCerrarIngrediente
        ).setOnClickListener {
            busquedaRecetasJob?.cancel()
            dialog.dismiss()
        }

        configurarBusquedaRecetas(
            vista = vista,
            categoriaId = categoriaId
        )

        configurarRepeticionComida(
            vista = vista,
            diaOriginal = dia
        )

        val btnAgregar = vista.findViewById<TextView>(
            R.id.btnAgregarAlPlaneador
        )

        btnAgregar.isEnabled = true
        btnAgregar.isClickable = true
        btnAgregar.alpha = 1f

        btnAgregar.setOnClickListener {
            val receta = recetaSeleccionada

            if (receta == null) {
                Toast.makeText(
                    this,
                    "Selecciona un platillo primero.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val fechas = fechasComidaRepetida
                .ifEmpty { setOf(dia.fecha) }
                .toSet()

            // Evitar agregar dos veces la misma receta con
            // la misma categoría, tipo de comida y fechas.
            val duplicada = recetasPendientesPlaneador.any {
                it.receta.REC_ID == receta.REC_ID &&
                        it.tipoComida == tipoComida &&
                        it.categoriaId == categoriaId &&
                        it.fechas == fechas
            }

            if (duplicada) {
                Toast.makeText(
                    this,
                    "Esta receta ya está seleccionada para esos días.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val numeroColacion = if (tipoComida == 4) {
                (dia.espacios
                    .filter { it.TPC_ID == 4 }
                    .maxOfOrNull { it.ECM_NUMERO_COLACION } ?: 0) + 1
            } else {
                0
            }

            recetasPendientesPlaneador.add(
                RecetaPendientePlaneador(
                    receta = receta,
                    tipoComida = tipoComida,
                    categoriaId = categoriaId,
                    fechas = fechas,
                    porciones = porcionesSeleccionadas ?: 1,
                    numeroColacion = numeroColacion
                )
            )

            busquedaRecetasJob?.cancel()
            dialog.dismiss()

            Toast.makeText(
                this,
                "${receta.REC_NOMBRE} agregada a la selección.",
                Toast.LENGTH_SHORT
            ).show()

            mostrarVentanaAgregarComidas(
                dia = dia,
                tipoComida = tipoComida
            )
        }

        dialog.show()
    }
    // =========================================================
    // REPETIR COMIDA EN OTROS DÍAS
    // =========================================================
    private fun configurarRepeticionComida(
        vista: View,
        diaOriginal: DiaPlaneador
    ) {
        val btnRepetir = vista.findViewById<TextView>(
            R.id.repetir_comida
        )

        val listaDias = vista.findViewById<RecyclerView>(
            R.id.seleccionardiaarepetir
        )

        val diasPlaneador = adapterDia.obtenerDias()

        val diasRepeticion = diasPlaneador.map { dia ->
            DiaRepeticion(
                fecha = dia.fecha,
                nombre = dia.nombreDia,
                inicial = dia.nombreDia
                    .take(1)
                    .uppercase()
            )
        }

        fechasComidaRepetida = setOf(diaOriginal.fecha)

        listaDias.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        adapterDiasRepeticion = DiasRepeticionAdapter(
            dias = diasRepeticion,
            fechaOriginal = diaOriginal.fecha
        ) { fechasSeleccionadas ->
            fechasComidaRepetida = fechasSeleccionadas
        }
        listaDias.adapter = adapterDiasRepeticion

        btnRepetir.setOnClickListener {
            listaDias.visibility =
                if (listaDias.visibility == View.GONE) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
        }
    }

    // =========================================================
    // BUSCAR RECETAS
    // =========================================================

    private fun configurarBusquedaRecetas(vista: View, categoriaId: Int) {
        val buscador = vista.findViewById<EditText>(
            R.id.etBuscarReceta2
        )
        val resultados = vista.findViewById<RecyclerView>(
            R.id.resultados_derecetas
        )
        resultados.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        var adapter: RecetasCardAdapterSelec? = null

        adapter = RecetasCardAdapterSelec(
            recetas = mutableListOf(),
            recetasSeleccionadas = recetasSeleccionadas,
            onRecetaClick = { receta ->
                recetaSeleccionada = receta
            },
            onSeleccionarReceta = { receta, seleccionar ->
                if (seleccionar) {
                    if (
                        recetasSeleccionadas.none {
                            it.REC_ID == receta.REC_ID
                        }
                    ) {
                        recetasSeleccionadas.add(receta)
                    }

                    recetaSeleccionada = receta

                    Toast.makeText(
                        this,
                        "Receta seleccionada: ${receta.REC_NOMBRE}",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    recetasSeleccionadas.removeAll {
                        it.REC_ID == receta.REC_ID
                    }

                    if (recetaSeleccionada?.REC_ID == receta.REC_ID) {
                        recetaSeleccionada =
                            recetasSeleccionadas.lastOrNull()
                    }
                }

                adapter?.actualizarSeleccionadas()
            }
        )

        resultados.adapter = adapter

        buscador.addTextChangedListener(
            object : android.text.TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) = Unit

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) = Unit

                override fun afterTextChanged(
                    s: android.text.Editable?
                ) {
                    val texto = s?.toString()?.trim().orEmpty()

                    busquedaRecetasJob?.cancel()

                    if (texto.length < 2) {
                        adapter?.limpiar()
                        resultados.visibility = View.GONE
                        return
                    }

                    busquedaRecetasJob = lifecycleScope.launch {
                        delay(2000)

                        try {
                            val respuesta =
                                ApiClient.apiService.getFiltrosRecetasNombre(
                                    FiltrosRecetasNombreRequest(
                                        busqueda = texto,
                                        categoriaId = categoriaId
                                    )
                                )

                            // Ignorar resultados de búsquedas anteriores.
                            if (
                                buscador.text.toString().trim() != texto
                            ) {
                                return@launch
                            }

                            if (respuesta.success) {
                                adapter?.actualizar(
                                    respuesta.coincidencia
                                )

                                resultados.visibility =
                                    if (respuesta.coincidencia.isNotEmpty()) {
                                        View.VISIBLE
                                    } else {
                                        View.GONE
                                    }
                            } else {
                                adapter?.limpiar()
                                resultados.visibility = View.GONE
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            if (
                                buscador.text.toString().trim() == texto
                            ) {
                                adapter?.limpiar()
                                resultados.visibility = View.GONE

                                Toast.makeText(
                                    this@PlaneadorSemanalActivity,
                                    "Error al buscar recetas.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }
        )

        // =====================================================
        // BOTÓN LIMPIAR
        // =====================================================

        vista.findViewById<View>(
            R.id.btnLimpiarReceta
        ).setOnClickListener {

            busquedaRecetasJob?.cancel()
            busquedaRecetasJob = null

            buscador.text.clear()

            adapter?.limpiar()
            resultados.visibility = View.GONE

            recetasSeleccionadas.clear()
            recetaSeleccionada = null
            porcionesSeleccionadas = null

            adapter?.actualizarSeleccionadas()

            val imm = getSystemService(
                Context.INPUT_METHOD_SERVICE
            ) as InputMethodManager

            imm.hideSoftInputFromWindow(
                buscador.windowToken,
                0
            )

            Toast.makeText(
                this@PlaneadorSemanalActivity,
                "Campos limpiados.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // =========================================================
    // SELECTOR DE HORA POR ESPACIO
    // =========================================================

    private fun mostrarSelectorHora(espacio: EspacioComida) {
        val calendario = Calendar.getInstance()

        val partes = espacio.ECM_HORA
            ?.take(5)
            ?.split(":")
            ?.mapNotNull { it.toIntOrNull() }

        val horaInicial = partes?.getOrNull(0)
            ?: calendario.get(Calendar.HOUR_OF_DAY)

        val minutoInicial = partes?.getOrNull(1)
            ?: calendario.get(Calendar.MINUTE)

        val selector = TimePickerDialog(
            this,
            R.style.TemaRelojGourMeet,
            { _, hora, minuto ->

                val horaElegida = String.format(
                    Locale.US,
                    "%02d:%02d",
                    hora,
                    minuto
                )

                guardarHoraEspacio(
                    espacio = espacio,
                    hora = horaElegida
                )
            },
            horaInicial,
            minutoInicial,
            true
        )

        selector.setTitle(
            when (espacio.TPC_ID) {
                1 -> "Horario del desayuno"
                2 -> "Horario de la comida"
                3 -> "Horario de la cena"
                4 -> "Horario de la colación"
                else -> "Selecciona la hora"
            }
        )

        selector.show()

        selector.getButton(TimePickerDialog.BUTTON_POSITIVE)
            ?.setTextColor(
                ContextCompat.getColor(
                    this,
                    R.color.azulgourmeet
                )
            )

        selector.getButton(TimePickerDialog.BUTTON_NEGATIVE)
            ?.setTextColor(
                ContextCompat.getColor(
                    this,
                    R.color.azulgourmeet
                )
            )
    }

    // =========================================================
    // GUARDAR HORA EN LA BASE DE DATOS
    // =========================================================

    private fun guardarHoraEspacio(
        espacio: EspacioComida,
        hora: String
    ) {
        lifecycleScope.launch {
            try {
                val respuesta =
                    ApiClient.apiService.actualizarHoraEspacio(
                        ActualizarHoraEspacioRequest(
                            HOG_ID = hogId,
                            CLI_ID = cliId,
                            ECM_ID = espacio.ECM_ID,
                            ECM_HORA = hora
                        )
                    )

                if (respuesta.success) {
                    espacio.ECM_HORA = hora

                    adapterDia.notifyDataSetChanged()

                    Toast.makeText(
                        this@PlaneadorSemanalActivity,
                        "Hora guardada: $hora",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@PlaneadorSemanalActivity,
                        respuesta.message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Toast.makeText(
                    this@PlaneadorSemanalActivity,
                    "No se pudo guardar la hora. Verifica tu conexión.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // =========================================================
    // GUARDAR RECETA EN EL PLANEADOR
    // =========================================================

    private fun guardarRecetaEnPlaneador(
        receta: RecetaconFiltro,
        diaOriginal: DiaPlaneador,
        tipoComida: Int,
        fechas: Set<String>,
        porciones: Int,
        numeroColacion: Int = 0,
        recargarAlTerminar: Boolean = true,
        alTerminar: (Boolean) -> Unit = {}
    ) {
        lifecycleScope.launch {
            try {
                val fechasFinales = (fechas + diaOriginal.fecha)
                    .distinct()
                    .sorted()

                val request = GuardarRecetaPlaneadorRequest(
                    HOG_ID = hogId,
                    CLI_ID = cliId,
                    REC_ID = receta.REC_ID,
                    TPC_ID = tipoComida,
                    fechas = fechasFinales,
                    ECR_PORCIONES = porciones.toDouble(),
                    ECM_NUMERO_COLACION = numeroColacion
                )

                val respuesta =
                    ApiClient.apiService.guardarRecetaPlaneador(request)

                if (respuesta.success) {
                    alTerminar(true)

                    if (recargarAlTerminar) {
                        obtenerPlaneadorSemanal()
                    }
                } else {
                    Toast.makeText(
                        this@PlaneadorSemanalActivity,
                        respuesta.message,
                        Toast.LENGTH_LONG
                    ).show()

                    alTerminar(false)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Toast.makeText(
                    this@PlaneadorSemanalActivity,
                    "No se pudo guardar ${receta.REC_NOMBRE}: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()

                alTerminar(false)
            }
        }
    }

    private fun guardarEstadoDia(
        dia: DiaPlaneador,
        estadoAnterior: Boolean
    ) {
        if (plaId <= 0) {
            dia.seleccionado = estadoAnterior
            adapterDia.notifyDataSetChanged()
            return
        }

        lifecycleScope.launch {
            try {
                val respuesta = ApiClient.apiService.actualizarEstadoDia(
                    ActualizarEstadoDiaRequest(
                        PLA_ID = plaId,
                        HOG_ID = hogId,
                        CLI_ID = cliId,
                        PLD_FECHA = dia.fecha,
                        PLD_COCINAR = if (dia.seleccionado) 1 else 0
                    )
                )

                if (respuesta.success) {
                    actualizarResumenDiasSeleccionados()

                    Toast.makeText(
                        this@PlaneadorSemanalActivity,
                        if (dia.seleccionado)
                            "${dia.nombreDia}: cocinar"
                        else
                            "${dia.nombreDia}: no cocinar",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    dia.seleccionado = estadoAnterior
                    adapterDia.notifyDataSetChanged()

                    Toast.makeText(
                        this@PlaneadorSemanalActivity,
                        respuesta.message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                dia.seleccionado = estadoAnterior
                adapterDia.notifyDataSetChanged()

                Toast.makeText(
                    this@PlaneadorSemanalActivity,
                    "No se pudo actualizar el día.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    private fun guardarRecetasPendientes() {
        if (recetasPendientesPlaneador.isEmpty()) {
            Toast.makeText(
                this,
                "No hay recetas pendientes por guardar.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val pendientes = recetasPendientesPlaneador.toList()

        lifecycleScope.launch {
            val guardadas = mutableListOf<RecetaPendientePlaneador>()
            val errores = mutableListOf<String>()

            for (pendiente in pendientes) {
                try {
                    val respuesta = ApiClient.apiService.guardarRecetaPlaneador(
                        GuardarRecetaPlaneadorRequest(
                            HOG_ID = hogId,
                            CLI_ID = cliId,
                            REC_ID = pendiente.receta.REC_ID,
                            TPC_ID = pendiente.tipoComida,
                            fechas = pendiente.fechas.toList().sorted(),
                            ECR_PORCIONES = pendiente.porciones.toDouble(),
                            ECM_NUMERO_COLACION = pendiente.numeroColacion
                        )
                    )

                    if (respuesta.success) {
                        guardadas.add(pendiente)
                    } else {
                        errores.add(
                            "${pendiente.receta.REC_NOMBRE}: ${respuesta.message}"
                        )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    errores.add(
                        "${pendiente.receta.REC_NOMBRE}: ${e.message ?: "Error desconocido"}"
                    )
                }
            }

            // Quitar solo las recetas guardadas correctamente.
            recetasPendientesPlaneador.removeAll(guardadas.toSet())

            // Recargar una sola vez al finalizar.
            obtenerPlaneadorSemanal()

            if (errores.isEmpty()) {
                Toast.makeText(
                    this@PlaneadorSemanalActivity,
                    "Se guardaron ${guardadas.size} recetas correctamente.",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    this@PlaneadorSemanalActivity,
                    "Guardadas: ${guardadas.size}. Pendientes con error: ${errores.size}. Revisa los registros.",
                    Toast.LENGTH_LONG
                ).show()

                errores.forEach { error ->
                    android.util.Log.e("PlaneadorSemanal", error)
                }
            }
        }
    }
    private fun actualizarResumenDiasSeleccionados() {
        // Obtener todos los días del planeador
        val dias = adapterDia.obtenerDias()
        // Filtrar los días seleccionados
        val diasSeleccionados = dias.filter {
            it.seleccionado
        }
        // Contar las recetas de los días seleccionados
        val totalRecetas = diasSeleccionados.sumOf { dia ->
            dia.espacios.sumOf { espacio ->
                espacio.recetas.size
            }
        }
        // Actualizar el total de recetas
        binding.txtTotalRecetas.text =
            "Vas a cocinar: $totalRecetas recetas"

        // Actualizar el resumen horizontal de días
        adapterResumenDias?.actualizarDias(diasSeleccionados)
    }
    private fun configurarDiasQueVamosACocinar() {
        binding.rvdiasquevamosacocinar.layoutManager =
            LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL,
                false
            )

        adapterResumenDias =
            AdapterDiasSeleccionados(emptyList())

        binding.rvdiasquevamosacocinar.adapter =
            adapterResumenDias
    }
    private fun confirmarEliminarComida(
        receta: RecetaEspacio,
        ecmId: Int
    ) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Eliminar platillo")
            .setMessage(
                "¿Deseas quitar \"${receta.REC_NOMBRE}\" de esta planeación?"
            )
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->

                eliminarComidaDePlaneacion(receta, ecmId)
            }
            .show()
    }
    private fun eliminarComidaDePlaneacion(
        receta: RecetaEspacio,
        ecmId: Int
    ) {
        lifecycleScope.launch {
            try {
                val request = EliminarRecetaEspacioRequest(
                    ECR_ID = receta.ECR_ID,
                    ECM_ID = ecmId,
                    PLA_ID = plaId,
                    CLI_ID = cliId,
                    HOG_ID = hogId
                )

                val response = ApiClient.apiService.eliminarRecetaEspacio(
                    request
                )

                if (response.success) {
                    android.widget.Toast.makeText(
                        this@PlaneadorSemanalActivity,
                        response.message,
                        android.widget.Toast.LENGTH_SHORT
                    ).show()

                    // Recarga el planeador desde el servidor para
                    // actualizar las recetas y el resumen.
                    obtenerPlaneadorSemanal()

                } else {
                    android.widget.Toast.makeText(
                        this@PlaneadorSemanalActivity,
                        response.message,
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                android.util.Log.e(
                    "ELIMINAR_COMIDA",
                    "Error al eliminar el platillo",
                    e
                )

                android.widget.Toast.makeText(
                    this@PlaneadorSemanalActivity,
                    "No se pudo eliminar el platillo. Verifica tu conexión.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun mostrarVentanaAgregarColacion(
        dia: DiaPlaneador,
        numeroColacion: Int
    ) {
        // =========================================
        // VALIDAR NÚMERO DE COLACIÓN
        // =========================================

        if (numeroColacion !in 1..2) {
            Toast.makeText(
                this,
                "Número de colación no válido.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        numeroColacionSeleccionada = numeroColacion

        // =========================================
        // CREAR VENTANA
        // =========================================

        val dialog = BottomSheetDialog(this)

        val vista = LayoutInflater.from(this).inflate(
            R.layout.activity_agregar_recetas_a_mi_colacion,
            null,
            false
        )

        dialog.setContentView(vista)

        // =========================================
        // REINICIAR EL ESTADO DE LA COLACIÓN
        // =========================================

        recetasSeleccionadasColacion.clear()
        recetaSeleccionadaColacion = null

        ingredienteColacion1 = null
        ingredienteColacion2 = null

        unidadColacion1 = "gramos"
        unidadColacion2 = "gramos"

        preparacionColacion = "Sin especificar"

        fechasComidaRepetida = setOf(dia.fecha)

        // =========================================
        // CANCELAR BÚSQUEDAS ANTERIORES
        // =========================================

        busquedaRecetasColacionJob?.cancel()
        busquedaRecetasColacionJob = null

        // =========================================
        // BOTÓN PARA CERRAR LA VENTANA
        // =========================================

        vista.findViewById<ImageButton>(
            R.id.btnCerrarIngrediente
        ).setOnClickListener {
            busquedaRecetasColacionJob?.cancel()
            busquedaRecetasColacionJob = null

            dialog.dismiss()
        }

        // =========================================
        // BUSCADOR DE RECETAS E INGREDIENTES
        // =========================================

        configurarBusquedaColacion(vista)

        // =========================================
        // SELECTOR DE UNIDAD DEL INGREDIENTE 1
        // =========================================

        configurarSelectorUnidad(
            vista,
            R.id.seleccionar_unidad_de_media1
        ) { unidad ->
            unidadColacion1 = unidad

            vista.findViewById<TextView>(
                R.id.txtUnidad1
            ).text = abreviarUnidadColacion(unidad)

            actualizarEstadoBotonColacion(vista)
        }

        // =========================================
        // SELECTOR DE UNIDAD DEL INGREDIENTE 2
        // =========================================

        configurarSelectorUnidad(
            vista,
            R.id.seleccionar_unidad_de_media2
        ) { unidad ->
            unidadColacion2 = unidad

            vista.findViewById<TextView>(
                R.id.txtUnidad2
            ).text = abreviarUnidadColacion(unidad)

            actualizarEstadoBotonColacion(vista)
        }

        // =========================================
        // SELECTOR DE PREPARACIÓN
        // =========================================

        configurarSelectorPreparacion(vista)

        // =========================================
        // CONFIGURAR REPETICIÓN POR DÍAS
        // =========================================

        configurarRepeticionComida(
            vista = vista,
            diaOriginal = dia
        )

        // =========================================
        // VALIDAR CANTIDADES EN TIEMPO REAL
        // =========================================

        val cantidad1 = vista.findViewById<EditText>(
            R.id.cantidad1
        )

        val cantidad2 = vista.findViewById<EditText>(
            R.id.cantidad2
        )

        val actualizarBoton = object : TextWatcher {

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) = Unit

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                actualizarEstadoBotonColacion(vista)
            }

            override fun afterTextChanged(
                s: Editable?
            ) = Unit
        }

        cantidad1.addTextChangedListener(actualizarBoton)
        cantidad2.addTextChangedListener(actualizarBoton)

        // =========================================
        // ESTADO INICIAL DEL BOTÓN
        // =========================================

        actualizarEstadoBotonColacion(vista)

        // =========================================
        // BOTÓN AGREGAR AL PLANEADOR
        // =========================================

        val btnAgregar = vista.findViewById<View>(
            R.id.btnAgregarAlPlaneador
        )

        btnAgregar.setOnClickListener {

            // Validar la selección antes de guardar.
            actualizarEstadoBotonColacion(vista)

            if (!btnAgregar.isEnabled) {
                Toast.makeText(
                    this,
                    "Selecciona una receta o completa los ingredientes y la preparación.",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            // Evitar pulsaciones repetidas.
            btnAgregar.isEnabled = false

            // Guardar usando el número de colación de esta ventana.
            guardarColacionSeleccionada(
                vista = vista,
                dialog = dialog,
                numeroColacion = numeroColacion
            )
        }

        // =========================================
        // LIMPIAR AL CERRAR LA VENTANA
        // =========================================

        dialog.setOnDismissListener {
            obtenerPlaneadorSemanal()
            busquedaRecetasColacionJob?.cancel()
            busquedaRecetasColacionJob = null
            cantidad1.removeTextChangedListener(actualizarBoton)
            cantidad2.removeTextChangedListener(actualizarBoton)
        }

        // =========================================
        // MOSTRAR VENTANA
        // =========================================

        dialog.show()
    }

    private fun guardarColacionSeleccionada(
        vista: View,
        dialog: BottomSheetDialog,
        numeroColacion: Int
    ) {
        // Evitar guardar si falta el plan.
        if (plaId <= 0) {
            Toast.makeText(
                this,
                "No se encontró el planificador.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Obtener las fechas seleccionadas.
        val fechas = fechasComidaRepetida.toList().sorted()

        if (fechas.isEmpty()) {
            Toast.makeText(
                this,
                "Selecciona al menos un día.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Validar que se haya seleccionado una receta
        // o al menos un ingrediente.
        val receta = recetaSeleccionadaColacion
        val ing1 = ingredienteColacion1
        val ing2 = ingredienteColacion2

        if (receta == null && ing1 == null && ing2 == null) {
            Toast.makeText(
                this,
                "Selecciona una receta o un ingrediente.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // La receta y los ingredientes son modalidades excluyentes.
        if (receta != null && (ing1 != null || ing2 != null)) {
            Toast.makeText(
                this,
                "Selecciona una receta o ingredientes, no ambos.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Obtener cantidades y validar los ingredientes.
        val cantidad1Texto = vista.findViewById<EditText>(
            R.id.cantidad1
        ).text.toString().trim()

        val cantidad2Texto = vista.findViewById<EditText>(
            R.id.cantidad2
        ).text.toString().trim()

        val cantidad1 = cantidad1Texto.replace(",", ".").toDoubleOrNull()
        val cantidad2 = cantidad2Texto.replace(",", ".").toDoubleOrNull()

        if (ing1 != null && (cantidad1 == null || cantidad1 <= 0)) {
            Toast.makeText(
                this,
                "Ingresa una cantidad válida para el primer ingrediente.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (ing2 != null && (cantidad2 == null || cantidad2 <= 0)) {
            Toast.makeText(
                this,
                "Ingresa una cantidad válida para el segundo ingrediente.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (receta == null && preparacionColacion == "Sin especificar") {
            Toast.makeText(
                this,
                "Selecciona la preparación de la colación.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Preparar la lista de ingredientes.
        val ingredientes = mutableListOf<IngredienteColacionRequest>()

        if (ing1 != null) {
            ingredientes.add(
                IngredienteColacionRequest(
                    ING_ID = ing1.id,
                    ECI_CANTIDAD = cantidad1,
                    ECI_UNIDAD = unidadColacion1
                )
            )
        }

        if (ing2 != null) {
            ingredientes.add(
                IngredienteColacionRequest(
                    ING_ID = ing2.id,
                    ECI_CANTIDAD = cantidad2,
                    ECI_UNIDAD = unidadColacion2
                )
            )
        }

        // Evitar doble pulsación mientras se guardan los registros.
        val boton = vista.findViewById<View>(
            R.id.btnAgregarAlPlaneador
        )
        boton.isEnabled = false

        lifecycleScope.launch {
            try {
                var guardadas = 0

                for (fecha in fechas) {
                    val request = GuardarColacionRequest(
                        PLA_ID = plaId,
                        ECM_FECHA = fecha,
                        ECM_HORA = null,
                        ECM_NUMERO_COLACION = numeroColacion,
                        ECM_NOTAS = null,
                        REC_ID = receta?.REC_ID,
                        ECR_PORCIONES = 1.0,
                        ingredientes = ingredientes.toList()
                    )

                    val respuesta = ApiClient.apiService.guardarColacion(request)

                    if (!respuesta.success) {
                        throw Exception(
                            respuesta.message.ifBlank {
                                "No se pudo guardar la colación del $fecha."
                            }
                        )
                    }

                    guardadas++
                }

                Toast.makeText(
                    this@PlaneadorSemanalActivity,
                    "Colación guardada en $guardadas día(s).",
                    Toast.LENGTH_LONG
                ).show()

                dialog.dismiss()

                // Aquí recarga el planeador si ya tienes una función
                // para actualizar las comidas de la semana.

            } catch (e: Exception) {
                Toast.makeText(
                    this@PlaneadorSemanalActivity,
                    "Error al guardar la colación: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()

            } finally {
                if (dialog.isShowing) {
                    boton.isEnabled = true
                    actualizarEstadoBotonColacion(vista)
                }
            }
        }
    }
    private fun seleccionarIngredienteColacion(
        ingrediente: BuscarIngredientes, vista: View, adapter: IngredienteColacionAdapter
    ) {

        // =====================================================
        // VALIDAR QUE NO HAYA UNA RECETA SELECCIONADA
        // =====================================================

        if (recetasSeleccionadasColacion.isNotEmpty()) {
            Toast.makeText(
                this,
                "Quita la receta antes de seleccionar ingredientes.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // =====================================================
        // OBTENER VISTAS
        // =====================================================

        val txtIngrediente1 = vista.findViewById<TextView>(
            R.id.ingrediente1
        )

        val txtIngrediente2 = vista.findViewById<TextView>(
            R.id.ingrediente2
        )

        val cantidad1 = vista.findViewById<EditText>(
            R.id.cantidad1
        )

        val cantidad2 = vista.findViewById<EditText>(
            R.id.cantidad2
        )

        val txtUnidad1 = vista.findViewById<TextView>(
            R.id.txtUnidad1
        )

        val txtUnidad2 = vista.findViewById<TextView>(
            R.id.txtUnidad2
        )

        val contenedor = vista.findViewById<View>(
            R.id.contenedorparacomplementos
        )

        // =====================================================
        // VALIDAR SI EL INGREDIENTE YA ESTÁ EN EL OTRO ESPACIO
        // =====================================================

        if (
            ingredienteColacionEnEdicion == 1 &&
            ingredienteColacion2?.id == ingrediente.id
        ) {
            Toast.makeText(
                this,
                "Este ingrediente ya está en el espacio 2.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (
            ingredienteColacionEnEdicion == 2 &&
            ingredienteColacion1?.id == ingrediente.id
        ) {
            Toast.makeText(
                this,
                "Este ingrediente ya está en el espacio 1.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // =====================================================
        // AGREGAR, QUITAR O REEMPLAZAR
        // =====================================================

        when (ingredienteColacionEnEdicion) {

            // -------------------------------------------------
            // REEMPLAZAR INGREDIENTE 1
            // -------------------------------------------------

            1 -> {

                ingredienteColacion1 = ingrediente
                txtIngrediente1.text = ingrediente.nombre

                // Conservar la cantidad y unidad existentes
                // si ya había un ingrediente en este espacio.
                if (cantidad1.text.isNullOrBlank()) {
                    unidadColacion1 = "gramos"
                    txtUnidad1.text =
                        abreviarUnidadColacion(unidadColacion1)
                }

                ingredienteColacionEnEdicion = 0
            }

            // -------------------------------------------------
            // REEMPLAZAR INGREDIENTE 2
            // -------------------------------------------------

            2 -> {

                ingredienteColacion2 = ingrediente
                txtIngrediente2.text = ingrediente.nombre

                if (cantidad2.text.isNullOrBlank()) {
                    unidadColacion2 = "gramos"
                    txtUnidad2.text =
                        abreviarUnidadColacion(unidadColacion2)
                }

                ingredienteColacionEnEdicion = 0
            }

            // -------------------------------------------------
            // AGREGAR NORMALMENTE
            // -------------------------------------------------

            else -> {

                when {

                    // Si ya existe en el espacio 1, quitarlo.
                    ingredienteColacion1?.id == ingrediente.id -> {

                        ingredienteColacion1 = null

                        txtIngrediente1.text = "Ingrediente"
                        cantidad1.setText("")

                        unidadColacion1 = "gramos"
                        txtUnidad1.text =
                            abreviarUnidadColacion(unidadColacion1)
                    }

                    // Si ya existe en el espacio 2, quitarlo.
                    ingredienteColacion2?.id == ingrediente.id -> {

                        ingredienteColacion2 = null

                        txtIngrediente2.text = "Ingrediente"
                        cantidad2.setText("")

                        unidadColacion2 = "gramos"
                        txtUnidad2.text =
                            abreviarUnidadColacion(unidadColacion2)
                    }

                    // Agregar al primer espacio libre.
                    ingredienteColacion1 == null -> {

                        ingredienteColacion1 = ingrediente
                        txtIngrediente1.text = ingrediente.nombre

                        if (cantidad1.text.isNullOrBlank()) {
                            unidadColacion1 = "gramos"
                            txtUnidad1.text =
                                abreviarUnidadColacion(unidadColacion1)
                        }
                    }

                    // Agregar al segundo espacio libre.
                    ingredienteColacion2 == null -> {

                        ingredienteColacion2 = ingrediente
                        txtIngrediente2.text = ingrediente.nombre

                        if (cantidad2.text.isNullOrBlank()) {
                            unidadColacion2 = "gramos"
                            txtUnidad2.text =
                                abreviarUnidadColacion(unidadColacion2)
                        }
                    }

                    // No permitir un tercer ingrediente.
                    else -> {

                        Toast.makeText(
                            this,
                            "Solo puedes seleccionar dos ingredientes.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        // =====================================================
        // ACTUALIZAR CONTENEDOR
        // =====================================================

        val hayIngredientes =
            ingredienteColacion1 != null ||
                    ingredienteColacion2 != null

        contenedor.visibility = if (hayIngredientes) {
            View.VISIBLE
        } else {
            View.GONE
        }

        // =====================================================
        // ACTUALIZAR SELECCIÓN VISUAL
        // =====================================================

        adapter.actualizarSeleccion(
            setOfNotNull(
                ingredienteColacion1?.id,
                ingredienteColacion2?.id
            )
        )
    }
    @RequiresApi(Build.VERSION_CODES.HONEYCOMB)
    private fun configurarSelectorUnidad(vista: View, idSelector: Int, alSeleccionar: (String) -> Unit
    ) {
        val selector = vista.findViewById<ImageView>(idSelector)

        val unidades = listOf(
            "gramos",
            "kilogramos",
            "mililitros",
            "litros",
            "pieza(s)",
            "taza(s)",
            "cucharada(s)",
            "cucharadita(s)"
        )

        selector.setOnClickListener {
            PopupMenu(this, selector).apply {
                unidades.forEach { unidad ->
                    menu.add(unidad)
                }

                setOnMenuItemClickListener { item ->
                    alSeleccionar(item.title.toString())
                    true
                }

                show()
            }
        }
    }
    private fun configurarBusquedaColacion(vista: View) {

        val buscador = vista.findViewById<EditText>(
            R.id.etBuscarReceta2
        ) ?: return

        val resultadosRecetas = vista.findViewById<RecyclerView>(
            R.id.resultados_derecetas
        ) ?: return

        val resultadosIngredientes = vista.findViewById<RecyclerView>(
            R.id.resultados_ingredientes
        ) ?: return

        val contenedorIngredientes = vista.findViewById<View>(
            R.id.contenedorparacomplementos
        ) ?: return

        val contenedorIngrediente1 = vista.findViewById<View>(
            R.id.ingrediente1
        ) ?: return

        val contenedorIngrediente2 = vista.findViewById<View>(
            R.id.ingrediente2
        ) ?: return

        // =====================================================
        // CONFIGURAR RECYCLERVIEWS
        // =====================================================

        resultadosRecetas.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.HORIZONTAL,
            false
        )

        resultadosIngredientes.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.HORIZONTAL,
            false
        )

        // =====================================================
        // CLIC EN LOS CONTENEDORES DE INGREDIENTES
        // =====================================================

        contenedorIngrediente1.setOnClickListener {

            if (recetasSeleccionadasColacion.isNotEmpty()) {
                Toast.makeText(
                    this,
                    "Quita la receta antes de agregar ingredientes.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            ingredienteColacionEnEdicion = 1

            // Limpiar la búsqueda anterior para comenzar una nueva.
            buscador.setText("")
            buscador.requestFocus()

            resultadosRecetas.visibility = View.GONE
            resultadosIngredientes.visibility = View.GONE
            contenedorIngredientes.visibility = View.VISIBLE

            Toast.makeText(
                this,
                "Busca el ingrediente que ocupará el espacio 1.",
                Toast.LENGTH_SHORT
            ).show()
        }

        contenedorIngrediente2.setOnClickListener {

            if (recetasSeleccionadasColacion.isNotEmpty()) {
                Toast.makeText(
                    this,
                    "Quita la receta antes de agregar ingredientes.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            ingredienteColacionEnEdicion = 2

            buscador.setText("")
            buscador.requestFocus()

            resultadosRecetas.visibility = View.GONE
            resultadosIngredientes.visibility = View.GONE
            contenedorIngredientes.visibility = View.VISIBLE

            Toast.makeText(
                this,
                "Busca el ingrediente que ocupará el espacio 2.",
                Toast.LENGTH_SHORT
            ).show()
        }

        // =====================================================
        // ADAPTADOR DE RECETAS
        // =====================================================
        var adapterRecetas: RecetasCardAdapterSelec? = null

        adapterRecetas = RecetasCardAdapterSelec(
            recetas = mutableListOf(),
            recetasSeleccionadas = recetasSeleccionadasColacion,

            onRecetaClick = { receta ->

                recetaSeleccionadaColacion = receta

                // Actualizar el botón cuando se pulsa la tarjeta.
                actualizarEstadoBotonColacion(vista)
            },

            onSeleccionarReceta = { receta, seleccionar ->

                if (seleccionar) {

                    if (
                        ingredienteColacion1 != null ||
                        ingredienteColacion2 != null
                    ) {
                        Toast.makeText(
                            this,
                            "Quita los ingredientes antes de seleccionar una receta.",
                            Toast.LENGTH_SHORT
                        ).show()

                        adapterRecetas?.actualizarSeleccionadas()

                    } else {

                        recetasSeleccionadasColacion.clear()
                        recetasSeleccionadasColacion.add(receta)
                        recetaSeleccionadaColacion = receta

                        ingredienteColacionEnEdicion = 0

                        contenedorIngredientes.visibility = View.GONE
                        resultadosIngredientes.visibility = View.GONE
                        resultadosRecetas.visibility = View.VISIBLE

                        Toast.makeText(
                            this,
                            "Receta seleccionada: ${receta.REC_NOMBRE}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                } else {

                    recetasSeleccionadasColacion.removeAll {
                        it.REC_ID == receta.REC_ID
                    }

                    recetaSeleccionadaColacion =
                        recetasSeleccionadasColacion.lastOrNull()

                    if (recetasSeleccionadasColacion.isEmpty()) {
                        contenedorIngredientes.visibility = View.VISIBLE
                    }
                }

                adapterRecetas?.actualizarSeleccionadas()

                // IMPORTANTE: reevaluar el botón después de seleccionar
                // o quitar la receta.
                actualizarEstadoBotonColacion(vista)
            }
        )

        resultadosRecetas.adapter = adapterRecetas


        // =====================================================
        // ADAPTADOR DE INGREDIENTES
        // =====================================================

        var adapterIngredientes: IngredienteColacionAdapter? = null

        adapterIngredientes = IngredienteColacionAdapter { ingrediente ->

            if (recetasSeleccionadasColacion.isNotEmpty()) {

                Toast.makeText(
                    this,
                    "Quita la receta antes de seleccionar ingredientes.",
                    Toast.LENGTH_SHORT
                ).show()

                adapterIngredientes?.actualizarSeleccion(
                    setOfNotNull(
                        ingredienteColacion1?.id,
                        ingredienteColacion2?.id
                    )
                )

            } else {

                adapterIngredientes?.let { adapter ->

                    seleccionarIngredienteColacion(
                        ingrediente = ingrediente,
                        vista = vista,
                        adapter = adapter
                    )

                    // Al seleccionar un ingrediente, ocultar recetas.
                    if (
                        ingredienteColacion1 != null ||
                        ingredienteColacion2 != null
                    ) {
                        resultadosRecetas.visibility = View.GONE
                        contenedorIngredientes.visibility = View.VISIBLE
                    }
                }
            }
        }

        resultadosIngredientes.adapter = adapterIngredientes

        // =====================================================
        // BUSCADOR UNIFICADO: RECETAS E INGREDIENTES
        // =====================================================

        buscador.addTextChangedListener(
            object : android.text.TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) = Unit

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) = Unit

                override fun afterTextChanged(
                    s: android.text.Editable?
                ) {

                    val texto = s?.toString()?.trim().orEmpty()

                    busquedaRecetasColacionJob?.cancel()

                    if (texto.length < 2) {

                        adapterRecetas?.limpiar()
                        adapterIngredientes?.limpiar()

                        resultadosRecetas.visibility = View.GONE
                        resultadosIngredientes.visibility = View.GONE

                        return
                    }

                    busquedaRecetasColacionJob = lifecycleScope.launch {

                        kotlinx.coroutines.delay(400)

                        try {

                            val resultadoRecetas = async {
                                ApiClient.apiService.getFiltrosRecetasNombre(
                                    FiltrosRecetasNombreRequest(
                                        busqueda = texto,
                                        categoriaId = 0
                                    )
                                )
                            }

                            val resultadoIngredientes = async {
                                ApiClient.apiService.autocompleteIngredientes(
                                    texto
                                )
                            }

                            val respuestaRecetas = resultadoRecetas.await()
                            val respuestaIngredientes =
                                resultadoIngredientes.await()

                            // Ignorar respuestas de búsquedas anteriores.
                            if (buscador.text.toString().trim() != texto) {
                                return@launch
                            }
                            // =============================================
                            // RESULTADOS DE RECETAS
                            // =============================================
                            if (respuestaRecetas.success) {
                                adapterRecetas?.actualizar(
                                    respuestaRecetas.coincidencia
                                )

                                resultadosRecetas.visibility =
                                    if (
                                        ingredienteColacion1 == null &&
                                        ingredienteColacion2 == null &&
                                        respuestaRecetas.coincidencia.isNotEmpty()
                                    ) {
                                        View.VISIBLE
                                    } else {
                                        View.GONE
                                    }

                            } else {

                                adapterRecetas?.limpiar()
                                resultadosRecetas.visibility = View.GONE
                            }

                            // =============================================
                            // RESULTADOS DE INGREDIENTES
                            // =============================================

                            if (
                                respuestaIngredientes.success &&
                                respuestaIngredientes.ingredientes.isNotEmpty()
                            ) {

                                adapterIngredientes?.actualizarLista(
                                    respuestaIngredientes.ingredientes
                                )

                                adapterIngredientes?.actualizarSeleccion(
                                    setOfNotNull(
                                        ingredienteColacion1?.id,
                                        ingredienteColacion2?.id
                                    )
                                )

                                resultadosIngredientes.visibility =
                                    if (
                                        recetasSeleccionadasColacion.isEmpty()
                                    ) {
                                        View.VISIBLE
                                    } else {
                                        View.GONE
                                    }

                            } else {

                                adapterIngredientes?.limpiar()
                                resultadosIngredientes.visibility = View.GONE
                            }

                            // =============================================
                            // RESPETAR LA MODALIDAD SELECCIONADA
                            // =============================================

                            if (recetasSeleccionadasColacion.isNotEmpty()) {

                                contenedorIngredientes.visibility = View.GONE
                                resultadosIngredientes.visibility = View.GONE

                                if (
                                    respuestaRecetas.success &&
                                    respuestaRecetas.coincidencia.isNotEmpty()
                                ) {
                                    resultadosRecetas.visibility = View.VISIBLE
                                }
                            }

                            if (
                                ingredienteColacion1 != null ||
                                ingredienteColacion2 != null
                            ) {
                                resultadosRecetas.visibility = View.GONE
                            }

                        } catch (e: kotlinx.coroutines.CancellationException) {

                            throw e

                        } catch (e: Exception) {

                            if (buscador.text.toString().trim() == texto) {

                                adapterRecetas?.limpiar()
                                adapterIngredientes?.limpiar()

                                resultadosRecetas.visibility = View.GONE
                                resultadosIngredientes.visibility = View.GONE

                                android.util.Log.e(
                                    "COLACION",
                                    "Error buscando recetas o ingredientes",
                                    e
                                )
                            }
                        }
                    }
                }
            }
        )
    }
    private fun configurarSelectorPreparacion(vista: View) {
        val selector = vista.findViewById<View>(
            R.id.selectorPreparacionColacion
        )

        val texto = vista.findViewById<TextView>(
            R.id.txtPreparacionColacion
        )

        selector.setOnClickListener {
            PopupMenu(this, selector).apply {
                preparacionesColacion.forEach { preparacion ->
                    menu.add(preparacion)
                }

                setOnMenuItemClickListener { item ->
                    preparacionColacion = item.title.toString()
                    texto.text = preparacionColacion
                    true
                }

                show()
            }
        }
    }
    private fun abreviarUnidadColacion(unidad: String): String {
        return when (unidad) {
            "gramos" -> "g"
            "kilogramos" -> "kg"
            "mililitros" -> "ml"
            "litros" -> "l"
            "pieza(s)" -> "pza"
            "taza(s)" -> "tza"
            "cucharada(s)" -> "cda"
            "cucharadita(s)" -> "cdta"
            else -> unidad
        }
    }
    private fun mostrarModoColacion(vista: View, mostrarIngredientes: Boolean) {
        val resultadosIngredientes = vista.findViewById<RecyclerView>(
            R.id.resultados_ingredientes
        )
        val resultadosRecetas = vista.findViewById<RecyclerView>(
            R.id.resultados_derecetas
        )
        if (mostrarIngredientes) {
            resultadosIngredientes.visibility = View.VISIBLE
            resultadosRecetas.visibility = View.GONE
        } else {
            resultadosIngredientes.visibility = View.GONE
            resultadosRecetas.visibility = View.VISIBLE
        }
    }
    private fun actualizarEstadoBotonColacion(vista: View) {

        val btnAgregar = vista.findViewById<View>(
            R.id.btnAgregarAlPlaneador
        )

        // Detectar si hay una receta seleccionada.
        val hayReceta =
            recetaSeleccionadaColacion != null ||
                    recetasSeleccionadasColacion.isNotEmpty()

        // Detectar ingredientes seleccionados.
        val hayIngrediente1 = ingredienteColacion1 != null
        val hayIngrediente2 = ingredienteColacion2 != null

        val hayIngredientes = hayIngrediente1 || hayIngrediente2

        // MODALIDAD 1: receta seleccionada.
        // No necesita cantidades ni preparación de ingredientes.
        if (hayReceta) {

            val seleccionValida = !hayIngredientes

            btnAgregar.isEnabled = seleccionValida
            btnAgregar.alpha = if (seleccionValida) 1f else 0.5f

            return
        }

        // MODALIDAD 2: ingredientes sueltos.
        val cantidad1 = vista.findViewById<EditText>(
            R.id.cantidad1
        ).text.toString().trim()
            .replace(",", ".")
            .toDoubleOrNull()

        val cantidad2 = vista.findViewById<EditText>(
            R.id.cantidad2
        ).text.toString().trim()
            .replace(",", ".")
            .toDoubleOrNull()

        val cantidad1Valida =
            !hayIngrediente1 || (cantidad1 != null && cantidad1 > 0)

        val cantidad2Valida =
            !hayIngrediente2 || (cantidad2 != null && cantidad2 > 0)

        val preparacionValida =
            preparacionColacion != "Sin especificar"

        val seleccionValida =
            hayIngredientes &&
                    cantidad1Valida &&
                    cantidad2Valida &&
                    preparacionValida

        btnAgregar.isEnabled = seleccionValida
        btnAgregar.alpha = if (seleccionValida) 1f else 0.5f
    }
    private fun eliminarElementoDelEspacio(
        espacio: EspacioComida,
        elemento: ElementoComida
    ) {
        if (plaId <= 0 || cliId <= 0 || hogId <= 0) {
            Toast.makeText(
                this,
                "No se encontraron los datos del planeador.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (espacio.ECM_ID <= 0) {
            Toast.makeText(
                this,
                "El espacio de comida no tiene un identificador válido.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // El elemento debe ser exclusivamente receta o ingrediente.
        val esReceta = elemento.ecrId != null
        val esIngrediente = elemento.eciId != null

        if (esReceta == esIngrediente) {
            android.util.Log.e(
                "ELIMINAR_ELEMENTO",
                "Tipo inválido: nombre=${elemento.nombre}, " +
                        "ECR_ID=${elemento.ecrId}, ECI_ID=${elemento.eciId}"
            )

            Toast.makeText(
                this,
                "No se pudo identificar el tipo de elemento.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // Obtener el ID correspondiente.
        val idElemento: Int = (
                if (esReceta) elemento.ecrId else elemento.eciId
                ) ?: run {
            Toast.makeText(
                this,
                "El elemento no tiene un ID válido.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (idElemento <= 0) {
            android.util.Log.e(
                "ELIMINAR_ELEMENTO",
                "ID inválido: nombre=${elemento.nombre}, " +
                        "ECR_ID=${elemento.ecrId}, " +
                        "ECI_ID=${elemento.eciId}, " +
                        "ECM_ID=${espacio.ECM_ID}"
            )

            Toast.makeText(
                this,
                "El identificador de este elemento es 0. " +
                        "Hay que revisar cómo se cargó desde la API.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(
                if (esReceta) "Eliminar platillo"
                else "Eliminar ingrediente"
            )
            .setMessage(
                "¿Deseas quitar \"${elemento.nombre}\" de esta planeación?"
            )
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->
                lifecycleScope.launch {
                    try {
                        android.util.Log.d(
                            "ELIMINAR_ELEMENTO",
                            "Tipo=${if (esReceta) "RECETA" else "INGREDIENTE"}, " +
                                    "ID=$idElemento, ECM_ID=${espacio.ECM_ID}, " +
                                    "PLA_ID=$plaId, CLI_ID=$cliId, HOG_ID=$hogId"
                        )

                        val mensaje: String

                        if (esReceta) {
                            val respuesta =
                                ApiClient.apiService.eliminarRecetaEspacio(
                                    EliminarRecetaEspacioRequest(
                                        ECR_ID = idElemento,
                                        ECM_ID = espacio.ECM_ID,
                                        PLA_ID = plaId,
                                        CLI_ID = cliId,
                                        HOG_ID = hogId
                                    )
                                )

                            if (!respuesta.success) {
                                Toast.makeText(
                                    this@PlaneadorSemanalActivity,
                                    respuesta.message,
                                    Toast.LENGTH_LONG
                                ).show()
                                return@launch
                            }

                            mensaje = respuesta.message
                        } else {
                            val respuesta =
                                ApiClient.apiService.eliminarIngredienteEspacio(
                                    EliminarIngredienteEspacioRequest(
                                        ECI_ID = idElemento,
                                        ECM_ID = espacio.ECM_ID,
                                        PLA_ID = plaId,
                                        CLI_ID = cliId,
                                        HOG_ID = hogId
                                    )
                                )

                            if (!respuesta.success) {
                                Toast.makeText(
                                    this@PlaneadorSemanalActivity,
                                    respuesta.message,
                                    Toast.LENGTH_LONG
                                ).show()
                                return@launch
                            }

                            mensaje = respuesta.message
                        }

                        Toast.makeText(
                            this@PlaneadorSemanalActivity,
                            mensaje,
                            Toast.LENGTH_SHORT
                        ).show()

                        obtenerPlaneadorSemanal()

                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e

                    } catch (e: retrofit2.HttpException) {
                        val cuerpoError = try {
                            e.response()?.errorBody()?.string()
                        } catch (_: Exception) {
                            null
                        }

                        android.util.Log.e(
                            "ELIMINAR_ELEMENTO",
                            "HTTP ${e.code()}: $cuerpoError",
                            e
                        )

                        Toast.makeText(
                            this@PlaneadorSemanalActivity,
                            "Error HTTP ${e.code()}: " +
                                    (cuerpoError ?: "Error del servidor"),
                            Toast.LENGTH_LONG
                        ).show()

                    } catch (e: Exception) {
                        android.util.Log.e(
                            "ELIMINAR_ELEMENTO",
                            "Error al eliminar ${elemento.nombre}",
                            e
                        )

                        Toast.makeText(
                            this@PlaneadorSemanalActivity,
                            "No se pudo eliminar el elemento.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .show()
    }

    private fun mostrarDialogoBuscarCocinero(dia: DiaPlaneador) {

        // 1. Validar que el día esté seleccionado.
        if (!dia.seleccionado) {
            Toast.makeText(
                this,
                "Primero selecciona el día para cocinar.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // 2. Validar los identificadores necesarios.
        if (hogId <= 0 || hogUsuIdSolicitante <= 0) {
            Toast.makeText(
                this,
                "No se pudo identificar el hogar o el usuario.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // 3. Crear el contenido del diálogo.
        val contenedor = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 24, 48, 8)
        }

        val textoDia = android.widget.TextView(this).apply {
            text = "Selecciona quién cocinará el ${dia.nombreDia}"
            textSize = 16f
        }

        val campoBusqueda = android.widget.EditText(this).apply {
            hint = "Buscar integrante por nombre o correo"
            inputType = android.text.InputType.TYPE_CLASS_TEXT
        }

        val progreso = android.widget.ProgressBar(this).apply {
            visibility = android.view.View.GONE
            isIndeterminate = true
        }

        val textoEstado = android.widget.TextView(this).apply {
            text = "Escribe al menos dos caracteres para buscar."
            textSize = 14f
            setPadding(0, 16, 0, 16)
        }

        val listaIntegrantes = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
        }

        contenedor.addView(textoDia)
        contenedor.addView(campoBusqueda)
        contenedor.addView(progreso)
        contenedor.addView(textoEstado)
        contenedor.addView(listaIntegrantes)

        val scroll = android.widget.ScrollView(this).apply {
            addView(contenedor)
        }

        val dialogo = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Asignar cocinero")
            .setView(scroll)
            .setNegativeButton("Cerrar", null)
            .create()

        dialogo.show()

        // 4. Preparar la búsqueda con una pausa de 400 ms.
        val handler = android.os.Handler(
            android.os.Looper.getMainLooper()
        )

        val tareaBusqueda = object : Runnable {

            override fun run() {

                val termino = campoBusqueda.text
                    .toString()
                    .trim()

                if (termino.length < 2) {
                    progreso.visibility = android.view.View.GONE
                    listaIntegrantes.removeAllViews()

                    textoEstado.text =
                        "Escribe al menos dos caracteres para buscar."

                    textoEstado.visibility = android.view.View.VISIBLE
                    return
                }

                progreso.visibility = android.view.View.VISIBLE
                textoEstado.visibility = android.view.View.GONE
                listaIntegrantes.removeAllViews()

                lifecycleScope.launch {

                    try {

                        val respuesta = ApiClient.apiService
                            .buscarIntegrantesHogar(
                                BuscarIntegrantesHogarRequest(
                                    HOG_ID = hogId,
                                    HOG_USU_ID_SOLICITANTE =
                                        hogUsuIdSolicitante,
                                    busqueda = termino
                                )
                            )

                        // Ignorar resultados de búsquedas anteriores.
                        if (
                            campoBusqueda.text.toString().trim() != termino ||
                            !dialogo.isShowing
                        ) {
                            return@launch
                        }

                        listaIntegrantes.removeAllViews()

                        if (respuesta.success) {

                            val integrantes = respuesta.integrantes

                            if (integrantes.isEmpty()) {

                                textoEstado.text =
                                    "No se encontraron integrantes."

                                textoEstado.visibility =
                                    android.view.View.VISIBLE

                            } else {

                                textoEstado.visibility =
                                    android.view.View.GONE

                                integrantes.forEach { integrante ->

                                    val tarjeta =
                                        android.widget.LinearLayout(
                                            this@PlaneadorSemanalActivity
                                        ).apply {

                                            orientation =
                                                android.widget.LinearLayout.VERTICAL

                                            setPadding(16, 16, 16, 16)

                                            isClickable = true
                                            isFocusable = true

                                            val parametros =
                                                android.widget.LinearLayout.LayoutParams(
                                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                                                )

                                            parametros.setMargins(0, 4, 0, 4)
                                            layoutParams = parametros

                                            setBackgroundResource(
                                                android.R.drawable.list_selector_background
                                            )
                                        }

                                    val nombre =
                                        android.widget.TextView(
                                            this@PlaneadorSemanalActivity
                                        ).apply {
                                            text = integrante.HOG_USU_NOMBRE
                                            textSize = 16f
                                        }

                                    val correo =
                                        android.widget.TextView(
                                            this@PlaneadorSemanalActivity
                                        ).apply {
                                            text = integrante.HOG_USU_CORREO
                                                ?.takeIf { it.isNotBlank() }
                                                ?: "Sin correo registrado"

                                            textSize = 13f
                                        }

                                    val rol =
                                        android.widget.TextView(
                                            this@PlaneadorSemanalActivity
                                        ).apply {
                                            text = "Rol: ${integrante.HOG_USU_ROL}"
                                            textSize = 12f
                                        }

                                    tarjeta.addView(nombre)
                                    tarjeta.addView(correo)
                                    tarjeta.addView(rol)

                                    tarjeta.setOnClickListener {

                                        // Validar que pertenezca al hogar actual.
                                        if (integrante.HOG_ID != hogId) {
                                            Toast.makeText(
                                                this@PlaneadorSemanalActivity,
                                                "El integrante no pertenece a este hogar.",
                                                Toast.LENGTH_LONG
                                            ).show()

                                            return@setOnClickListener
                                        }

                                        // Confirmar antes de guardar.
                                        androidx.appcompat.app.AlertDialog.Builder(
                                            this@PlaneadorSemanalActivity
                                        )
                                            .setTitle("Confirmar cocinero")
                                            .setMessage(
                                                "¿Deseas asignar a " +
                                                        integrante.HOG_USU_NOMBRE +
                                                        " para cocinar el día " +
                                                        dia.nombreDia +
                                                        "?"
                                            )
                                            .setNegativeButton(
                                                "Cancelar",
                                                null
                                            )
                                            .setPositiveButton(
                                                "Asignar cocinero"
                                            ) { _, _ ->

                                                dialogo.dismiss()

                                                asignarCocineroAlDia(
                                                    dia = dia,
                                                    integrante = integrante
                                                )
                                            }
                                            .show()
                                    }

                                    listaIntegrantes.addView(tarjeta)
                                }
                            }

                        } else {

                            textoEstado.text =
                                respuesta.message.ifBlank {
                                    "No se pudo realizar la búsqueda."
                                }

                            textoEstado.visibility =
                                android.view.View.VISIBLE
                        }

                    } catch (e: kotlinx.coroutines.CancellationException) {

                        throw e

                    } catch (e: Exception) {

                        if (dialogo.isShowing) {
                            textoEstado.text =
                                "Error al buscar integrantes. Verifica tu conexión e inténtalo nuevamente."

                            textoEstado.visibility =
                                android.view.View.VISIBLE
                        }

                        android.util.Log.e(
                            "PlaneadorSemanal",
                            "Error al buscar integrantes del hogar",
                            e
                        )

                    } finally {

                        if (
                            dialogo.isShowing &&
                            campoBusqueda.text.toString().trim() == termino
                        ) {
                            progreso.visibility =
                                android.view.View.GONE
                        }
                    }
                }
            }
        }

        // 5. Detectar cambios en el campo de búsqueda.
        campoBusqueda.addTextChangedListener(
            object : android.text.TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) = Unit

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {

                    handler.removeCallbacks(tareaBusqueda)
                    listaIntegrantes.removeAllViews()

                    val termino = s?.toString()?.trim().orEmpty()

                    if (termino.length < 2) {

                        progreso.visibility = android.view.View.GONE

                        textoEstado.text =
                            "Escribe al menos dos caracteres para buscar."

                        textoEstado.visibility = android.view.View.VISIBLE
                        return
                    }

                    textoEstado.text = "Preparando búsqueda..."
                    textoEstado.visibility = android.view.View.VISIBLE

                    handler.postDelayed(tareaBusqueda, 400)
                }

                override fun afterTextChanged(
                    s: android.text.Editable?
                ) = Unit
            }
        )

        // 6. Limpiar la búsqueda al cerrar el diálogo.
        dialogo.setOnDismissListener {
            handler.removeCallbacks(tareaBusqueda)
        }
    }

     private fun asignarCocineroAlDia(
        dia: DiaPlaneador,
        integrante: IntegranteHogarBusqueda
    ) {
        // 1. Validar el identificador del día del planeador.
        if (dia.pldId <= 0) {
            Toast.makeText(
                this,
                "No se identificó el día del planeador. Verifica que tenga un PLD_ID válido.",
                Toast.LENGTH_LONG
            ).show()

            android.util.Log.e(
                "PlaneadorSemanal",
                "PLD_ID inválido: ${dia.pldId}"
            )
            return
        }

        // 2. Validar el hogar.
        if (hogId <= 0 || integrante.HOG_ID != hogId) {
            Toast.makeText(
                this,
                "El integrante no pertenece al hogar seleccionado.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // 3. Validar quién realiza la asignación.
        if (hogUsuIdSolicitante <= 0) {
            Toast.makeText(
                this,
                "No se pudo identificar al usuario que realiza la asignación.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // 4. Ejecutar la petición al servidor.
        lifecycleScope.launch {
            try {
                val request = AsignarCocineroRequest(
                    pldId = dia.pldId,
                    hogUsuIdCocinero = integrante.HOG_USU_ID,
                    hogUsuIdAsigno = hogUsuIdSolicitante
                )

                android.util.Log.d(
                    "PlaneadorSemanal",
                    "Asignando cocinero: " +
                            "PLD_ID=${request.pldId}, " +
                            "COCINERO=${request.hogUsuIdCocinero}, " +
                            "ASIGNO=${request.hogUsuIdAsigno}"
                )

                val respuesta = ApiClient.apiService.asignarCocinero(request)

                if (respuesta.success) {
                    // Actualizar el día con el cocinero asignado.
                    dia.hogUsuIdCocinero = integrante.HOG_USU_ID
                    dia.nombreCocinero = integrante.HOG_USU_NOMBRE
                    Toast.makeText(
                        this@PlaneadorSemanalActivity,
                        "${integrante.HOG_USU_NOMBRE} fue asignado para cocinar el ${dia.nombreDia}.",
                        Toast.LENGTH_LONG
                    ).show()
                    android.util.Log.d(
                        "PlaneadorSemanal",
                        "Asignación guardada. PCC_ID=${respuesta.pccId}"
                    )

                    // Actualizar los botones del adaptador.
                    adapterDia.actualizarDias(adapterDia.obtenerDias())

                    // Recargar los datos desde el servidor.
                    obtenerPlaneadorSemanal()

                } else {

                    Toast.makeText(
                        this@PlaneadorSemanalActivity,
                        respuesta.message.ifBlank {
                            "No se pudo guardar la asignación del cocinero."
                        },
                        Toast.LENGTH_LONG
                    ).show()

                    android.util.Log.e(
                        "PlaneadorSemanal",
                        "La API rechazó la asignación: ${respuesta.message}"
                    )
                }

            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e

            } catch (e: Exception) {

                android.util.Log.e(
                    "PlaneadorSemanal",
                    "Error al asignar cocinero",
                    e
                )

                Toast.makeText(
                    this@PlaneadorSemanalActivity,
                    "Ocurrió un error al guardar la asignación. Verifica tu conexión.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }



}