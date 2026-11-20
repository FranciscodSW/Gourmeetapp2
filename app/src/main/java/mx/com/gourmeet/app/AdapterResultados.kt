package mx.com.gourmeet.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import mx.com.gourmeet.app.data.models.precioVisible
import mx.com.gourmeet.app.data.models.IngredienteProveedor
import mx.com.gourmeet.app.data.models.Proveedor
import mx.com.gourmeet.app.data.models.RecetaconFiltro
import mx.com.gourmeet.app.data.models.SeccionResultados

data class ProductoRecomendado(val ingrediente: IngredienteProveedor, val proveedor: Proveedor)

class AdapterResultados(
    private val onClick: (RecetaconFiltro) -> Unit,
    private val onProductoClick: (Proveedor) -> Unit
) : RecyclerView.Adapter<AdapterResultados.ViewHolder>() {
    private var secciones = emptyList<SeccionResultados>()
    private var productos = emptyList<ProductoRecomendado>()
    private var mostrarProductos = false
    private val posicionProductos get() = if (mostrarProductos && productos.isNotEmpty()) minOf(2, secciones.size) else -1

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titulo: TextView = itemView.findViewById(R.id.txtCoincidencia)
        val flecha: ImageView = itemView.findViewById(R.id.btnToggleCoincidencia)
        val lista: RecyclerView = itemView.findViewById<RecyclerView>(R.id.rvCoincidencia).apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        }

        fun mostrar(tituloSeccion: String, items: RecyclerView.Adapter<*>) {
            titulo.text = tituloSeccion
            lista.adapter = items
            lista.visibility = View.VISIBLE
            flecha.rotation = 0f
            flecha.contentDescription = "Ocultar ${tituloSeccion.lowercase()}"
            flecha.setOnClickListener {
                val mostrar = lista.visibility != View.VISIBLE
                lista.visibility = if (mostrar) View.VISIBLE else View.GONE
                flecha.rotation = if (mostrar) 0f else -90f
                flecha.contentDescription = "${if (mostrar) "Ocultar" else "Mostrar"} ${tituloSeccion.lowercase()}"
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_resultados, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val indice = posicionProductos
        if (position == indice) {
            holder.mostrar("Productos de nuestros proveedores", ProductosAdapter(productos, onProductoClick))
        } else {
            val seccion = secciones[if (indice >= 0 && position > indice) position - 1 else position]
            holder.mostrar(seccion.titulo, CoincidenciaAdapter(seccion.recetas.toMutableList(), onClick))
        }
    }

    override fun getItemCount(): Int = secciones.size + if (posicionProductos >= 0) 1 else 0

    fun actualizar(lista: List<SeccionResultados>, incluirProductos: Boolean = false) {
        secciones = lista
        mostrarProductos = incluirProductos
        notifyDataSetChanged()
    }

    fun actualizarProductos(lista: List<ProductoRecomendado>) {
        productos = lista
        if (mostrarProductos) notifyDataSetChanged()
    }

    private class ProductosAdapter(
        private val productos: List<ProductoRecomendado>,
        private val onClick: (Proveedor) -> Unit
    ) : RecyclerView.Adapter<ProductosAdapter.ProductoHolder>() {
        class ProductoHolder(view: View) : RecyclerView.ViewHolder(view) {
            val nombre: TextView = view.findViewById(R.id.nombreProductoCercano)
            val tienda: TextView = view.findViewById(R.id.tiendaProductoCercano)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ProductoHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_producto_cercano, parent, false))

        override fun onBindViewHolder(holder: ProductoHolder, position: Int) {
            val producto = productos[position]
            holder.nombre.text = producto.ingrediente.NOMBRE
            holder.tienda.text = listOfNotNull(producto.proveedor.Pro_nombre, producto.ingrediente.precioVisible()).joinToString("\n")
            holder.itemView.setOnClickListener { onClick(producto.proveedor) }
        }

        override fun getItemCount() = productos.size
    }
}
