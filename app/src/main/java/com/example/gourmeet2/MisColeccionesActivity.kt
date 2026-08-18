package com.example.gourmeet2
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.Toast
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gourmeet2.data.api.ApiClient
import com.example.gourmeet2.data.models.*
import com.example.gourmeet2.databinding.ActivityMisColeccionesBinding
import com.example.gourmeet2.databinding.DialogIconosBinding
import com.example.gourmeet2.databinding.DialogNuevaColeccionBinding
import com.example.gourmeet2.utils.SesionUsuario
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import android.os.Handler
import android.util.Log
import com.google.android.material.bottomsheet.BottomSheetBehavior

class MisColeccionesActivity :
    AppCompatActivity() {
    private lateinit var binding: ActivityMisColeccionesBinding
    private val recetasSeleccionadas =mutableListOf<RecetaconFiltro>()
    private val recetasEdicion = mutableListOf<RecetaconFiltro>()
    private lateinit var adapter: ColeccionesAdapter
    private lateinit var adapterBusqueda:BusquedaColeccionAdapter
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var adapterIngredientes: IngredientesSeleccionadosAdapter
    private lateinit var adapterRecetasRealizadas: RecetaCarrAdapterSelec
    private val recetasRealizadas = mutableListOf<RecetaconFiltro>()
    private val ingredientesSeleccionados = mutableListOf<ResultadoBusqueda>()
    private var runnableBusqueda: Runnable? = null
    private lateinit var adapterRecetasSeleccionadas: RecetasCardAdapterSelec

    private lateinit var adapterRecetasMisIngredientes: RecetasMisIngredientesAdapter
    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)
        binding = ActivityMisColeccionesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        inicializarRecycler()
        inicializarRecyclerRecetasRealizadas()
        cargarRecetasRealizadas()
        cargarColecciones()
        eventos()

    }
    private fun inicializarRecyclerRecetasSeleccionadas(
        bindingDialog: DialogNuevaColeccionBinding,
        listaSeleccionadas: MutableList<RecetaconFiltro>
    ) {

        adapterRecetasSeleccionadas =
            RecetasCardAdapterSelec(

                // ==========================================
                // LISTA QUE SE MOSTRARÁ EN EL RECYCLER
                // ==========================================

                listaSeleccionadas.toMutableList(),

                // ==========================================
                // LISTA DE RECETAS SELECCIONADAS
                // ==========================================

                listaSeleccionadas,

                // ==========================================
                // CLICK EN LA TARJETA
                // ==========================================

                onRecetaClick = { receta ->

                    val intent =
                        Intent(
                            this@MisColeccionesActivity,
                            DetalleRecetaActivity::class.java
                        )

                    intent.putExtra(
                        "REC_ID",
                        receta.REC_ID
                    )

                    startActivity(intent)
                },

                // ==========================================
                // CLICK EN CÍRCULO
                // ==========================================

                onSeleccionarReceta = { receta, seleccionada ->

                    if (seleccionada) {

                        // ------------------------------
                        // AGREGAR
                        // ------------------------------

                        if (
                            listaSeleccionadas.none {
                                it.REC_ID == receta.REC_ID
                            }
                        ) {

                            listaSeleccionadas.add(
                                receta
                            )
                        }

                    } else {

                        // ------------------------------
                        // ELIMINAR
                        // ------------------------------

                        listaSeleccionadas.removeAll {

                            it.REC_ID == receta.REC_ID

                        }
                    }


                    // ==================================
                    // ACTUALIZAR RECETAS MOSTRADAS
                    // ==================================

                    adapterRecetasSeleccionadas.actualizar(
                        listaSeleccionadas
                    )


                    // ==================================
                    // ACTUALIZAR ESTADO DE LOS CÍRCULOS
                    // ==================================

                    if (::adapterBusqueda.isInitialized) {

                        adapterBusqueda.notifyDataSetChanged()

                    }
                }
            )


        // ==============================================
        // CONFIGURAR RECYCLER
        // ==============================================

        bindingDialog.recyclerRecetas.apply {

            layoutManager =
                LinearLayoutManager(
                    this@MisColeccionesActivity,
                    LinearLayoutManager.HORIZONTAL,
                    false
                )

            adapter =
                adapterRecetasSeleccionadas
        }
    }
    private fun inicializarRecycler() {

        adapter = ColeccionesAdapter(

            mutableListOf(),

            object : ColeccionesAdapter.OnColeccionListener {

                override fun onEditar(
                    coleccion: ColeccionConRecetas
                ) {

                    mostrarDialogEditarColeccion(
                        coleccion
                    )
                }

                override fun onEliminar(
                    coleccion: ColeccionConRecetas
                ) {

                    confirmarEliminarColeccion(
                        coleccion
                    )

                }

                override fun onRecetaClick(
                    recetaId: Int
                ) {

                    val intent = Intent(
                        this@MisColeccionesActivity,
                        DetalleRecetaActivity::class.java
                    )

                    intent.putExtra(
                        "REC_ID",
                        recetaId
                    )

                    startActivity(intent)

                }

            }

        )

        binding.recyclerColecciones.apply {

            layoutManager = LinearLayoutManager(
                this@MisColeccionesActivity
            )

            adapter =
                this@MisColeccionesActivity.adapter

        }

    }

    private fun cargarColecciones() {

        lifecycleScope.launch {

            try {

                val response = ApiClient.apiService
                    .listarColeccionesConRecetas(

                        ListarColeccionesRecetasRequest(

                            SesionUsuario.obtenerId(
                                this@MisColeccionesActivity
                            )

                        )

                    )

                if (response.success) {

                    adapter.actualizar(

                        response.colecciones

                    )

                }

            } catch (e: Exception) {

                Toast.makeText(

                    this@MisColeccionesActivity,

                    "No fue posible cargar las colecciones",

                    Toast.LENGTH_SHORT

                ).show()

            }

        }

    }

    private fun eventos() {

        binding.layoutNuevaColeccion.setOnClickListener {

            mostrarDialogNuevaColeccion()

        }

    }
    private fun mostrarDialogNuevaColeccion() {

        // =====================================================
        // CREAR BINDING
        // =====================================================

        val bindingDialog =
            DialogNuevaColeccionBinding.inflate(
                layoutInflater
            )

        val dialog =
            BottomSheetDialog(this)
        bindingDialog.imgCerrar.setOnClickListener {
            dialog.dismiss()
        }


        // =====================================================
        // RECETAS SELECCIONADAS
        // =====================================================

        inicializarRecyclerRecetasSeleccionadas(
            bindingDialog,
            recetasSeleccionadas
        )


        // =====================================================
        // RECETAS CON MIS INGREDIENTES
        // =====================================================

        adapterRecetasMisIngredientes =
            RecetasMisIngredientesAdapter(

                // Recetas encontradas
                mutableListOf(),

                // Recetas actualmente seleccionadas
                recetasSeleccionadas,

                // Click en tarjeta
                onRecetaClick = { receta ->

                    val intent =
                        Intent(
                            this@MisColeccionesActivity,
                            DetalleRecetaActivity::class.java
                        )

                    intent.putExtra(
                        "REC_ID",
                        receta.REC_ID
                    )

                    startActivity(intent)
                },

                // Click en círculo
                onSeleccionarReceta = { receta, seleccionada ->

                    if (seleccionada) {

                        agregarReceta(
                            receta
                        )

                    } else {

                        eliminarReceta(
                            receta
                        )
                    }

                    // Actualizar sección de recetas de la colección
                    adapterRecetasSeleccionadas.actualizar(
                        recetasSeleccionadas
                    )

                    // Actualizar círculos de búsqueda
                    if (::adapterBusqueda.isInitialized) {

                        adapterBusqueda.notifyDataSetChanged()

                    }

                    // Actualizar círculos de esta sección
                    adapterRecetasMisIngredientes
                        .actualizarSeleccionadas()
                }
            )


        bindingDialog.recyclerRecetasMisIngredientes.apply {

            layoutManager =
                LinearLayoutManager(
                    this@MisColeccionesActivity,
                    LinearLayoutManager.HORIZONTAL,
                    false
                )

            adapter =
                adapterRecetasMisIngredientes
        }


        // =====================================================
        // INGREDIENTES SELECCIONADOS
        // =====================================================

        adapterIngredientes =
            IngredientesSeleccionadosAdapter(

                mutableListOf(),

                object :
                    IngredientesSeleccionadosAdapter
                    .OnIngredienteSeleccionadoListener {

                    override fun onEliminarIngrediente(
                        ingrediente: ResultadoBusqueda
                    ) {

                        ingredientesSeleccionados.removeAll {

                            it.id == ingrediente.id

                        }

                        adapterIngredientes.actualizar(
                            ingredientesSeleccionados
                        )

                        buscarRecetasConMisIngredientes(
                            bindingDialog
                        )
                    }
                }
            )


        bindingDialog.recyclerIngredientesSeleccionados.apply {

            layoutManager =
                LinearLayoutManager(
                    this@MisColeccionesActivity,
                    LinearLayoutManager.HORIZONTAL,
                    false
                )

            adapter =
                adapterIngredientes
        }


        // =====================================================
        // ADAPTER DE RESULTADOS DE BÚSQUEDA
        // =====================================================

        adapterBusqueda =
            BusquedaColeccionAdapter(

                mutableListOf(),

                recetasSeleccionadas,

                object :
                    BusquedaColeccionAdapter
                    .OnResultadoClickListener {


                    // =========================================
                    // INGREDIENTE
                    // =========================================

                    override fun onIngredienteClick(
                        resultado: ResultadoBusqueda
                    ) {

                        val yaExiste =
                            ingredientesSeleccionados.any {

                                it.id == resultado.id

                            }

                        if (yaExiste) {
                            return
                        }


                        ingredientesSeleccionados.add(
                            resultado
                        )


                        adapterIngredientes.actualizar(
                            ingredientesSeleccionados
                        )


                        buscarRecetasConMisIngredientes(
                            bindingDialog
                        )


                        bindingDialog
                            .layoutIngredientesSeleccionados
                            .visibility = View.VISIBLE


                        bindingDialog
                            .txtResultadosBusqueda
                            .visibility = View.GONE


                        bindingDialog
                            .recyclerResultadosBusqueda
                            .visibility = View.GONE


                        bindingDialog
                            .edtBuscarRecetas
                            .setText("")
                    }


                    // =========================================
                    // RECETA
                    // =========================================

                    override fun onRecetaClick(
                        receta: RecetaconFiltro
                    ) {

                        val intent =
                            Intent(
                                this@MisColeccionesActivity,
                                DetalleRecetaActivity::class.java
                            )

                        intent.putExtra(
                            "REC_ID",
                            receta.REC_ID
                        )

                        startActivity(intent)
                    }


                    // =========================================
                    // CATEGORIA
                    // =========================================

                    override fun onCategoriaClick(
                        resultado: ResultadoBusqueda
                    ) {

                        Toast.makeText(
                            this@MisColeccionesActivity,
                            resultado.nombre,
                            Toast.LENGTH_SHORT
                        ).show()
                    }


                    // =========================================
                    // SELECCIONAR / DESELECCIONAR RECETA
                    // =========================================

                    override fun onSeleccionarReceta(
                        receta: RecetaconFiltro,
                        seleccionada: Boolean
                    ) {

                        Log.d(
                            "COLECCION",
                            "Click receta: ${receta.REC_ID} - ${receta.REC_NOMBRE}"
                        )

                        Log.d(
                            "COLECCION",
                            "Seleccionada: $seleccionada"
                        )

                        if (seleccionada) {

                            agregarReceta(receta)

                        } else {

                            eliminarReceta(receta)

                        }

                        Log.d(
                            "COLECCION",
                            "Total seleccionadas: ${recetasSeleccionadas.size}"
                        )

                        actualizarRecetasSeleccionadas()
                    }
                }
            )


        // =====================================================
        // RECYCLER DE RESULTADOS
        // =====================================================

        bindingDialog.recyclerResultadosBusqueda.apply {

            layoutManager =
                LinearLayoutManager(
                    this@MisColeccionesActivity
                )

            adapter =
                adapterBusqueda
        }


        // =====================================================
        // CERRAR BÚSQUEDA
        // =====================================================

        bindingDialog.imgCerrarBusqueda.setOnClickListener {

            bindingDialog.layoutBotones.visibility =
                View.VISIBLE

            bindingDialog.layoutBuscar.visibility =
                View.GONE

            bindingDialog.txtResultadosBusqueda.visibility =
                View.GONE

            bindingDialog.recyclerResultadosBusqueda.visibility =
                View.GONE

            bindingDialog.txtRecetas.visibility =
                View.VISIBLE

            bindingDialog.recyclerRecetas.visibility =
                View.VISIBLE
        }


        // =====================================================
        // ABRIR BÚSQUEDA
        // =====================================================

        bindingDialog.btnBuscarRecetas.setOnClickListener {

            bindingDialog.layoutBotones.visibility =
                View.GONE

            bindingDialog.layoutBuscar.visibility =
                View.VISIBLE

            bindingDialog.txtResultadosBusqueda.visibility =
                View.VISIBLE

            bindingDialog.recyclerResultadosBusqueda.visibility =
                View.VISIBLE
        }


        // =====================================================
        // BÚSQUEDA CON RETRASO DE 2 SEGUNDOS
        // =====================================================

        bindingDialog.edtBuscarRecetas.addTextChangedListener(

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

                    // -----------------------------------------
                    // CANCELAR BÚSQUEDA ANTERIOR
                    // -----------------------------------------

                    runnableBusqueda?.let {

                        handler.removeCallbacks(it)

                    }


                    // -----------------------------------------
                    // CREAR NUEVA BÚSQUEDA
                    // -----------------------------------------

                    val texto =
                        s?.toString()?.trim() ?: ""


                    if (texto.isEmpty()) {

                        adapterBusqueda.actualizar(
                            emptyList()
                        )

                        return
                    }


                    runnableBusqueda =
                        Runnable {

                            buscarContenido(
                                texto,
                                bindingDialog
                            )
                        }


                    // -----------------------------------------
                    // ESPERAR 2 SEGUNDOS
                    // -----------------------------------------

                    handler.postDelayed(

                        runnableBusqueda!!,

                        1000

                    )
                }


                override fun afterTextChanged(
                    s: Editable?
                ) {
                }

            }
        )


        // =====================================================
        // ICONO DE COLECCIÓN
        // =====================================================

        bindingDialog.cardIconoColeccion.setOnClickListener {

            mostrarDialogIconos(
                bindingDialog
            )
        }
        // =====================================================
        // GUARDAR COLECCIÓN
        // =====================================================

        bindingDialog.btnGuardar.setOnClickListener {

            guardarColeccion(
                bindingDialog,
                dialog
            )

        }


        // =====================================================
        // AHORA SÍ MONTAMOS EL DIALOG
        // =====================================================

        dialog.setContentView(
            bindingDialog.root
        )


        // =====================================================
        // MOSTRAR
        // =====================================================

        dialog.show()
        dialog.behavior.state =
            BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.isDraggable = false


    }

    private fun actualizarRecetasSeleccionadas() {
        adapterRecetasSeleccionadas.actualizar(
            recetasSeleccionadas
        )

        adapterBusqueda.notifyDataSetChanged()
    }
    private fun mostrarDialogIconos(
        bindingDialog: DialogNuevaColeccionBinding
    ) {

        val bindingIconos =
            DialogIconosBinding.inflate(layoutInflater)

        val dialog = BottomSheetDialog(this)

        dialog.setContentView(bindingIconos.root)

        bindingIconos.recyclerIconos.layoutManager =
            LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL,
                false
            )

        val iconos = listOf(

            IconoColeccion(
                "Favoritos",
                R.drawable.ic_favoritos
            ),

            IconoColeccion(
                "Desayuno",
                R.drawable.ic_desayuno
            ),

            IconoColeccion(
                "Pasta",
                R.drawable.ic_pasta
            ),

            IconoColeccion(
                "Guardar",
                R.drawable.ic_guardar
            )

        )
        bindingIconos.recyclerIconos.adapter =

            IconosAdapter(iconos) { icono ->

                bindingDialog.imgIcono.setImageResource(
                    icono.drawable
                )

                bindingDialog.imgIcono.tag =
                    icono.nombre

                dialog.dismiss()
            }

        dialog.show()

    }
    private fun buscarContenido(
        texto: String,
        bindingDialog: DialogNuevaColeccionBinding
    ) {

        lifecycleScope.launch {

            try {

                val response = ApiClient.apiService
                    .buscarContenidoColeccion(

                        BuscarContenidoColeccionRequest(

                            texto

                        )

                    )

                if (!response.success) return@launch
                val lista = mutableListOf<ResultadoBusqueda>()
                if (response.ingredientes.isNotEmpty()) {

                    lista.add(

                        ResultadoBusqueda(

                            tipo = ResultadoBusqueda.TITULO,

                            titulo = "INGREDIENTES"

                        )

                    )

                    response.ingredientes.forEach {

                        lista.add(

                            ResultadoBusqueda(

                                tipo = ResultadoBusqueda.INGREDIENTE,

                                id = it.id,

                                nombre = it.nombre,

                                foto = it.foto

                            )

                        )

                    }

                }
                if (response.recetas.isNotEmpty()) {

                    lista.add(
                        ResultadoBusqueda(
                            tipo = ResultadoBusqueda.TITULO,
                            titulo = "RECETAS"
                        )
                    )

                    lista.add(
                        ResultadoBusqueda(
                            tipo = ResultadoBusqueda.RECETAS_HORIZONTAL,
                            titulo = "",
                            recetas = response.recetas
                        )
                    )
                }
                // ==========================================
// CATEGORÍAS
// ==========================================

                if (!response.categorias.isNullOrEmpty()) {

                    lista.add(
                        ResultadoBusqueda(
                            tipo = ResultadoBusqueda.TITULO,
                            titulo = "CATEGORÍAS"
                        )
                    )

                    response.categorias.forEach { categoria ->

                        // ==========================================
                        // CATEGORÍA
                        // ==========================================

                        lista.add(
                            ResultadoBusqueda(
                                tipo = ResultadoBusqueda.CATEGORIA,
                                id = categoria.id,
                                nombre = categoria.nombre,
                                color = categoria.color
                            )
                        )

                        // ==========================================
                        // RECETAS DE LA CATEGORÍA
                        // ==========================================

                        val recetasCategoria =
                            categoria.recetas ?: emptyList()

                        if (recetasCategoria.isNotEmpty()) {

                            lista.add(
                                ResultadoBusqueda(
                                    tipo = ResultadoBusqueda.RECETAS_HORIZONTAL,
                                    titulo = "Recetas de ${categoria.nombre}",
                                    recetas = recetasCategoria
                                )
                            )
                        }
                    }
                }
                adapterBusqueda.actualizar(lista)
                if (lista.isEmpty()) {

                    bindingDialog.txtResultadosBusqueda.visibility = View.GONE
                    bindingDialog.recyclerResultadosBusqueda.visibility = View.GONE

                } else {

                    bindingDialog.txtResultadosBusqueda.visibility = View.VISIBLE
                    bindingDialog.recyclerResultadosBusqueda.visibility = View.VISIBLE

                }


            } catch (e: Exception) {

                e.printStackTrace()

                Toast.makeText(

                    this@MisColeccionesActivity,

                    "Error al buscar",

                    Toast.LENGTH_SHORT

                ).show()

            }

        }

    }
    private fun buscarRecetasConMisIngredientes(
        bindingDialog: DialogNuevaColeccionBinding
    ) {

        lifecycleScope.launch {

            try {

                val idsIngredientes =
                    ingredientesSeleccionados.map {
                        it.id
                    }

                Log.d(
                    "MIS_INGREDIENTES",
                    "IDs enviados: $idsIngredientes"
                )

                // --------------------------------
                // NO HAY INGREDIENTES
                // --------------------------------

                if (idsIngredientes.isEmpty()) {

                    adapterRecetasMisIngredientes.actualizar(
                        emptyList()
                    )

                    bindingDialog.txtRecetasMisIngredientes.visibility =
                        View.GONE

                    bindingDialog.recyclerRecetasMisIngredientes.visibility =
                        View.GONE

                    bindingDialog.layoutIngredientesSeleccionados.visibility =
                        View.GONE

                    return@launch
                }

                // --------------------------------
                // BUSCAR RECETAS
                // --------------------------------

                val response =
                    ApiClient.apiService.buscarRecetasPorIngredientes(

                        BuscarRecetasPorIngredientesRequest(
                            ingredientes = idsIngredientes
                        )

                    )

                if (!response.success) {

                    return@launch
                }

                // --------------------------------
                // ACTUALIZAR RECETAS
                // --------------------------------

                adapterRecetasMisIngredientes.actualizar(
                    response.recetas
                )

                // --------------------------------
                // MOSTRAR / OCULTAR SECCIÓN
                // --------------------------------

                if (response.recetas.isEmpty()) {

                    bindingDialog.txtRecetasMisIngredientes.visibility =
                        View.GONE

                    bindingDialog.recyclerRecetasMisIngredientes.visibility =
                        View.GONE

                } else {

                    bindingDialog.txtRecetasMisIngredientes.visibility =
                        View.VISIBLE

                    bindingDialog.recyclerRecetasMisIngredientes.visibility =
                        View.VISIBLE
                }

            } catch (e: Exception) {

                e.printStackTrace()

                Toast.makeText(
                    this@MisColeccionesActivity,
                    "Error al buscar recetas con mis ingredientes",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    private fun recetaEstaSeleccionada(
        receta: RecetaconFiltro
    ): Boolean {

        return recetasSeleccionadas.any {
            it.REC_ID == receta.REC_ID
        }
    }
    private fun agregarReceta(
        receta: RecetaconFiltro
    ) {

        if (!recetaEstaSeleccionada(receta)) {

            recetasSeleccionadas.add(receta)

        }
    }
    private fun eliminarReceta(
        receta: RecetaconFiltro
    ) {

        recetasSeleccionadas.removeAll {

            it.REC_ID == receta.REC_ID

        }
    }
    private fun guardarColeccion(
        bindingDialog: DialogNuevaColeccionBinding,
        dialog: BottomSheetDialog
    ) {

        // ==========================================
        // OBTENER DATOS
        // ==========================================

        val nombre =
            bindingDialog.edtNombre.text
                ?.toString()
                ?.trim()
                ?: ""

        val portada =
            bindingDialog.imgIcono.tag
                ?.toString()
                ?: ""


        // ==========================================
        // VALIDAR NOMBRE
        // ==========================================

        if (nombre.isEmpty()) {

            Toast.makeText(
                this@MisColeccionesActivity,
                "Escribe un nombre para la colección",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        // ==========================================
        // OBTENER ID DEL USUARIO
        // ==========================================

        val cliId =
            SesionUsuario.obtenerId(
                this@MisColeccionesActivity
            )


        if (cliId <= 0) {

            Toast.makeText(
                this@MisColeccionesActivity,
                "No se encontró el usuario",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        // ==========================================
        // OBTENER RECETAS
        // ==========================================

        val idsRecetas =
            recetasSeleccionadas.map {

                it.REC_ID

            }


        // ==========================================
        // VALIDAR RECETAS
        // ==========================================

        if (idsRecetas.isEmpty()) {

            Toast.makeText(
                this@MisColeccionesActivity,
                "Agrega al menos una receta",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        // ==========================================
        // CREAR REQUEST
        // ==========================================

        val request =
            CrearColeccionRequest(

                CLI_ID = cliId,

                COL_NOMBRE = nombre,

                COL_PORTADA = portada,

                COL_PRIVADA = 0,

                RECETAS = idsRecetas

            )


        // ==========================================
        // ENVIAR API
        // ==========================================

        lifecycleScope.launch {

            try {

                val response =
                    ApiClient.apiService
                        .crearColeccion(
                            request
                        )


                // ==================================
                // RESPUESTA
                // ==================================

                if (response.success) {

                    Toast.makeText(
                        this@MisColeccionesActivity,
                        "Colección creada correctamente",
                        Toast.LENGTH_SHORT
                    ).show()


                    // ==================================
                    // LIMPIAR LISTAS
                    // ==================================

                    recetasSeleccionadas.clear()

                    ingredientesSeleccionados.clear()


                    // ==================================
                    // CERRAR DIALOG
                    // ==================================

                    dialog.dismiss()


                    // ==================================
                    // RECARGAR COLECCIONES
                    // ==================================

                    cargarColecciones()


                } else {

                    Toast.makeText(
                        this@MisColeccionesActivity,
                        response.message,
                        Toast.LENGTH_LONG
                    ).show()
                }


            } catch (e: Exception) {

                e.printStackTrace()

                Toast.makeText(
                    this@MisColeccionesActivity,
                    "Error al crear la colección",
                    Toast.LENGTH_LONG
                ).show()
            }

        }
    }
    private fun confirmarEliminarColeccion(
        coleccion: ColeccionConRecetas
    ) {

        val dialog =
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Eliminar colección")
                .setMessage(
                    "¿Estás seguro de que deseas eliminar la colección \"${coleccion.COL_NOMBRE}\"?"
                )
                .setNegativeButton(
                    "Cancelar",
                    null
                )
                .setPositiveButton(
                    "Eliminar"
                ) { _, _ ->

                    eliminarColeccion(
                        coleccion
                    )

                }
                .create()


        // ==========================================
        // COLOR DE LOS BOTONES
        // ==========================================

        dialog.setOnShowListener {
            dialog.getButton(
                androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE
            ).setTextColor(
                androidx.core.content.ContextCompat.getColor(
                    this,
                    R.color.rojo
                )
            )
            dialog.getButton(
                androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE
            ).setTextColor(
                androidx.core.content.ContextCompat.getColor(
                    this,
                    R.color.azulgourmeet
                )
            )
        }


        dialog.show()
    }
    private fun eliminarColeccion(
        coleccion: ColeccionConRecetas
    ) {

        val cliId =
            SesionUsuario.obtenerId(
                this@MisColeccionesActivity
            )

        if (cliId <= 0) {

            Toast.makeText(
                this@MisColeccionesActivity,
                "Usuario inválido",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        val request =
            EliminarColeccionRequest(

                COL_ID =
                    coleccion.COL_ID,

                CLI_ID =
                    cliId

            )


        lifecycleScope.launch {

            try {

                val response =
                    ApiClient.apiService
                        .eliminarColeccion(
                            request
                        )


                if (response.success) {

                    Toast.makeText(
                        this@MisColeccionesActivity,
                        "Colección eliminada correctamente",
                        Toast.LENGTH_SHORT
                    ).show()


                    // ==================================
                    // ACTUALIZAR LISTA
                    // ==================================

                    cargarColecciones()


                } else {

                    Toast.makeText(
                        this@MisColeccionesActivity,
                        response.message,
                        Toast.LENGTH_LONG
                    ).show()

                }


            } catch (e: Exception) {

                e.printStackTrace()

                Toast.makeText(
                    this@MisColeccionesActivity,
                    "Error al eliminar la colección",
                    Toast.LENGTH_LONG
                ).show()

            }

        }
    }

    private fun convertirRecetaColeccion(
        receta: RecetaColeccion
    ): RecetaconFiltro {

        return RecetaconFiltro(

            REC_ID = receta.REC_ID,

            REC_NOMBRE = receta.REC_NOMBRE,

            REC_TIEMPO_PREPARACION =
                receta.REC_TIEMPO_PREPARACION,

            Dificultad =
                receta.Dificultad,

            Categoria =
                receta.Categoria,

            FotoReceta =
                receta.FotoReceta,

            Ingredientes =
                receta.Ingredientes,

            coincidencias = null,

            totalIngredientesBuscados = null,

            calorias = null,

            gasto = null

        )
    }
    private fun mostrarDialogEditarColeccion(
        coleccion: ColeccionConRecetas
    ) {

        // =====================================================
        // LIMPIAR LISTA DE EDICIÓN
        // =====================================================

        recetasEdicion.clear()


        // =====================================================
        // CARGAR RECETAS ACTUALES DE LA COLECCIÓN
        // =====================================================

        coleccion.RECETAS.forEach { receta ->

            recetasEdicion.add(
                convertirRecetaColeccion(receta)
            )
        }


        Log.d(
            "COLECCION_EDICION",
            "Recetas cargadas: ${recetasEdicion.size}"
        )


        // =====================================================
        // CREAR BINDING
        // =====================================================

        val bindingDialog =
            DialogNuevaColeccionBinding.inflate(
                layoutInflater
            )

        val dialog =
            BottomSheetDialog(this)


        // =====================================================
        // NOMBRE
        // =====================================================

        bindingDialog.edtNombre.setText(
            coleccion.COL_NOMBRE
        )
        ///icono
        val nombreIcono = coleccion.COL_PORTADA

        if (!nombreIcono.isNullOrEmpty()) {

            val drawableId =
                resources.getIdentifier(
                    nombreIcono,
                    "drawable",
                    packageName
                )

            if (drawableId != 0) {

                bindingDialog.imgIcono.setImageResource(
                    drawableId
                )

                bindingDialog.imgIcono.tag =
                    nombreIcono

            } else {

                bindingDialog.imgIcono.setImageResource(
                    R.drawable.ic_nuevo_icono
                )

                bindingDialog.imgIcono.tag = null
            }

        } else {

            bindingDialog.imgIcono.setImageResource(
                R.drawable.ic_nuevo_icono
            )

            bindingDialog.imgIcono.tag = null
        }



        // =====================================================
        // ADAPTER RECETAS DE LA COLECCIÓN
        // =====================================================

        adapterRecetasSeleccionadas =
            RecetasCardAdapterSelec(

                // Lista independiente para mostrar
                recetasEdicion.toMutableList(),

                // Lista real de seleccionadas
                recetasEdicion,

                onRecetaClick = { receta ->

                    val intent =
                        Intent(
                            this@MisColeccionesActivity,
                            DetalleRecetaActivity::class.java
                        )

                    intent.putExtra(
                        "REC_ID",
                        receta.REC_ID
                    )

                    startActivity(intent)
                },

                onSeleccionarReceta = { receta, seleccionada ->

                    if (seleccionada) {

                        if (
                            recetasEdicion.none {
                                it.REC_ID == receta.REC_ID
                            }
                        ) {

                            recetasEdicion.add(
                                receta
                            )
                        }

                    } else {

                        recetasEdicion.removeAll {

                            it.REC_ID == receta.REC_ID

                        }
                    }

                    // Actualizar únicamente la lista visual
                    adapterRecetasSeleccionadas.actualizar(
                        recetasEdicion.toList()
                    )

                    // Actualizar palomitas de los resultados
                    if (::adapterBusqueda.isInitialized) {

                        adapterBusqueda.notifyDataSetChanged()

                    }

                    Log.d(
                        "COLECCION_EDICION",
                        "Recetas restantes: ${recetasEdicion.size}"
                    )
                }
            )


        // =====================================================
        // RECYCLER DE RECETAS DE LA COLECCIÓN
        // =====================================================

        bindingDialog.recyclerRecetas.apply {

            layoutManager =
                LinearLayoutManager(
                    this@MisColeccionesActivity,
                    LinearLayoutManager.HORIZONTAL,
                    false
                )

            adapter =
                adapterRecetasSeleccionadas
        }


        // =====================================================
        // RECETAS CON MIS INGREDIENTES
        // =====================================================
        adapterRecetasMisIngredientes =
            RecetasMisIngredientesAdapter(

                mutableListOf(),

                recetasEdicion,

                onRecetaClick = { receta ->

                    val intent =
                        Intent(
                            this@MisColeccionesActivity,
                            DetalleRecetaActivity::class.java
                        )

                    intent.putExtra(
                        "REC_ID",
                        receta.REC_ID
                    )

                    startActivity(intent)
                },

                onSeleccionarReceta = { receta, seleccionada ->

                    if (seleccionada) {

                        if (
                            recetasEdicion.none {
                                it.REC_ID == receta.REC_ID
                            }
                        ) {

                            recetasEdicion.add(
                                receta
                            )
                        }

                    } else {

                        recetasEdicion.removeAll {

                            it.REC_ID == receta.REC_ID

                        }
                    }


                    // Actualizar recetas de la colección
                    adapterRecetasSeleccionadas.actualizar(
                        recetasEdicion
                    )


                    // Actualizar círculos de búsqueda
                    if (::adapterBusqueda.isInitialized) {

                        adapterBusqueda.notifyDataSetChanged()

                    }


                    // Actualizar círculos de "Mis ingredientes"
                    adapterRecetasMisIngredientes
                        .actualizarSeleccionadas()
                }
            )



        bindingDialog.recyclerRecetasMisIngredientes.apply {

            layoutManager =
                LinearLayoutManager(
                    this@MisColeccionesActivity,
                    LinearLayoutManager.HORIZONTAL,
                    false
                )

            adapter =
                adapterRecetasMisIngredientes
        }


        // =====================================================
        // INGREDIENTES SELECCIONADOS
        // =====================================================

        ingredientesSeleccionados.clear()

        adapterIngredientes =
            IngredientesSeleccionadosAdapter(

                mutableListOf(),

                object :
                    IngredientesSeleccionadosAdapter
                    .OnIngredienteSeleccionadoListener {

                    override fun onEliminarIngrediente(
                        ingrediente: ResultadoBusqueda
                    ) {

                        ingredientesSeleccionados.removeAll {

                            it.id == ingrediente.id

                        }

                        adapterIngredientes.actualizar(
                            ingredientesSeleccionados
                        )

                        buscarRecetasConMisIngredientes(
                            bindingDialog
                        )
                    }
                }
            )


        bindingDialog.recyclerIngredientesSeleccionados.apply {

            layoutManager =
                LinearLayoutManager(
                    this@MisColeccionesActivity,
                    LinearLayoutManager.HORIZONTAL,
                    false
                )

            adapter =
                adapterIngredientes
        }


        // =====================================================
        // ADAPTER DE BÚSQUEDA
        // =====================================================

        adapterBusqueda =
            BusquedaColeccionAdapter(

                mutableListOf(),

                // IMPORTANTE:
                // aquí usamos recetasEdicion
                recetasEdicion,

                object :
                    BusquedaColeccionAdapter
                    .OnResultadoClickListener {


                    // =========================================
                    // INGREDIENTE
                    // =========================================

                    override fun onIngredienteClick(
                        resultado: ResultadoBusqueda
                    ) {

                        val yaExiste =
                            ingredientesSeleccionados.any {

                                it.id == resultado.id

                            }

                        if (yaExiste) {
                            return
                        }


                        ingredientesSeleccionados.add(
                            resultado
                        )


                        adapterIngredientes.actualizar(
                            ingredientesSeleccionados
                        )


                        buscarRecetasConMisIngredientes(
                            bindingDialog
                        )


                        bindingDialog
                            .layoutIngredientesSeleccionados
                            .visibility =
                            View.VISIBLE


                        bindingDialog
                            .txtResultadosBusqueda
                            .visibility =
                            View.GONE


                        bindingDialog
                            .recyclerResultadosBusqueda
                            .visibility =
                            View.GONE


                        bindingDialog
                            .edtBuscarRecetas
                            .setText("")
                    }


                    // =========================================
                    // RECETA
                    // =========================================

                    override fun onRecetaClick(
                        receta: RecetaconFiltro
                    ) {

                        val intent =
                            Intent(
                                this@MisColeccionesActivity,
                                DetalleRecetaActivity::class.java
                            )

                        intent.putExtra(
                            "REC_ID",
                            receta.REC_ID
                        )

                        startActivity(intent)
                    }


                    // =========================================
                    // CATEGORÍA
                    // =========================================

                    override fun onCategoriaClick(
                        resultado: ResultadoBusqueda
                    ) {

                        Toast.makeText(
                            this@MisColeccionesActivity,
                            resultado.nombre,
                            Toast.LENGTH_SHORT
                        ).show()
                    }


                    // =========================================
                    // SELECCIONAR / DESELECCIONAR RECETA
                    // =========================================

                    override fun onSeleccionarReceta(
                        receta: RecetaconFiltro,
                        seleccionada: Boolean
                    ) {

                        Log.d(
                            "COLECCION_EDICION",
                            "Receta: ${receta.REC_ID}"
                        )

                        if (seleccionada) {

                            if (
                                recetasEdicion.none {
                                    it.REC_ID == receta.REC_ID
                                }
                            ) {

                                recetasEdicion.add(
                                    receta
                                )
                            }

                        } else {

                            recetasEdicion.removeAll {

                                it.REC_ID == receta.REC_ID

                            }
                        }

                        // Actualizar tarjetas
                        adapterRecetasSeleccionadas.actualizar(
                            recetasEdicion.toList()
                        )

                        // Actualizar círculos
                        adapterBusqueda.notifyDataSetChanged()

                        Log.d(
                            "COLECCION_EDICION",
                            "Total: ${recetasEdicion.size}"
                        )
                    }
                }
            )


        // =====================================================
        // RECYCLER RESULTADOS
        // =====================================================

        bindingDialog.recyclerResultadosBusqueda.apply {

            layoutManager =
                LinearLayoutManager(
                    this@MisColeccionesActivity
                )

            adapter =
                adapterBusqueda
        }


        // =====================================================
        // BOTÓN BUSCAR
        // =====================================================

        bindingDialog.btnBuscarRecetas.setOnClickListener {

            bindingDialog.layoutBotones.visibility =
                View.GONE

            bindingDialog.layoutBuscar.visibility =
                View.VISIBLE

            bindingDialog.txtResultadosBusqueda.visibility =
                View.VISIBLE

            bindingDialog.recyclerResultadosBusqueda.visibility =
                View.VISIBLE
        }


        // =====================================================
        // CERRAR BÚSQUEDA
        // =====================================================

        bindingDialog.imgCerrarBusqueda.setOnClickListener {

            bindingDialog.layoutBotones.visibility =
                View.VISIBLE

            bindingDialog.layoutBuscar.visibility =
                View.GONE

            bindingDialog.txtResultadosBusqueda.visibility =
                View.GONE

            bindingDialog.recyclerResultadosBusqueda.visibility =
                View.GONE

            bindingDialog.txtRecetas.visibility =
                View.VISIBLE

            bindingDialog.recyclerRecetas.visibility =
                View.VISIBLE
        }


        // =====================================================
        // BÚSQUEDA
        // =====================================================

        bindingDialog.edtBuscarRecetas.addTextChangedListener(

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

                    runnableBusqueda?.let {

                        handler.removeCallbacks(it)

                    }


                    val texto =
                        s?.toString()?.trim() ?: ""


                    if (texto.isEmpty()) {

                        adapterBusqueda.actualizar(
                            emptyList()
                        )

                        return
                    }


                    runnableBusqueda =
                        Runnable {

                            buscarContenido(
                                texto,
                                bindingDialog
                            )
                        }


                    handler.postDelayed(
                        runnableBusqueda!!,
                        1000
                    )
                }


                override fun afterTextChanged(
                    s: Editable?
                ) {
                }
            }
        )


        // =====================================================
        // ICONO
        // =====================================================

        bindingDialog.cardIconoColeccion.setOnClickListener {

            mostrarDialogIconos(
                bindingDialog
            )
        }


        // =====================================================
        // GUARDAR CAMBIOS
        // =====================================================

        bindingDialog.btnGuardar.setOnClickListener {

            actualizarColeccion(
                coleccion,
                bindingDialog,
                dialog
            )
        }


        // =====================================================
        // MOSTRAR DIALOG
        // =====================================================

        dialog.setContentView(
            bindingDialog.root
        )

        dialog.show()
    }
    private fun actualizarColeccion(
        coleccion: ColeccionConRecetas,
        bindingDialog: DialogNuevaColeccionBinding,
        dialog: BottomSheetDialog
    ) {

        // ==========================================
        // NOMBRE
        // ==========================================

        val nombre =
            bindingDialog.edtNombre.text
                ?.toString()
                ?.trim()
                ?: ""


        // ==========================================
        // VALIDAR NOMBRE
        // ==========================================

        if (nombre.isEmpty()) {

            Toast.makeText(
                this@MisColeccionesActivity,
                "Escribe un nombre para la colección",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        // ==========================================
        // USUARIO
        // ==========================================

        val cliId =
            SesionUsuario.obtenerId(
                this@MisColeccionesActivity
            )


        if (cliId <= 0) {

            Toast.makeText(
                this@MisColeccionesActivity,
                "Usuario inválido",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        // ==========================================
        // RECETAS DE LA EDICIÓN
        // ==========================================

        val idsRecetas =
            recetasEdicion.map {

                it.REC_ID

            }


        Log.d(
            "COLECCION_EDICION",
            "COL_ID: ${coleccion.COL_ID}"
        )

        Log.d(
            "COLECCION_EDICION",
            "Recetas a guardar: $idsRecetas"
        )


        // ==========================================
        // VALIDAR RECETAS
        // ==========================================

        if (idsRecetas.isEmpty()) {

            Toast.makeText(
                this@MisColeccionesActivity,
                "La colección debe tener al menos una receta",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        // ==========================================
        // PORTADA
        // ==========================================

        val portada =
            bindingDialog.imgIcono.tag
                ?.toString()
                ?: coleccion.COL_PORTADA
                ?: ""


        // ==========================================
        // REQUEST
        // ==========================================

        val request =
            ActualizarColeccionRequest(

                COL_ID =
                    coleccion.COL_ID,

                CLI_ID =
                    cliId,

                COL_NOMBRE =
                    nombre,

                COL_PORTADA =
                    portada,

                COL_PRIVADA =
                    if (coleccion.COL_PRIVADA) 1 else 0,

                RECETAS =
                    idsRecetas

            )


        // ==========================================
        // LLAMAR API
        // ==========================================

        lifecycleScope.launch {

            try {

                val response =
                    ApiClient.apiService
                        .actualizarColeccion(
                            request
                        )


                if (response.success) {

                    Toast.makeText(
                        this@MisColeccionesActivity,
                        "Colección actualizada correctamente",
                        Toast.LENGTH_SHORT
                    ).show()


                    // ==================================
                    // CERRAR
                    // ==================================

                    dialog.dismiss()


                    // ==================================
                    // RECARGAR COLECCIONES
                    // ==================================

                    cargarColecciones()


                } else {

                    Toast.makeText(
                        this@MisColeccionesActivity,
                        response.message,
                        Toast.LENGTH_LONG
                    ).show()
                }


            } catch (e: Exception) {

                e.printStackTrace()

                Toast.makeText(
                    this@MisColeccionesActivity,
                    "Error al actualizar la colección",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    private fun inicializarRecyclerRecetasRealizadas() {

        adapterRecetasRealizadas =
            RecetaCarrAdapterSelec(
                mutableListOf()
            ) { receta ->

                val intent =
                    Intent(
                        this@MisColeccionesActivity,
                        DetalleRecetaActivity::class.java
                    )

                intent.putExtra(
                    "REC_ID",
                    receta.REC_ID
                )

                startActivity(intent)
            }

        binding.recyclerRecetasRealizadas.apply {

            layoutManager =
                LinearLayoutManager(
                    this@MisColeccionesActivity,
                    LinearLayoutManager.HORIZONTAL,
                    false
                )

            adapter =
                adapterRecetasRealizadas

            isNestedScrollingEnabled = false
        }
    }
    private fun cargarRecetasRealizadas() {

        lifecycleScope.launch {

            try {

                val cliId =
                    SesionUsuario.obtenerId(
                        this@MisColeccionesActivity
                    )

                if (cliId <= 0) {
                    return@launch
                }

                val response =
                    ApiClient.apiService
                        .listarRecetasRealizadas(
                            RecetasRealizadasRequest(
                                CLI_ID = cliId
                            )
                        )

                if (!response.success) {

                    Toast.makeText(
                        this@MisColeccionesActivity,
                        "No fue posible cargar las recetas realizadas",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@launch
                }

                recetasRealizadas.clear()

                recetasRealizadas.addAll(
                    response.recetas
                )

                adapterRecetasRealizadas.actualizar(
                    recetasRealizadas
                )

            } catch (e: Exception) {

                e.printStackTrace()

                Toast.makeText(
                    this@MisColeccionesActivity,
                    "Error al cargar recetas realizadas",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}