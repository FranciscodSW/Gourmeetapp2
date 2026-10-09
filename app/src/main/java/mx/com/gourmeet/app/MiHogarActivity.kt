package mx.com.gourmeet.app

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckedTextView
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ListView
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import android.widget.Toast.makeText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import mx.com.gourmeet.app.data.api.ApiClient
import mx.com.gourmeet.app.data.models.*
import mx.com.gourmeet.app.databinding.ActivityMiHogarBinding
import mx.com.gourmeet.app.utils.SesionUsuario
import kotlinx.coroutines.launch
import mx.com.gourmeet.app.databinding.ItemAgregarHogarBinding
import com.bumptech.glide.Glide
import mx.com.gourmeet.app.ui.adapters.IngredienteMiniAdapter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.toString

class MiHogarActivity : AppCompatActivity() {
    // Usuarios que estamos seleccionando para INVITAR
    private val miembrosPendientes =
        mutableListOf<UsuarioBusqueda>()

    // Miembros que YA existen en el hogar
    private val miembrosHogar =
        mutableListOf<MiembroHogar>()
    private lateinit var adapterMiembrosPendientes:
            MiembrosPendientesAdapter
    private var cantidadNinos = 0
    private val estadosHogar = mutableListOf<String>()
    private var guardandoHogar = false
    private lateinit var adapterMiembrosHogar: MiembrosHogarAdapter
    private var iconoHogarSeleccionado: String = "CASA"
    private var imagenIngredienteSeleccionada: String? = null
    private lateinit var adapterCategoriasIngredientes: CategoriaIngredientesAdapter
    private val ingredientesTemporales =
        mutableListOf<IngredienteAlacena>()
    private lateinit var adapterIngredientesTemporales:
            IngredienteAlacenaAdapter
    private lateinit var misIngredientes:
            RecyclerView
    private var cliIdActual: Int = 0
    private var latitudHogar: Double? = null
    private var longitudHogar: Double? = null
    private var direccionHogar: String? = null
    private enum class ModoIngrediente {
        AGREGAR,
        EDITAR,
        USAR_EN_RECETA
    }
    private var alacenaSeleccionada: Alacena? = null
    private val listaAlacenas = mutableListOf<Alacena>()

