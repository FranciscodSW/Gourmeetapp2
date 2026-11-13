

package mx.com.gourmeet.app

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import mx.com.gourmeet.app.data.api.ApiClient
import mx.com.gourmeet.app.data.models.*
import mx.com.gourmeet.app.databinding.ActivityListaDeComprasBinding
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ListaDeComprasActivity : AppCompatActivity() {
    private var pdfPendiente: ByteArray? = null

    private val guardarPdfLauncher =
        registerForActivityResult(
            ActivityResultContracts.CreateDocument("application/pdf")
        ) { uri: Uri? ->

            if (uri != null) {
                guardarArchivoPdf(uri)
            } else {
                mostrarMensaje("Se canceló el guardado del PDF.")
                pdfPendiente = null
            }
        }

    private lateinit var binding: ActivityListaDeComprasBinding

    private var plaId: Int = -1
    private var hogId: Int = -1
    private var cliId: Int = -1

    private lateinit var adapterCategorias: CategoriaListaCompraAdapter

    private val actualizacionesEnCurso = mutableSetOf<Int>()
    private var cargaEnCurso: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityListaDeComprasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        plaId = intent.getIntExtra("PLA_ID", -1)
        hogId = intent.getIntExtra("HOG_ID", -1)
        cliId = intent.getIntExtra("CLI_ID", -1)

        // HOG_ID es opcional: -1 representa el planeador individual.
        // PLA_ID y CLI_ID sí son obligatorios en ambas modalidades.
        if (plaId <= 0 || cliId <= 0) {
            Toast.makeText(
                this,
                "No se recibieron correctamente los datos del planeador.",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        Log.d(
            "ListaDeCompras",
            "PLA_ID=$plaId, modalidad=${if (hogId > 0) "HOGAR" else "INDIVIDUAL"}, HOG_ID=${hogId.takeIf { it > 0 }}, CLI_ID=$cliId"
        )

        configurarPantalla()
    }

    // ==========================================
    // CONFIGURAR PANTALLA
    // ==========================================

    private fun configurarPantalla() {

        adapterCategorias = CategoriaListaCompraAdapter(
            mutableListOf()
        ) { ingrediente, nuevoEstado ->

            actualizarEstadoIngrediente(
                ingrediente,
                nuevoEstado
            )
        }
        binding.btnDescargarLista.setOnClickListener {
            descargarListaCompras()
        }

        binding.ingredientesfaltante.layoutManager =
            LinearLayoutManager(this)

        binding.ingredientesfaltante.isNestedScrollingEnabled = false

        binding.ingredientesfaltante.adapter = adapterCategorias

        cargarListaCompras()
    }

    // ==========================================
    // OBTENER, SINCRONIZAR Y CARGAR
    // ==========================================

    private fun cargarListaCompras() {

        if (cargaEnCurso) return
        cargaEnCurso = true

        lifecycleScope.launch {

            try {

                val request = ListaCompraRequest(
                    plaId = plaId,
                    cliId = cliId
                )

                // 1. OBTENER O CREAR LISTA

                val respuestaLista = ApiClient.apiService
                    .obtenerOCrearLista(request)

                if (!respuestaLista.success) {
                    mostrarMensaje(respuestaLista.message)
                    return@launch
                }

                Log.d(
                    "LISTA_COMPRAS",
                    "LCO_ID=${respuestaLista.lista?.lcoId}"
                )

                // 2. SINCRONIZAR INGREDIENTES

                val respuestaSync = ApiClient.apiService
                    .sincronizarListaCompras(request)

                if (!respuestaSync.success) {
                    mostrarMensaje(respuestaSync.message)
                    return@launch
                }

                Log.d(
                    "LISTA_COMPRAS",
                    "Sincronización correcta: ${respuestaSync.totalIngredientes}"
                )

                // 3. CONSULTAR INGREDIENTES

                consultarIngredientes()

            } catch (e: Exception) {

                Log.e(
                    "LISTA_COMPRAS",
                    "Error al cargar lista",
                    e
                )

                mostrarMensaje(
                    "Error al cargar la lista: ${e.message}"
                )
            } finally {
                cargaEnCurso = false
            }
        }
    }

    // ==========================================
    // CONSULTAR INGREDIENTES
    // ==========================================

    private suspend fun consultarIngredientes() {

        val request = ListaCompraRequest(
            plaId = plaId,
            cliId = cliId
        )

        val respuesta = ApiClient.apiService
            .listarIngredientesLista(request)

        if (!respuesta.success) {
            mostrarMensaje(respuesta.message)
            return
        }

        val ingredientes = respuesta.ingredientes ?: emptyList()

        Log.d(
            "LISTA_COMPRAS",
            "Ingredientes recibidos: ${ingredientes.size}"
        )

        // AGRUPAR POR CATEGORÍA

        val categorias = ingredientes
            .groupBy {
                it.categoria
                    ?.trim()
                    ?.ifEmpty { "Sin categoría" }
                    ?: "Sin categoría"
            }
            .map { (nombreCategoria, lista) ->

                CategoriaListaCompra(
                    nombreCategoria = nombreCategoria,
                    ingredientes = lista
                )
            }
            .sortedBy { it.nombreCategoria.lowercase() }

        // ACTUALIZAR RECYCLERVIEW

        adapterCategorias.actualizarLista(categorias)

        Log.d(
            "LISTA_COMPRAS",
            "Categorías mostradas: ${categorias.size}"
        )

    }

    // ==========================================
    // ACTUALIZAR ESTADO DE COMPRA
    // ==========================================

    private fun actualizarEstadoIngrediente(
        ingrediente: IngredienteListaCompra,
        nuevoEstado: Int
    ) {

        val lciId = ingrediente.lciId

        // Evitar solicitudes repetidas del mismo ingrediente.

        if (!actualizacionesEnCurso.add(lciId)) {
            return
        }

        lifecycleScope.launch {

            try {

                val request = ActualizarEstadoCompraRequest(
                    lciId = lciId,
                    comprado = nuevoEstado,
                    cliId = cliId
                )

                val respuesta = ApiClient.apiService
                    .actualizarEstadoCompra(request)

                if (!respuesta.success) {
                    mostrarMensaje(respuesta.message)
                    return@launch
                }

                Log.d(
                    "LISTA_COMPRAS",
                    "LCI_ID=$lciId actualizado a $nuevoEstado"
                )

                // Consultar nuevamente para reflejar el estado
                // confirmado por MariaDB.

                consultarIngredientes()

            } catch (e: Exception) {

                Log.e(
                    "LISTA_COMPRAS",
                    "Error al actualizar estado",
                    e
                )

                mostrarMensaje(
                    "No se pudo actualizar el ingrediente."
                )

            } finally {

                actualizacionesEnCurso.remove(lciId)
            }
        }
    }

    // ==========================================
    // MENSAJES
    // ==========================================

    private fun mostrarMensaje(mensaje: String) {

        Toast.makeText(
            this,
            mensaje,
            Toast.LENGTH_LONG
        ).show()
    }

// ==========================================
// GENERAR PDF DE LISTA DE COMPRAS
// ==========================================

    private fun descargarListaCompras() {

        binding.btnDescargarLista.isEnabled = false

        lifecycleScope.launch {

            try {

                mostrarMensaje("Preparando tu lista de compras...")

                // Consultar los ingredientes actualizados del servidor.
                val request = ListaCompraRequest(
                    plaId = plaId,
                    cliId = cliId
                )

                val respuesta = ApiClient.apiService
                    .listarIngredientesLista(request)

                if (!respuesta.success) {
                    mostrarMensaje(
                        respuesta.message ?: "No se pudo consultar la lista."
                    )
                    return@launch
                }

                val ingredientes = respuesta.ingredientes ?: emptyList()

                if (ingredientes.isEmpty()) {
                    mostrarMensaje("No hay ingredientes para generar el PDF.")
                    return@launch
                }

                // Generar el documento fuera del hilo principal.
                val generador = GeneradorPdfListaCompras(this@ListaDeComprasActivity)

                val documento = withContext(Dispatchers.Default) {
                    generador.generar(
                        ingredientes = ingredientes,
                        plaId = plaId
                    )
                }

                pdfPendiente = documento

                // Abrir selector para guardar el documento.
                guardarPdfLauncher.launch(
                    "Lista_de_compras_Planeador_$plaId.pdf"
                )

            } catch (e: Exception) {

                Log.e(
                    "PDF_LISTA_COMPRAS",
                    "Error al generar PDF",
                    e
                )

                mostrarMensaje(
                    "No se pudo generar el PDF: ${e.message}"
                )

            } finally {

                binding.btnDescargarLista.isEnabled = true
            }
        }
    }

// ==========================================
// GUARDAR PDF EN EL DISPOSITIVO
// ==========================================

    private fun guardarArchivoPdf(uri: Uri) {

        val archivo = pdfPendiente

        if (archivo == null) {
            mostrarMensaje("No se encontró el documento para guardar.")
            return
        }

        lifecycleScope.launch {

            try {

                withContext(Dispatchers.IO) {

                    val salida = contentResolver.openOutputStream(uri)
                        ?: throw Exception("No se pudo abrir el archivo.")

                    salida.use {
                        it.write(archivo)
                        it.flush()
                    }
                }

                pdfPendiente = null

                mostrarMensaje(
                    "¡Lista de compras guardada correctamente!"
                )

                Log.d(
                    "PDF_LISTA_COMPRAS",
                    "PDF guardado correctamente. PLA_ID=$plaId"
                )

            } catch (e: Exception) {

                Log.e(
                    "PDF_LISTA_COMPRAS",
                    "Error al guardar PDF",
                    e
                )

                mostrarMensaje(
                    "Error al guardar el PDF: ${e.message}"
                )

            } finally {
                pdfPendiente = null
            }
        }
    }

}

