package com.example.gourmeet2

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.DatePickerDialog
import androidx.lifecycle.lifecycleScope
import com.example.gourmeet2.data.api.ApiClient
import com.example.gourmeet2.data.models.Hogar
import com.example.gourmeet2.data.models.IconoHogar
import com.example.gourmeet2.databinding.ActivityMiHogarBinding
import com.example.gourmeet2.utils.SesionUsuario
import kotlinx.coroutines.launch
import com.example.gourmeet2.databinding.ItemAgregarHogarBinding
import kotlin.collections.toBooleanArray
import com.bumptech.glide.Glide
import com.example.gourmeet2.GestorCaducidad.calcularFechaCaducidadLacteos
import com.example.gourmeet2.data.models.BuscarIngredientes
import com.example.gourmeet2.data.models.UsuarioBusqueda
import com.example.gourmeet2.databinding.DialogAgregarIngredienteBinding
import com.example.gourmeet2.ui.adapters.IngredienteMiniAdapter
import java.lang.String.format
import java.util.Calendar
import java.util.Locale

class MiHogarActivity : AppCompatActivity() {
    private var cantidadNinos = 0
    private var cantidadAdultos = 0
    private var cantidadAdultosMayores = 0
    private var ingredienteSeleccionadoId: Int? = null
    private lateinit var binding: ActivityMiHogarBinding
    // Guardaremos aquí el hogar que pertenece al usuario
    private lateinit var usuariosBusquedaAdapter: UsuariosBusquedaAdapter
    private var busquedaRunnable: Runnable? = null
    private val handler = android.os.Handler(
        android.os.Looper.getMainLooper()
    )
    private var familiaIngredienteSeleccionado: String? = null
    private lateinit var miembrosHogarAdapter: MiembrosHogarAdapter
    private val miembrosPendientes =
        mutableListOf<UsuarioBusqueda>()
    private var hogarActual: Hogar? = null
    private var dialogAgregarHogar: Dialog? = null
    private var dialogAgregarHogarBinding: ItemAgregarHogarBinding? = null
    private val equiposSeleccionados = mutableSetOf<Int>()
    private val equiposCocina = arrayOf(
        "Freidora de aire",
        "Horno",
        "Olla express",
        "Batidora",
        "Microondas",
        "Sartenes",
        "Ollas",
        "Licuadora",
        "Refrigerador",
        "Tostador",
        "Extractor",
        "Estufa",
        "Parrilla eléctrica"
    )
    private val mapaLauncher =
        registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
        ) { resultado ->

            if (resultado.resultCode == RESULT_OK) {

                val datos = resultado.data

                val direccion =
                    datos?.getStringExtra("direccion")

                val latitud =
                    datos?.getDoubleExtra("lat", 0.0)

                val longitud =
                    datos?.getDoubleExtra("lng", 0.0)

                if (!direccion.isNullOrEmpty()) {

                    dialogAgregarHogarBinding
                        ?.editUbicacion
                        ?.setText(direccion)

                    // Guardaremos estos datos después
                    // cuando conectemos la creación del hogar.

                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMiHogarBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnAgregarHogar.setOnClickListener {
            mostrarVentanaAgregarHogar()
        }


        cargarHogar()
    }
    private fun cargarHogar() {

        val cliId = obtenerCliId()

        if (cliId == null) {

            Toast.makeText(
                this,
                "No se pudo obtener el usuario",
                Toast.LENGTH_SHORT
            ).show()

            mostrarSinHogar()
            return
        }

        lifecycleScope.launch {

            try {

                val respuesta =
                    ApiClient.apiService.obtenerHogarUsuario(cliId)

                if (!respuesta.success) {

                    Toast.makeText(
                        this@MiHogarActivity,
                        respuesta.mensaje,
                        Toast.LENGTH_SHORT
                    ).show()

                    return@launch
                }

                if (respuesta.tiene_hogar && respuesta.hogar != null) {

                    // Guardamos el hogar actual
                    hogarActual = respuesta.hogar

                    mostrarConHogar()

                } else {

                    mostrarSinHogar()
                }

            } catch (e: Exception) {

                e.printStackTrace()

                Toast.makeText(
                    this@MiHogarActivity,
                    "Error al consultar el hogar",
                    Toast.LENGTH_SHORT
                ).show()

                mostrarSinHogar()
            }
        }
    }
    private fun mostrarSinHogar() {

        binding.sincuenta.visibility = View.VISIBLE
        binding.vista2.visibility = View.GONE
    }
    private fun mostrarConHogar() {

        binding.sincuenta.visibility = View.GONE
        binding.vista2.visibility = View.VISIBLE
    }
    private fun obtenerCliId(): Int? {

        val id = SesionUsuario.obtenerId(this)

        return if (id > 0) {
            id
        } else {
            null
        }
    }
    private fun mostrarVentanaAgregarHogar() {

        val dialog = Dialog(this)

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val dialogBinding =
            ItemAgregarHogarBinding.inflate(layoutInflater)

        dialog.setContentView(dialogBinding.root)

        dialog.setCancelable(true)

        // Guardamos referencias
        dialogAgregarHogar = dialog
        dialogAgregarHogarBinding = dialogBinding

        val window = dialog.window

        if (window != null) {

            window.setBackgroundDrawable(
                ColorDrawable(Color.TRANSPARENT)
            )

            window.setDimAmount(0.55f)

            window.addFlags(
                WindowManager.LayoutParams.FLAG_DIM_BEHIND
            )

            window.setGravity(Gravity.BOTTOM)
        }

        // Configuramos la ubicación
        configurarSeleccionUbicacion(dialogBinding)
        configurarSelectorIconos(dialogBinding, dialog)
        configurarEquipamiento(dialogBinding)
        configurarBusquedaUsuarios(dialogBinding)
        configurarContadoresPersonas(dialogBinding)
        configurarMiembrosHogar(dialogBinding)
        configurarAlacenaHogar(dialogBinding)

        dialog.show()

        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        dialog.window?.setGravity(Gravity.BOTTOM)
    }
    private fun configurarSeleccionUbicacion(
        dialogBinding: ItemAgregarHogarBinding
    ) {

        dialogBinding.editUbicacion.setOnClickListener {

            val intent = Intent(
                this,
                MapaSeleccionActivity::class.java
            )

            mapaLauncher.launch(intent)
        }
    }
    private fun configurarSelectorIconos(
        dialogBinding: ItemAgregarHogarBinding,
        dialog: Dialog
    ) {

        // Inicialmente oculto
        dialogBinding.selecciondeicono.visibility =
            View.GONE

        // RecyclerView horizontal
        dialogBinding.selecciondeicono.layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(
                this,
                androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL,
                false
            )

        // Adapter
        dialogBinding.selecciondeicono.adapter =
            IconosHogarAdapter(iconosHogar) { iconoSeleccionado ->

                // Cambiar imagen principal
                dialogBinding.seleccionarimagen.setImageResource(
                    iconoSeleccionado.recurso
                )

                // Ocultar selector
                dialogBinding.selecciondeicono.visibility =
                    View.GONE
            }

        // Abrir selector
        dialogBinding.seleccionarimagen.setOnClickListener {

            dialogBinding.selecciondeicono.visibility =
                View.VISIBLE
        }
    }
    private fun configurarEquipamiento(
        dialogBinding: ItemAgregarHogarBinding
    ) {

        dialogBinding.editEquipamiento.setOnClickListener {

            mostrarChecklistEquipamiento(dialogBinding)
        }
    }
    private val iconosHogar = listOf(

        IconoHogar(
            "Casa",
            R.drawable.ic_casa_azul
        ),

        IconoHogar(
            "Casa 2",
            R.drawable.ic_casa1
        ),

        IconoHogar(
            "Departamento",
            R.drawable.ic_casa2
        ),

        IconoHogar(
            "Edificio",
            R.drawable.ic_casa_azul
        )
    )
    private fun configurarBusquedaUsuarios(
        dialogBinding: ItemAgregarHogarBinding
    ) {

        usuariosBusquedaAdapter =
            UsuariosBusquedaAdapter(emptyList()) { usuario ->

                agregarMiembroTemporal(usuario)
            }

        dialogBinding.recyclerUsuariosBusqueda.layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(this)

        dialogBinding.recyclerUsuariosBusqueda.adapter =
            usuariosBusquedaAdapter

        dialogBinding.txtNombreusuariobuscar.addTextChangedListener(
            object : android.text.TextWatcher {

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
                }

                override fun afterTextChanged(
                    s: android.text.Editable?
                ) {

                    val texto = s?.toString()?.trim() ?: ""

                    busquedaRunnable?.let {
                        handler.removeCallbacks(it)
                    }

                    if (texto.length < 2) {

                        dialogBinding.recyclerUsuariosBusqueda.visibility =
                            View.GONE

                        return
                    }

                    busquedaRunnable = Runnable {

                        buscarUsuarios(
                            texto,
                            dialogBinding
                        )
                    }

                    handler.postDelayed(
                        busquedaRunnable!!,
                        500
                    )
                }
            }
        )
    }
    private fun buscarUsuarios(
        texto: String,
        dialogBinding: ItemAgregarHogarBinding
    ) {

        val cliId = obtenerCliId()

        if (cliId == null) {
            return
        }

        lifecycleScope.launch {

            try {

                val respuesta =
                    ApiClient.apiService.buscarUsuariosHogar(
                        texto,
                        cliId
                    )

                if (!respuesta.success) {

                    dialogBinding.recyclerUsuariosBusqueda.visibility =
                        View.GONE

                    return@launch
                }

                usuariosBusquedaAdapter.actualizarUsuarios(
                    respuesta.usuarios
                )

                if (respuesta.usuarios.isNotEmpty()) {

                    dialogBinding.recyclerUsuariosBusqueda.visibility =
                        View.VISIBLE

                } else {

                    dialogBinding.recyclerUsuariosBusqueda.visibility =
                        View.GONE
                }

            } catch (e: Exception) {

                e.printStackTrace()

                dialogBinding.recyclerUsuariosBusqueda.visibility =
                    View.GONE

                Toast.makeText(
                    this@MiHogarActivity,
                    "Error al buscar usuarios",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    private fun mostrarChecklistEquipamiento(
        dialogBinding: ItemAgregarHogarBinding
    ) {

        val seleccionados = BooleanArray(equiposCocina.size) { index ->
            equiposSeleccionados.contains(index)
        }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Equipo básico de cocina")
            .setMultiChoiceItems(
                equiposCocina,
                seleccionados
            ) { _, which, isChecked ->

                if (isChecked) {
                    equiposSeleccionados.add(which)
                } else {
                    equiposSeleccionados.remove(which)
                }
            }
            .setNegativeButton("CANCELAR", null)
            .setPositiveButton("LISTO") { _, _ ->

                val textoSeleccionado =
                    equiposSeleccionados
                        .sorted()
                        .map { equiposCocina[it] }
                        .joinToString(", ")

                dialogBinding.editEquipamiento.setText(
                    "Equipo seleccionado"
                )
            }
            .create()

        // Mostrar primero el diálogo
        dialog.show()

        // --------------------------------
        // COLOR DEL BOTÓN LISTO
        // --------------------------------

        dialog.getButton(
            androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE
        ).setTextColor(
            androidx.core.content.ContextCompat.getColor(
                this,
                R.color.azulgourmeet
            )
        )

        // --------------------------------
        // COLOR DEL BOTÓN CANCELAR
        // --------------------------------

        dialog.getButton(
            androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE
        ).setTextColor(
            androidx.core.content.ContextCompat.getColor(
                this,
                R.color.azulgourmeet
            )
        )

        // --------------------------------
        // COLOR DE LAS PALOMITAS
        // --------------------------------

        val listView = dialog.listView

        for (i in 0 until listView.childCount) {

            val view = listView.getChildAt(i)

            if (view is android.widget.CheckedTextView) {

                view.checkMarkTintList =
                    android.content.res.ColorStateList.valueOf(
                        androidx.core.content.ContextCompat.getColor(
                            this,
                            R.color.azulgourmeet
                        )
                    )
            }
        }
    }
    private fun configurarContadoresPersonas(
        binding: ItemAgregarHogarBinding
    ) {

        // NIÑOS
        binding.btnMasNinos.setOnClickListener {
            cantidadNinos++
            binding.txtCantidadNinos.text = cantidadNinos.toString()
        }

        binding.btnMenosNinos.setOnClickListener {
            if (cantidadNinos > 0) {
                cantidadNinos--
                binding.txtCantidadNinos.text = cantidadNinos.toString()
            }
        }


        // ADULTOS
        binding.btnMasAdultos.setOnClickListener {
            cantidadAdultos++
            binding.txtCantidadAdultos.text = cantidadAdultos.toString()
        }

        binding.btnMenosAdultos.setOnClickListener {
            if (cantidadAdultos > 0) {
                cantidadAdultos--
                binding.txtCantidadAdultos.text = cantidadAdultos.toString()
            }
        }


        // ADULTOS MAYORES
        binding.btnMasAdultosMayores.setOnClickListener {
            cantidadAdultosMayores++
            binding.txtCantidadAdultosMayores.text =
                cantidadAdultosMayores.toString()
        }

        binding.btnMenosAdultosMayores.setOnClickListener {
            if (cantidadAdultosMayores > 0) {
                cantidadAdultosMayores--
                binding.txtCantidadAdultosMayores.text =
                    cantidadAdultosMayores.toString()
            }
        }
    }
    private fun agregarMiembroTemporal(
        usuario: UsuarioBusqueda
    ) {

        if (miembrosPendientes.any {
                it.CLI_ID == usuario.CLI_ID
            }
        ) {

            Toast.makeText(
                this,
                "Este usuario ya fue agregado",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        miembrosPendientes.add(usuario)

        miembrosHogarAdapter.actualizarMiembros(
            miembrosPendientes
        )

        dialogAgregarHogarBinding
            ?.miembrosdelhogar
            ?.visibility = View.VISIBLE

        Toast.makeText(
            this,
            "${usuario.CLI_NOMBRE} agregado al hogar",
            Toast.LENGTH_SHORT
        ).show()
    }
    private fun configurarMiembrosHogar(
        dialogBinding: ItemAgregarHogarBinding
    ) {

        miembrosHogarAdapter =
            MiembrosHogarAdapter(
                miembrosPendientes.toList()
            ) { usuario ->

                miembrosPendientes.removeAll {
                    it.CLI_ID == usuario.CLI_ID
                }

                miembrosHogarAdapter.actualizarMiembros(
                    miembrosPendientes
                )

                if (miembrosPendientes.isEmpty()) {

                    dialogBinding.miembrosdelhogar.visibility =
                        View.GONE
                }
            }

        dialogBinding.miembrosdelhogar.layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(
                this,
                androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL,
                false
            )

        dialogBinding.miembrosdelhogar.adapter =
            miembrosHogarAdapter

        dialogBinding.miembrosdelhogar.visibility =
            if (miembrosPendientes.isEmpty()) {
                View.GONE
            } else {
                View.VISIBLE
            }
    }

    private fun actualizarMiembrosTemporales() {

        miembrosHogarAdapter.actualizarMiembros(
            miembrosPendientes
        )

        val binding =
            dialogAgregarHogarBinding ?: return

        if (miembrosPendientes.isEmpty()) {

            binding.miembrosdelhogar.visibility =
                View.GONE

        } else {

            binding.miembrosdelhogar.visibility =
                View.VISIBLE
        }
    }
    private fun invitarUsuarioAlHogar(
        usuario: UsuarioBusqueda
    ) {

        val cliIdPropietario = obtenerCliId()

        val hogar = hogarActual

        if (cliIdPropietario == null) {

            Toast.makeText(
                this,
                "No se pudo identificar al usuario",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (hogar == null) {

            Toast.makeText(
                this,
                "No se encontró el hogar",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        lifecycleScope.launch {

            try {

                val respuesta =
                    ApiClient.apiService.invitarUsuarioHogar(
                        hogar.HOG_ID,
                        cliIdPropietario,
                        usuario.CLI_ID
                    )

                Toast.makeText(
                    this@MiHogarActivity,
                    respuesta.mensaje,
                    Toast.LENGTH_SHORT
                ).show()

                if (respuesta.success) {

                    // Por ahora solamente confirmamos
                    // que la invitación fue enviada.

                }

            } catch (e: Exception) {

                e.printStackTrace()

                Toast.makeText(
                    this@MiHogarActivity,
                    "Error al enviar la invitación",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun configurarAlacenaHogar(
        dialogBinding: ItemAgregarHogarBinding
    ) {

        dialogBinding.btnAgregarIngredientehogar.setOnClickListener {

            mostrarDialogAgregarIngrediente(
                dialogBinding
            )
        }
    }
    private fun mostrarDialogAgregarIngrediente(
        hogarBinding: ItemAgregarHogarBinding
    ) {

        val dialog = Dialog(this)

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val ingredienteBinding =
            DialogAgregarIngredienteBinding.inflate(
                layoutInflater
            )

        dialog.setContentView(
            ingredienteBinding.root
        )
        // ==========================================
// CONTROLES DE FECHAS Y CONFIGURACIÓN
// ==========================================

        val txtFechaCompra =
            ingredienteBinding.txtFechadecompra

        val txtFechaConsumo =
            ingredienteBinding.txtFechadecon

        val txtTipoEstado =
            ingredienteBinding.txtTipodeestado

        val txtTipoAlmacenamiento =
            ingredienteBinding.txtTipodealmacenamiento
        configurarBusquedaIngrediente(ingredienteBinding)

        dialog.setCancelable(true)

        val window = dialog.window

        if (window != null) {

            window.setBackgroundDrawable(
                ColorDrawable(Color.TRANSPARENT)
            )

            window.setDimAmount(0.55f)

            window.addFlags(
                WindowManager.LayoutParams.FLAG_DIM_BEHIND
            )

            window.setGravity(
                Gravity.BOTTOM
            )
        }

        dialog.show()

        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        dialog.window?.setGravity(
            Gravity.BOTTOM
        )

        // BOTÓN CERRAR
        ingredienteBinding.btnCerrarIngrediente.setOnClickListener {
            dialog.dismiss()
        }
        ingredienteBinding.txtFechadecompra.setOnClickListener {

            val calendario = Calendar.getInstance()

            val anio = calendario.get(Calendar.YEAR)
            val mes = calendario.get(Calendar.MONTH)
            val dia = calendario.get(Calendar.DAY_OF_MONTH)

            android.app.DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->

                    val fechaSeleccionada = format(
                        Locale.getDefault(),
                        "%02d/%02d/%04d",
                        dayOfMonth,
                        month + 1,
                        year
                    )

                    ingredienteBinding.txtFechadecompra.text =
                        fechaSeleccionada

                    // Por ahora solamente mostramos la fecha.
                    // El cálculo de caducidad lo conectaremos
                    // en el siguiente paso.

                },
                anio,
                mes,
                dia
            ).show()
        }
    }
    private fun configurarBusquedaIngrediente(
        binding: DialogAgregarIngredienteBinding
    ) {

        val rvResultadosIngrediente =
            binding.rvResultadosIngrediente

        val contenedorResultados =
            binding.contenedorResultadosIngrediente

        val edtIngrediente =
            binding.edtIngrediente

        val txtEmojiIngrediente =
            binding.txtEmojiIngrediente

        rvResultadosIngrediente.layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(
                this,
                androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL,
                false
            )

        var ingredienteSeleccionadoManualmente = false

        val adapterIngredientes =
            IngredienteMiniAdapter(
                emptyList()
            ) { ingredienteSeleccionado ->

                ingredienteSeleccionadoManualmente = true

                // Nombre
                edtIngrediente.setText(
                    ingredienteSeleccionado.nombre
                )

                // ID
                ingredienteSeleccionadoId =
                    ingredienteSeleccionado.id
                familiaIngredienteSeleccionado =
                    ingredienteSeleccionado.categoria

                // Imagen
                if (!ingredienteSeleccionado.imagen_url.isNullOrEmpty()) {

                    Glide.with(this)
                        .load(ingredienteSeleccionado.imagen_url)
                        .placeholder(R.drawable.ic_ingredientes)
                        .error(R.drawable.ic_ingredientes)
                        .into(txtEmojiIngrediente)

                } else {

                    txtEmojiIngrediente.setImageResource(
                        R.drawable.ic_ingredientes
                    )
                }

                // Ocultar resultados
                contenedorResultados.visibility =
                    View.GONE
            }

        rvResultadosIngrediente.adapter =
            adapterIngredientes

        // ==========================================
        // BUSCADOR
        // ==========================================

        edtIngrediente.addTextChangedListener(

            object : android.text.TextWatcher {

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

                    // Si acabamos de seleccionar un ingrediente
                    if (ingredienteSeleccionadoManualmente) {

                        ingredienteSeleccionadoManualmente = false

                        return
                    }

                    val busqueda =
                        s?.toString()
                            ?.trim()
                            .orEmpty()

                    // Campo vacío
                    if (busqueda.isEmpty()) {

                        contenedorResultados.visibility =
                            View.GONE

                        adapterIngredientes.actualizarLista(
                            emptyList()
                        )

                        ingredienteSeleccionadoId = null
                        familiaIngredienteSeleccionado=null

                        return
                    }

                    // Buscar
                    buscarIngredientesHogar(
                        busqueda,
                        adapterIngredientes,
                        contenedorResultados
                    )
                }

                override fun afterTextChanged(
                    s: android.text.Editable?
                ) {
                }
            }
        )
    }
    private fun buscarIngredientesHogar(
        busqueda: String,
        adapter: IngredienteMiniAdapter,
        contenedorResultados: View
    ) {

        lifecycleScope.launch {

            try {

                android.util.Log.d(
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

                    android.util.Log.d(
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

                android.util.Log.e(
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

}