    private val tiposAlmacenamientoHogar =
        mutableListOf<String>()
    private var adapterEstadosHogar:
            ArrayAdapter<String>? = null
    private var adapterAlmacenamientoHogar:
            ArrayAdapter<String>? = null
    private var popupEstadoHogar:
            PopupWindow? = null
    private var popupAlmacenamientoHogar:
            PopupWindow? = null
    private var cantidadAdultos = 0
    private lateinit var adapterConsumePrimero: IngredienteAlacenaAdapter
    private var cantidadAdultosMayores = 0
    private var ingredienteSeleccionadoId: Int? = null
    private lateinit var binding: ActivityMiHogarBinding
    // Guardaremos aquí el hogar que pertenece al usuario
    private lateinit var usuariosBusquedaAdapter: UsuariosBusquedaAdapter
    private var busquedaRunnable: Runnable? = null
    private val handler = Handler(Looper.getMainLooper())
    private var familiaIngredienteSeleccionado: String? = null
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
            ActivityResultContracts.StartActivityForResult()
        ) { resultado ->
            if (resultado.resultCode == RESULT_OK) {

                val datos = resultado.data

                direccionHogar =
                    datos?.getStringExtra("direccion")

                latitudHogar =
                    datos?.getDoubleExtra("lat", 0.0)

                longitudHogar =
                    datos?.getDoubleExtra("lng", 0.0)

                if (!direccionHogar.isNullOrEmpty()) {

                    dialogAgregarHogarBinding
                        ?.editUbicacion
                        ?.setText(direccionHogar)

                    // Guardaremos estos datos después
                    // cuando conectemos la creación del hogar.

                }
            }
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMiHogarBinding.inflate(layoutInflater)
        setContentView(binding.root)
        configurarListaMiembrosHogar()
        binding.btnAgregarHogar.setOnClickListener {
            mostrarVentanaAgregarHogar()
        }
        binding.organizadespensa.setOnClickListener {

            val hogar = hogarActual

            if (hogar == null) {

                makeText(
                    this,
                    "No se encontró el hogar.",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val intent = Intent(
                this,
                AlacenaActivity::class.java
            )

            intent.putExtra(
                "HOG_ID",
                hogar.HOG_ID
            )

            startActivity(intent)
        }
        binding.masUsuarios.setOnClickListener {
            mostrarDialogNuevosUsuarios()
        }

    cargarHogar()
    }
    private fun cargarHogar() {

        val cliId = obtenerCliId()

        if (cliId == null) {

            makeText(
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

                    cargarMiembrosHogar(
                        respuesta.hogar.HOG_ID
                    )

                } else {

                    mostrarSinHogar()
                }

            } catch (e: Exception) {

                e.printStackTrace()

                makeText(
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
        dialogBinding.guardarHogar.setOnClickListener {
            guardarHogar()
        }
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
            LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL,
                false
            )

        // Adapter
        dialogBinding.selecciondeicono.adapter =
            IconosHogarAdapter(iconosHogar) { iconoSeleccionado ->

                dialogBinding.seleccionarimagen.setImageResource(
                    iconoSeleccionado.recurso
                )

                iconoHogarSeleccionado =
                    iconoSeleccionado.nombre

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
            LinearLayoutManager(this)

        dialogBinding.recyclerUsuariosBusqueda.adapter =
            usuariosBusquedaAdapter

        dialogBinding.txtNombreusuariobuscar.addTextChangedListener(
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
                }

                override fun afterTextChanged(
                    s: Editable?
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

                makeText(
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

        val dialog = AlertDialog.Builder(this)
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
            AlertDialog.BUTTON_POSITIVE
        ).setTextColor(
            ContextCompat.getColor(
                this,
                R.color.azulgourmeet
            )
        )

        // --------------------------------
        // COLOR DEL BOTÓN CANCELAR
        // --------------------------------

        dialog.getButton(
            AlertDialog.BUTTON_NEGATIVE
        ).setTextColor(
            ContextCompat.getColor(
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

            if (view is CheckedTextView) {

                view.checkMarkTintList =
                    ColorStateList.valueOf(
                        ContextCompat.getColor(
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

            makeText(
                this,
                "Este usuario ya fue agregado",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        miembrosPendientes.add(usuario)

        adapterMiembrosPendientes.actualizarMiembros(
            miembrosPendientes
        )

        dialogAgregarHogarBinding
            ?.miembrosdelhogar
            ?.visibility = View.VISIBLE

        makeText(
            this,
            "${usuario.CLI_NOMBRE} agregado al hogar",
            Toast.LENGTH_SHORT
        ).show()
    }
    private fun configurarMiembrosHogar(
        dialogBinding: ItemAgregarHogarBinding
    ) {

        adapterMiembrosPendientes =
            MiembrosPendientesAdapter(
                miembrosPendientes.toList()
            ) { usuario ->

                miembrosPendientes.removeAll {
                    it.CLI_ID == usuario.CLI_ID
                }

                adapterMiembrosPendientes.actualizarMiembros(
                    miembrosPendientes
                )

                if (miembrosPendientes.isEmpty()) {

                    dialogBinding.miembrosdelhogar.visibility =
                        View.GONE

                }
            }

        dialogBinding.miembrosdelhogar.layoutManager =
            LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL,
                false
            )

        dialogBinding.miembrosdelhogar.adapter =
            adapterMiembrosPendientes

        dialogBinding.miembrosdelhogar.visibility =
            if (miembrosPendientes.isEmpty()) {
                View.GONE
            } else {
                View.VISIBLE
            }
    }

    private fun invitarUsuarioAlHogar(
        usuario: UsuarioBusqueda
    ) {

        val cliIdPropietario = obtenerCliId()

        val hogar = hogarActual

        if (cliIdPropietario == null) {

            makeText(
                this,
                "No se pudo identificar al usuario",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (hogar == null) {

            makeText(
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

                makeText(
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

        misIngredientes = dialogBinding.misingredientes

        misIngredientes.layoutManager =
            LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL,
                false
            )

        adapterIngredientesTemporales =
            IngredienteAlacenaAdapter(
                ingredientesTemporales
            ) { ingrediente ->

                mostrarDialogAgregarIngrediente(
                    ingredienteEditar = ingrediente
                )
            }

        misIngredientes.adapter =
            adapterIngredientesTemporales

        dialogBinding.btnAgregarIngredientehogar.setOnClickListener {

            mostrarDialogAgregarIngrediente()
        }
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


            agregarIngredienteTemporal(
                ingredienteDescripcion = ingrediente,
                fotoIngrediente = imagenIngredienteSeleccionada,
                categoria = familiaIngredienteSeleccionado,

                cantidadDouble = cantidadDouble,

                unidad = unidad,

                fechaCompra =
                    convertirFechaMySQL(
                        txtFechaCompra.text.toString()
                    ),

                fechaVencimiento =
                    convertirFechaMySQL(
                        txtFechaConsumo.text.toString()
                    ),

                estado =
                     txtTipoEstado.text
                        .toString()
                        .trim()
                        .ifEmpty { null },

                precioDouble = precioDouble,

                almacenamiento =
                    txtTipoAlmacenamiento.text
                        .toString()
                        .trim()
                        .ifEmpty { null },

                frecuenciaConsumo =
                   txtTipoFrecuencia.text
                        .toString()
                        .trim()
                        .ifEmpty { null },

                tipoAbastecimiento =
                    txtTipoAbastecimiento.text
                        .toString()
                        .trim()
                        .ifEmpty { null }
            )
            bottomSheet.dismiss()

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

            else -> {}
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
                imagenIngredienteSeleccionada =
                    ingredienteSeleccionado.imagen_url

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
    private fun agregarIngredienteTemporal(
        ingredienteDescripcion: String,
        fotoIngrediente: String?,
        categoria: String?,
        cantidadDouble: Double,
        unidad: String,
        fechaCompra: String?,
        fechaVencimiento: String?,
        estado: String?,
        precioDouble: Double?,
        almacenamiento: String?,
        frecuenciaConsumo: String?,
        tipoAbastecimiento: String?
    ) {

        val ingredienteTemporal = IngredienteAlacena(

            ALC_ID = 0,

            ING_ID = ingredienteSeleccionadoId!!,

            ING_DESCRIPCION = ingredienteDescripcion,

            Foto_Ingrediente = fotoIngrediente,

            AI_CANTIDAD = cantidadDouble,

            AI_UNIDAD = unidad,

            AI_FECHA_COMPRA = fechaCompra,

            AI_FECHA_VENCIMIENTO = fechaVencimiento,

            AI_ESTADO = estado,

            AI_PRECIO_COMPRA = precioDouble,

            AI_ALMACENAMIENTO = almacenamiento,

            AI_FRECUENCIA_CONSUMO = frecuenciaConsumo,

            AI_TIPO_ABASTECIMIENTO = tipoAbastecimiento,

            ALC_NOMBRE = null,

            ALC_ICONO = null,

            categoria = categoria
        )

        ingredientesTemporales.add(
            ingredienteTemporal
        )

        adapterIngredientesTemporales.actualizarLista(
            ingredientesTemporales
        )

        misIngredientes.visibility = View.VISIBLE
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
                        this@MiHogarActivity,
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
                        this@MiHogarActivity,
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
                    this@MiHogarActivity,
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
                        this@MiHogarActivity,
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
                    this@MiHogarActivity,
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
                        this@MiHogarActivity,
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
                        this@MiHogarActivity,
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
                makeText(
                    this@MiHogarActivity,
                    "Error de conexión con el servidor.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    private fun guardarHogar() {

        // ==========================================
        // EVITAR DOBLE CLIC
        // ==========================================

        if (guardandoHogar) {
            return
        }

        guardandoHogar = true

        lifecycleScope.launch {

            val dialogBinding =
                dialogAgregarHogarBinding

            // Desactivar botón inmediatamente
            dialogBinding?.guardarHogar?.isEnabled = false

            try {

                // ==========================================
                // VERIFICAR DIALOG
                // ==========================================

                if (dialogBinding == null) {

                    guardandoHogar = false

                    return@launch
                }

                // ==========================================
                // OBTENER USUARIO DE LA SESIÓN
                // ==========================================

                val cliId =
                    SesionUsuario.obtenerId(
                        this@MiHogarActivity
                    )

                if (cliId <= 0) {

                    makeText(
                        this@MiHogarActivity,
                        "No se encontró el usuario de la sesión.",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@launch
                }

                // ==========================================
                // OBTENER NOMBRE DEL HOGAR
                // ==========================================

                val nombreHogar =
                    dialogBinding.txtNombreHogar.text
                        .toString()
                        .trim()

                if (nombreHogar.isEmpty()) {

                    makeText(
                        this@MiHogarActivity,
                        "Ingresa el nombre del hogar.",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@launch
                }

                // ==========================================
                // CREAR REQUEST DEL HOGAR
                // ==========================================

                val requestHogar =
                    CrearHogarRequest(

                        CLI_ID = cliId,

                        HOG_NOMBRE =
                            nombreHogar,

                        HOG_ICONO =
                            iconoHogarSeleccionado,

                        HOG_LATITUD =
                            latitudHogar,

                        HOG_LONGITUD =
                            longitudHogar,

                        HOG_DIRECCION =
                            direccionHogar
                    )

                // ==========================================
                // CREAR HOGAR
                // ==========================================

                val responseHogar =
                    ApiClient.apiService.crearHogar(
                        requestHogar
                    )

                // ==========================================
                // VERIFICAR CREACIÓN DEL HOGAR
                // ==========================================

                if (!responseHogar.success) {

                    Toast.makeText(
                        this@MiHogarActivity,
                        responseHogar.message,
                        Toast.LENGTH_SHORT
                    ).show()

                    return@launch
                }

                // ==========================================
                // OBTENER HOG_ID
                // ==========================================

                val hogId =
                    responseHogar.HOG_ID

                if (hogId == null) {

                    makeText(
                        this@MiHogarActivity,
                        "No se recibió el ID del hogar.",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@launch
                }

                // ==========================================
                // CREAR ALACENA
                // ==========================================

                val requestAlacena =
                    CrearAlacenaHogarRequest(

                        HOG_ID =
                            hogId,

                        ALC_CLI_ID =
                            null,

                        ALC_NOMBRE =
                            "Alacena de $nombreHogar",

                        ALC_ICONO =
                            iconoHogarSeleccionado
                    )

                // ==========================================
                // LLAMAR API ALACENA
                // ==========================================

                val responseAlacena =
                    ApiClient.apiService.crearAlacenaHogar(
                        requestAlacena
                    )

                // ==========================================
                // VERIFICAR ALACENA
                // ==========================================

                if (!responseAlacena.success) {

                    Toast.makeText(
                        this@MiHogarActivity,
                        responseAlacena.message,
                        Toast.LENGTH_SHORT
                    ).show()

                    return@launch
                }

                // ==========================================
                // OBTENER ALC_ID
                // ==========================================

                val alcId =
                    responseAlacena.ALC_ID

                if (alcId == null) {

                    makeText(
                        this@MiHogarActivity,
                        "No se recibió el ID de la alacena.",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@launch
                }

                // ==========================================
                // ENVIAR INVITACIONES
                // ==========================================

                val invitacionesCorrectas =
                    enviarInvitacionesHogar(

                        hogId = hogId,

                        miembros =
                            miembrosPendientes.toList()
                    )

                if (!invitacionesCorrectas) {

                    makeText(
                        this@MiHogarActivity,
                        "No se pudieron enviar todas las invitaciones.",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@launch
                }

                // ==========================================
                // GUARDAR INGREDIENTES TEMPORALES
                // ==========================================

                for (ingrediente in ingredientesTemporales) {

                    val requestIngrediente =
                        GuardarIngredienteAlacenaRequest(

                            ALC_ID =
                                alcId,

                            ING_ID =
                                ingrediente.ING_ID,

                            AI_CANTIDAD =
                                ingrediente.AI_CANTIDAD,

                            AI_UNIDAD =
                                ingrediente.AI_UNIDAD,

                            AI_FECHA_COMPRA =
                                ingrediente.AI_FECHA_COMPRA,

                            AI_FECHA_VENCIMIENTO =
                                ingrediente.AI_FECHA_VENCIMIENTO,

                            AI_ESTADO =
                                ingrediente.AI_ESTADO,

                            AI_PRECIO_COMPRA =
                                ingrediente.AI_PRECIO_COMPRA,

                            AI_ALMACENAMIENTO =
                                ingrediente.AI_ALMACENAMIENTO,

                            AI_FRECUENCIA_CONSUMO =
                                ingrediente.AI_FRECUENCIA_CONSUMO,

                            AI_TIPO_ABASTECIMIENTO =
                                ingrediente.AI_TIPO_ABASTECIMIENTO
                        )

                    val responseIngrediente =
                        ApiClient.apiService
                            .guardarIngredienteAlacena(
                                requestIngrediente
                            )

                    // ==========================================
                    // VERIFICAR INGREDIENTE
                    // ==========================================

                    if (!responseIngrediente.success) {

                        makeText(
                            this@MiHogarActivity,
                            "Error al guardar ${ingrediente.ING_DESCRIPCION}: " +
                                    responseIngrediente.message,
                            Toast.LENGTH_LONG
                        ).show()

                        return@launch
                    }
                }

                // ==========================================
                // ACTUALIZAR HOGAR ACTUAL
                // ==========================================

                cargarHogar()

                // ==========================================
                // LIMPIAR DATOS TEMPORALES
                // ==========================================

                ingredientesTemporales.clear()

                miembrosPendientes.clear()

                // ==========================================
                // CERRAR DIALOG
                // ==========================================

                dialogAgregarHogar?.dismiss()

                dialogAgregarHogar = null
                dialogAgregarHogarBinding = null

                // ==========================================
                // MENSAJE FINAL
                // ==========================================

                makeText(
                    this@MiHogarActivity,
                    "Hogar creado correctamente.",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {

                e.printStackTrace()

                makeText(
                    this@MiHogarActivity,
                    "Error al crear el hogar: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()

            } finally {

                // ==========================================
                // LIBERAR BLOQUEO
                // ==========================================

                guardandoHogar = false

                dialogAgregarHogarBinding
                    ?.guardarHogar
                    ?.isEnabled = true
            }
        }
    }

    private suspend fun enviarInvitacionesHogar(
        hogId: Int,
        miembros: List<UsuarioBusqueda>
    ): Boolean {

        for (usuario in miembros) {

            try {

                val cliIdPropietario =
                    SesionUsuario.obtenerId(this)

                if (cliIdPropietario <= 0) {

                    makeText(
                        this,
                        "No se encontró el usuario propietario.",
                        Toast.LENGTH_SHORT
                    ).show()

                    return false
                }

                val response =
                    ApiClient.apiService.invitarUsuarioHogar(
                        hogId,
                        cliIdPropietario,
                        usuario.CLI_ID
                    )

                if (!response.success) {

                    makeText(
                        this,
                        "No se pudo invitar a ${usuario.CLI_NOMBRE}: ${response.mensaje}",
                        Toast.LENGTH_SHORT
                    ).show()

                    return false
                }
            } catch (e: Exception) {

                makeText(
                    this,
                    "Error al invitar a ${usuario.CLI_NOMBRE}: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()

                return false
            }
        }

        return true
    }
    private fun configurarListaMiembrosHogar() {

        binding.miembrosdelhogar.layoutManager =
            LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL,
                false
            )

        adapterMiembrosHogar =
            MiembrosHogarAdapter(
                miembrosHogar
            ) { miembro ->

                eliminarMiembroHogar(
                    miembro = miembro
                )
            }

        binding.miembrosdelhogar.adapter =
            adapterMiembrosHogar
    }
    private fun cargarMiembrosHogar(
        hogId: Int
    ) {

        lifecycleScope.launch {

            try {

                val request =
                    ListarMiembrosHogarRequest(
                        HOG_ID = hogId
                    )

                val response =
                    ApiClient.apiService.listarMiembrosHogar(
                        request
                    )

                if (!response.success) {

                    Toast.makeText(
                        this@MiHogarActivity,
                        response.mensaje,
                        Toast.LENGTH_SHORT
                    ).show()

                    return@launch
                }

                // ==========================================
                // ACTUALIZAR LISTA
                // ==========================================

                miembrosHogar.clear()

                miembrosHogar.addAll(
                    response.miembros
                )

                // ==========================================
                // ACTUALIZAR ADAPTER
                // ==========================================

                adapterMiembrosHogar.actualizarMiembros(
                    miembrosHogar
                )

                // ==========================================
                // MOSTRAR / OCULTAR
                // ==========================================

                binding.miembrosdelhogar.visibility =
                    if (miembrosHogar.isEmpty()) {
                        View.GONE
                    } else {
                        View.VISIBLE
                    }

            } catch (e: Exception) {

                e.printStackTrace()

                makeText(
                    this@MiHogarActivity,
                    "Error al cargar los miembros: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    private fun eliminarMiembroHogar(
        miembro: MiembroHogar
    ) {

        // ==========================================
        // OBTENER HOGAR
        // ==========================================

        val hogId = hogarActual?.HOG_ID

        // ==========================================
        // OBTENER PROPIETARIO
        // ==========================================

        val cliIdPropietario =
            SesionUsuario.obtenerId(this)

        // ==========================================
        // VALIDAR HOGAR
        // ==========================================

        if (hogId == null) {

            makeText(
                this,
                "No se encontró el hogar.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // ==========================================
        // VALIDAR PROPIETARIO
        // ==========================================

        if (cliIdPropietario <= 0) {

            makeText(
                this,
                "No se encontró el usuario de la sesión.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // ==========================================
        // ELIMINAR MIEMBRO
        // ==========================================

        lifecycleScope.launch {

            try {

                val request =
                    EliminarMiembroHogarRequest(

                        HOG_ID = hogId,

                        CLI_ID_PROPIETARIO =
                            cliIdPropietario,

                        HOG_USU_ID =
                            miembro.HOG_USU_ID
                    )

                // ==========================================
                // LLAMAR API
                // ==========================================

                val response =
                    ApiClient.apiService
                        .eliminarMiembroHogar(request)

                // ==========================================
                // MOSTRAR MENSAJE
                // ==========================================

                Toast.makeText(
                    this@MiHogarActivity,
                    response.message,
                    Toast.LENGTH_SHORT
                ).show()

                // ==========================================
                // ACTUALIZAR LISTA
                // ==========================================

                if (response.success) {

                    miembrosHogar.removeAll {

                        it.HOG_USU_ID ==
                                miembro.HOG_USU_ID
                    }

                    adapterMiembrosHogar.actualizarMiembros(
                        miembrosHogar
                    )

                    // ==========================================
                    // OCULTAR SI NO HAY MIEMBROS
                    // ==========================================

                    binding.miembrosdelhogar.visibility =
                        if (miembrosHogar.isEmpty()) {
                            View.GONE
                        } else {
                            View.VISIBLE
                        }
                }

            } catch (e: Exception) {

                makeText(
                    this@MiHogarActivity,
                    "Error al eliminar miembro: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    private fun mostrarDialogNuevosUsuarios() {

        val bottomSheet = BottomSheetDialog(this)

        val view = layoutInflater.inflate(
            R.layout.dialog_buscar_nuevos_usuarios,
            null
        )

        bottomSheet.setContentView(view)

        // ==========================================
        // REFERENCIAS
        // ==========================================

        val txtBuscar = view.findViewById<EditText>(
            R.id.txtNombreusuariobuscar
        )

        val recyclerUsuarios = view.findViewById<RecyclerView>(
            R.id.recyclerUsuariosBusqueda
        )

        val recyclerSeleccionados = view.findViewById<RecyclerView>(
            R.id.miembrosdelhogar
        )

        val btnCancelar = view.findViewById<Button>(
            R.id.btnCancelarMiembros
        )

        val btnGuardar = view.findViewById<Button>(
            R.id.btnGuardarMiembros
        )



        val nuevosMiembros = mutableListOf<UsuarioBusqueda>()

        // ==========================================
        // LISTA VISUAL COMBINADA
        // ==========================================
        // Contendrá:
        //
        // 1. Miembros que ya pertenecen al hogar
        // 2. Nuevos miembros seleccionados
        // ==========================================

        val itemsMiembrosEditar =
            mutableListOf<MiembroEditarItem>()

        // ==========================================
        // ADAPTER DE MIEMBROS
        // ==========================================

        lateinit var adapterEditar: MiembrosEditarHogarAdapter

        adapterEditar = MiembrosEditarHogarAdapter(
            itemsMiembrosEditar
        ) { usuario ->

            // ======================================
            // ELIMINAR SOLO UN NUEVO MIEMBRO
            // ======================================

            nuevosMiembros.removeAll {
                it.CLI_ID == usuario.CLI_ID
            }

            // Quitar de la lista visual
            itemsMiembrosEditar.removeAll { item ->

                item is MiembroEditarItem.Nuevo &&
                        item.usuario.CLI_ID == usuario.CLI_ID
            }

            adapterEditar.actualizarItems(
                itemsMiembrosEditar
            )
        }

        // ==========================================
        // CONFIGURAR RECYCLERVIEW DE MIEMBROS
        // ==========================================

        recyclerSeleccionados.layoutManager =
            LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL,
                false
            )

        recyclerSeleccionados.adapter = adapterEditar

        // ==========================================
        // CARGAR MIEMBROS EXISTENTES
        // ==========================================

        val hogar = hogarActual

        if (hogar != null) {

            lifecycleScope.launch {

                try {

                    val response =
                        ApiClient.apiService.listarMiembrosHogar(
                            ListarMiembrosHogarRequest(
                                HOG_ID = hogar.HOG_ID
                            )
                        )

                    if (response.success) {

                        // Limpiar por seguridad
                        itemsMiembrosEditar.clear()

                        // ==================================
                        // AGREGAR MIEMBROS EXISTENTES
                        // ==================================

                        response.miembros.forEach { miembro ->

                            itemsMiembrosEditar.add(
                                MiembroEditarItem.Existente(
                                    miembro
                                )
                            )
                        }

                        recyclerSeleccionados.visibility =
                            View.VISIBLE

                        adapterEditar.actualizarItems(
                            itemsMiembrosEditar
                        )

                    } else {

                        recyclerSeleccionados.visibility =
                            View.GONE
                    }

                } catch (e: Exception) {

                    Log.e(
                        "MiHogar",
                        "Error cargando miembros del hogar",
                        e
                    )

                    makeText(
                        this@MiHogarActivity,
                        "No se pudieron cargar los integrantes.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        } else {

            recyclerSeleccionados.visibility =
                View.GONE
        }

        // ==========================================
        // ADAPTER DE BÚSQUEDA
        // ==========================================

        val adapterBusqueda =
            UsuariosBusquedaAdapter(emptyList()) { usuario ->

                // ======================================
                // COMPROBAR SI YA PERTENECE AL HOGAR
                // ======================================

                val yaExiste = miembrosHogar.any {

                    it.CLI_ID != null &&
                            it.CLI_ID == usuario.CLI_ID
                }

                if (yaExiste) {

                    makeText(
                        this,
                        "${usuario.CLI_NOMBRE} ya pertenece al hogar.",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@UsuariosBusquedaAdapter
                }

                // ======================================
                // COMPROBAR SI YA FUE SELECCIONADO
                // ======================================

                val yaSeleccionado = nuevosMiembros.any {

                    it.CLI_ID == usuario.CLI_ID
                }

                if (yaSeleccionado) {

                    makeText(
                        this,
                        "${usuario.CLI_NOMBRE} ya está seleccionado.",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@UsuariosBusquedaAdapter
                }

                // ======================================
                // AGREGAR A LISTA TEMPORAL
                // ======================================

                nuevosMiembros.add(usuario)

                // ======================================
                // AGREGAR A LISTA VISUAL
                // ======================================

                itemsMiembrosEditar.add(
                    MiembroEditarItem.Nuevo(
                        usuario
                    )
                )

                // ======================================
                // ACTUALIZAR RECYCLERVIEW
                // ======================================

                recyclerSeleccionados.visibility =
                    View.VISIBLE

                adapterEditar.actualizarItems(
                    itemsMiembrosEditar
                )

                // ======================================
                // OCULTAR RESULTADOS
                // ======================================

                recyclerUsuarios.visibility =
                    View.GONE

                txtBuscar.setText("")
            }

        recyclerUsuarios.layoutManager =
            LinearLayoutManager(this)

        recyclerUsuarios.adapter =
            adapterBusqueda

        recyclerUsuarios.visibility =
            View.GONE

        // ==========================================
        // BUSCADOR
        // ==========================================

        var runnableBusqueda: Runnable? = null

        txtBuscar.addTextChangedListener(

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
                }

                override fun afterTextChanged(
                    s: Editable?
                ) {

                    val texto =
                        s?.toString()
                            ?.trim()
                            ?: ""

                    runnableBusqueda?.let {
                        handler.removeCallbacks(it)
                    }

                    if (texto.length < 2) {

                        recyclerUsuarios.visibility =
                            View.GONE

                        return
                    }

                    runnableBusqueda =
                        Runnable {

                            buscarUsuariosParaAgregar(
                                texto,
                                adapterBusqueda,
                                recyclerUsuarios
                            )
                        }

                    handler.postDelayed(
                        runnableBusqueda!!,
                        500
                    )
                }
            }
        )

        // ==========================================
        // CANCELAR
        // ==========================================

        btnCancelar.setOnClickListener {

            // Los nuevos miembros solamente estaban
            // en memoria.
            //
            // Al cancelar no se guarda nada.
            // No se envían invitaciones.

            nuevosMiembros.clear()

            bottomSheet.dismiss()
        }

        // ==========================================
        // GUARDAR
        // ==========================================

        btnGuardar.setOnClickListener {

            if (nuevosMiembros.isEmpty()) {

                makeText(
                    this,
                    "No agregaste ningún integrante.",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            // ======================================
            // AQUÍ SÍ SE GUARDAN / INVITAN
            // ======================================

            guardarNuevosMiembros(
                nuevosMiembros,
                bottomSheet
            )
        }

        // ==========================================
        // CONFIGURAR BOTTOM SHEET
        // ==========================================

        bottomSheet.setOnShowListener {

            val dialog =
                it as BottomSheetDialog

            val bottomSheetView =
                dialog.findViewById<View>(
                    com.google.android.material.R.id.design_bottom_sheet
                )

            bottomSheetView?.let { sheet ->

                val behavior =
                    BottomSheetBehavior.from(sheet)

                behavior.state =
                    BottomSheetBehavior.STATE_EXPANDED

                behavior.skipCollapsed =
                    true

                sheet.background =
                    ContextCompat.getDrawable(
                        this,
                        R.drawable.bg_bottom_sheet_alacena
                    )
            }
        }

        bottomSheet.show()
    }
    private fun buscarUsuariosParaAgregar(
        texto: String,
        adapter: UsuariosBusquedaAdapter,
        recycler: RecyclerView
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

                    recycler.visibility =
                        View.GONE

                    return@launch
                }

                adapter.actualizarUsuarios(
                    respuesta.usuarios
                )

                recycler.visibility =
                    if (respuesta.usuarios.isNotEmpty()) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }

            } catch (e: Exception) {

                e.printStackTrace()

                recycler.visibility =
                    View.GONE

                makeText(
                    this@MiHogarActivity,
                    "Error al buscar usuarios",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    private fun guardarNuevosMiembros(
        nuevosMiembros: List<UsuarioBusqueda>,
        bottomSheet: BottomSheetDialog
    ) {

        val hogar = hogarActual

        if (hogar == null) {

            makeText(
                this,
                "No se encontró el hogar.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (nuevosMiembros.isEmpty()) {

            makeText(
                this,
                "No hay nuevos integrantes.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        lifecycleScope.launch {

            try {

                val cliIdPropietario =
                    SesionUsuario.obtenerId(this@MiHogarActivity)

                if (cliIdPropietario <= 0) {

                    makeText(
                        this@MiHogarActivity,
                        "No se encontró el usuario propietario.",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@launch
                }


                // ==========================================
                // AGREGAR CADA USUARIO
                // ==========================================

                var agregados = 0

                for (usuario in nuevosMiembros) {

                    val request =
                        AgregarMiembroHogarRequest(

                            HOG_ID =
                                hogar.HOG_ID,

                            CLI_ID_PROPIETARIO =
                                cliIdPropietario,

                            CLI_ID =
                                usuario.CLI_ID
                        )


                    val response =
                        ApiClient.apiService
                            .agregarMiembroHogar(
                                request
                            )


                    if (response.success) {

                        agregados++

                    } else {

                        makeText(
                            this@MiHogarActivity,
                            "${usuario.CLI_NOMBRE}: ${response.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }


                // ==========================================
                // SI SE AGREGÓ AL MENOS UNO
                // ==========================================

                if (agregados > 0) {

                    makeText(
                        this@MiHogarActivity,
                        "$agregados integrante(s) agregado(s) correctamente.",
                        Toast.LENGTH_SHORT
                    ).show()

                    bottomSheet.dismiss()

                    // Recargar lista
                    cargarMiembrosHogar(
                        hogar.HOG_ID
                    )
                }

            } catch (e: Exception) {

                e.printStackTrace()

                makeText(
                    this@MiHogarActivity,
                    "Error al guardar integrantes: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    private fun agregarNuevoMiembro(
        usuario: UsuarioBusqueda,
        recycler: RecyclerView
    ) {

        // ==========================================
        // YA EXISTE EN EL HOGAR
        // ==========================================

        val yaEsMiembro =
            miembrosHogar.any {
                it.CLI_ID == usuario.CLI_ID
            }

        if (yaEsMiembro) {

            makeText(
                this,
                "${usuario.CLI_NOMBRE} ya pertenece al hogar.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        makeText(
            this,
            "${usuario.CLI_NOMBRE} seleccionado.",
            Toast.LENGTH_SHORT
        ).show()

        // Aquí agregaremos el usuario
        // a la lista temporal.
    }
}