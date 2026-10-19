
package mx.com.gourmeet.app

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mx.com.gourmeet.app.data.api.ApiClient
import mx.com.gourmeet.app.data.models.ActualizarEstadoDiaRequest
import mx.com.gourmeet.app.data.models.ActualizarHoraEspacioRequest
import mx.com.gourmeet.app.data.models.DiaPlaneador
import mx.com.gourmeet.app.data.models.DiaRepeticion
import mx.com.gourmeet.app.data.models.EliminarRecetaEspacioRequest
import mx.com.gourmeet.app.data.models.EspacioComida
import mx.com.gourmeet.app.data.models.FiltrosRecetasNombreRequest
import mx.com.gourmeet.app.data.models.GuardarRecetaPlaneadorRequest
import mx.com.gourmeet.app.data.models.PlaneadorRequest
import mx.com.gourmeet.app.data.models.RecetaEspacio
import mx.com.gourmeet.app.data.models.RecetaconFiltro
import mx.com.gourmeet.app.databinding.ItemPlaneadorSemanalBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PlaneadorSemanalActivity : AppCompatActivity() {

    private var porcionesSeleccionadas: Int? = null

    private var fechasComidaRepetida: Set<String> = emptySet()


    // Adaptador para seleccionar días en los que repetir una receta
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

    private val recetasPendientesPlaneador =
        mutableListOf<RecetaPendientePlaneador>()


    private val recetasSeleccionadas = mutableListOf<RecetaconFiltro>()
    private var recetaSeleccionada: RecetaconFiltro? = null

    private var busquedaRecetasJob: Job? = null

    private val formatoFecha =
        SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private val formatoDia =
        SimpleDateFormat("EEEE d", Locale("es", "MX"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ItemPlaneadorSemanalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cliId = intent.getIntExtra("CLI_ID", -1)
        hogId = intent.getIntExtra("HOG_ID", -1)

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

            onAgregarColacion = { dia ->
                if (dia.seleccionado) {
                    mostrarVentanaAgregarComidas(
                        dia = dia,
                        tipoComida = 4
                    )
                }
            },

            onAsignarHora = { espacio ->
                mostrarSelectorHora(espacio)
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

                // 2. Obtener los espacios y las fechas
                val espacios = response.espacios.orEmpty()
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

                // 3. Generar los días de la semana
                val dias = generarDias(
                    fechaInicio = fechaInicio,
                    fechaFin = fechaFin,
                    espacios = espacios
                )

                // 4. Actualizar el RecyclerView
                adapterDia.actualizarDias(dias)

                // 5. Actualizar el resumen de recetas y días seleccionados
                actualizarResumenDiasSeleccionados()

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Toast.makeText(
                    this@PlaneadorSemanalActivity,
                    "Error al cargar el planeador: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun generarDias(
        fechaInicio: String,
        fechaFin: String,
        espacios: List<EspacioComida>
    ): List<DiaPlaneador> {

        val inicio = formatoFecha.parse(fechaInicio)
            ?: return emptyList()

        val fin = formatoFecha.parse(fechaFin)
            ?: return emptyList()

        val espaciosPorFecha = espacios.groupBy {
            it.ECM_FECHA.take(10)
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

            val estadoCocinar =
                espaciosDelDia.firstOrNull()?.PLD_COCINAR ?: 1

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
                    seleccionado = estadoCocinar == 1
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

    private fun configurarBusquedaRecetas(
        vista: View,
        categoriaId: Int
    ) {
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

}