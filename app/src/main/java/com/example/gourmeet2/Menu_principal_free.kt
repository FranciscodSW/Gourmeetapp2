package com.example.gourmeet2
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.gourmeet2.data.api.ApiClient
import com.example.gourmeet2.data.models.BuscarIngredientes
import com.example.gourmeet2.data.models.BuscarRecetas
import com.example.gourmeet2.data.models.FiltrosRecetasRequest
import com.example.gourmeet2.data.models.SeccionResultados
import com.example.gourmeet2.databinding.ActivityMenuPrincipalFreeBinding
import kotlinx.coroutines.launch
class Menu_principal_free : AppCompatActivity() {
    private var menuAbierto = false
    private lateinit var binding: ActivityMenuPrincipalFreeBinding
    private var modoActual = Modo.INGREDIENTES
    enum class Modo {INGREDIENTES,RECETAS }
    enum class Seccion { BUSCADOR, ALACENA, PLANEADOR}
    private var panelBusquedaAbierto = false
    private var seccionActual = Seccion.BUSCADOR
    private var textoBusqueda = ""
    private val ingredientesSeleccionados = mutableListOf<BuscarIngredientes>()
    private lateinit var seleccionadosAdapter: SeleccionadosAdapter
    private val recetasSeleccionadas = mutableListOf<BuscarRecetas>()
    private lateinit var recetasAdapter: RecetasAdapter
    private lateinit var adapterResultados: AdapterResultados


    private lateinit var adapter: IngredienteAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuPrincipalFreeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        actualizarModo()
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
        adapter = IngredienteAdapter(emptyList()) { ingrediente ->
            moverASeleccionados(ingrediente)
            binding.rvResultados.visibility = View.GONE
            binding.panelingredietes.visibility = View.GONE
            binding.editBusqueda.setText("")
        }
        adapterResultados = AdapterResultados(mutableListOf())

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
        }
        binding.imgFlecha.setOnClickListener {
            if (!menuAbierto) {
                mostrarMenuAnimado()
            } else {
                ocultarMenuAnimado()
            }
            menuAbierto = !menuAbierto
        }
        binding.opPostre.setOnClickListener {
            cambiarEncabezado(
                "Postre",
                R.drawable.ic_logo_rosa
            )
        }
        binding.opBebida.setOnClickListener {
            cambiarEncabezado("Bebida", R.drawable.ic_logo_naranja)
        }
        binding.opSnack.setOnClickListener {
            cambiarEncabezado("Snack", R.drawable.ic_logo_morado)
        }
        binding.opEntrada.setOnClickListener {
            cambiarEncabezado("Entrada", R.drawable.ic_logo_verde)
        }
        binding.opPlatoFuerte.setOnClickListener {
            cambiarEncabezado("Plato fuerte", R.drawable.ic_logo_azul)
        }
        binding.barraExpandirBusqueda.setOnClickListener {
            if (!panelBusquedaAbierto) {
                abrirPanelBusqueda()
            } else {cerrarPanelBusqueda()
            }
            panelBusquedaAbierto = !panelBusquedaAbierto
        }
        seleccionadosAdapter = SeleccionadosAdapter(
            ingredientesSeleccionados
        ) { ingrediente ->
            ingredientesSeleccionados.remove(ingrediente)
            seleccionadosAdapter.notifyDataSetChanged()
        }
        binding.rvSeleccionados.layoutManager =
            GridLayoutManager(this, 1, GridLayoutManager.HORIZONTAL, false)
        binding.rvSeleccionados.adapter = seleccionadosAdapter
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom.toFloat()
            val extraOffset = 24f // ajusta esto (16–48 suele ser ideal)
            binding.panelBusqueda.translationY = -(imeHeight - extraOffset)
            binding.panelingredietes.translationY = -(imeHeight - extraOffset)
            insets
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
    private fun cargarRecetasPorIngredientes() {
        lifecycleScope.launch {
            try {

                val request = FiltrosRecetasRequest(
                    ingredientes = ingredientesSeleccionados.map { it.id }
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
}