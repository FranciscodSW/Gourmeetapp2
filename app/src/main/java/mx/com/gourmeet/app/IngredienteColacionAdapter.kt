package mx.com.gourmeet.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import mx.com.gourmeet.app.data.models.BuscarIngredientes
import com.bumptech.glide.Glide

class IngredienteColacionAdapter(
    private val onSeleccionar: (BuscarIngredientes) -> Unit
) : RecyclerView.Adapter<IngredienteColacionAdapter.IngredienteViewHolder>() {

    private val ingredientes = mutableListOf<BuscarIngredientes>()
    private val seleccionados = mutableSetOf<Int>()

    inner class IngredienteViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val imagen: ImageView = itemView.findViewById(R.id.img)
        val texto: TextView = itemView.findViewById(R.id.txt)
        val circulo: View = itemView.findViewById(R.id.circuloAzul)
        init {
            itemView.setOnClickListener {
                val posicion = bindingAdapterPosition
                if (posicion == RecyclerView.NO_POSITION) return@setOnClickListener

                val ingrediente = ingredientes[posicion]

                if (seleccionados.contains(ingrediente.id)) {
                    seleccionados.remove(ingrediente.id)
                } else {
                    seleccionados.add(ingrediente.id)
                }
                notifyItemChanged(posicion)
                onSeleccionar(ingrediente)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): IngredienteViewHolder {
        val vista = LayoutInflater.from(parent.context).inflate(
            R.layout.item_seleccionado_con_circulo_azul,
            parent,
            false
        )
        return IngredienteViewHolder(vista)
    }


    override fun onBindViewHolder(
        holder: IngredienteViewHolder,
        position: Int
    ) {
        val ingrediente = ingredientes[position]

        holder.texto.text = ingrediente.nombre.trim()

        holder.circulo.visibility =
            if (ingrediente.id in seleccionados) {
                View.VISIBLE
            } else {
                View.GONE
            }

        val urlImagen = ingrediente.imagen_url
            ?.trim()
            ?.takeIf { it.startsWith("https://") || it.startsWith("http://") }
            ?.takeIf { !it.endsWith("/") }

        if (urlImagen != null) {
            Glide.with(holder.itemView)
                .load(urlImagen)
                .centerCrop()
                .placeholder(R.drawable.ic_ingredientes)
                .error(R.drawable.ic_ingredientes)
                .into(holder.imagen)
        } else {
            Glide.with(holder.itemView).clear(holder.imagen)
            holder.imagen.setImageResource(R.drawable.ic_ingredientes)
        }
    }


    override fun getItemCount(): Int = ingredientes.size

    fun actualizarLista(nuevaLista: List<BuscarIngredientes>) {
        ingredientes.clear()
        ingredientes.addAll(nuevaLista)
        notifyDataSetChanged()
    }

    fun limpiar() {
        ingredientes.clear()
        seleccionados.clear()
        notifyDataSetChanged()
    }
    fun actualizarSeleccion(idsSeleccionados: Set<Int>) {
        seleccionados.clear()
        seleccionados.addAll(idsSeleccionados)
        notifyDataSetChanged()
    }
}

