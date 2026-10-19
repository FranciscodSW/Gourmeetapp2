package mx.com.gourmeet.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import mx.com.gourmeet.app.databinding.ItemNombreDeComidaAPrepararBinding


class AdapterNombreComida(
    private val recetas: MutableList<String>,
    private val onEliminar: ((Int) -> Unit)? = null
) : RecyclerView.Adapter<AdapterNombreComida.RecetaViewHolder>() {

    inner class RecetaViewHolder(
        val binding: ItemNombreDeComidaAPrepararBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecetaViewHolder {
        val binding = ItemNombreDeComidaAPrepararBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RecetaViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: RecetaViewHolder,
        position: Int
    ) {
        holder.binding.nombreComida.text = recetas[position]

        holder.binding.root.setOnLongClickListener {
            val posicion = holder.bindingAdapterPosition

            if (posicion != RecyclerView.NO_POSITION) {
                onEliminar?.invoke(posicion)
            }

            onEliminar != null
        }
    }

    override fun getItemCount(): Int = recetas.size

    fun actualizarRecetas(nuevasRecetas: List<String>) {
        recetas.clear()
        recetas.addAll(nuevasRecetas)
        notifyDataSetChanged()
    }

    fun agregarReceta(nombre: String) {
        recetas.add(nombre)
        notifyItemInserted(recetas.lastIndex)
    }

    fun eliminarReceta(posicion: Int) {
        if (posicion in recetas.indices) {
            recetas.removeAt(posicion)
            notifyItemRemoved(posicion)
        }
    }
}
