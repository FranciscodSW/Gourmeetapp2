package mx.com.gourmeet.app

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import mx.com.gourmeet.app.databinding.ItemNombreDeComidaAPrepararBinding

data class ElementoComida(
    val nombre: String,
    val ecrId: Int? = null,
    val eciId: Int? = null
)

class AdapterNombreComida(
    private val elementos: MutableList<ElementoComida>,
    private val onEliminar: ((ElementoComida) -> Unit)? = null
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
        val elemento = elementos[position]
        val binding = holder.binding

        binding.nombreComida.text = elemento.nombre

        binding.btnEliminarComida.visibility =
            if (onEliminar != null) View.VISIBLE else View.GONE

        binding.btnEliminarComida.setOnClickListener {
            val posicion = holder.bindingAdapterPosition

            if (posicion != RecyclerView.NO_POSITION) {
                val elemento = elementos[posicion]

                Log.d(
                    "ELIMINAR_ELEMENTO",
                    "Nombre=${elemento.nombre}, " +
                            "ECR_ID=${elemento.ecrId}, " +
                            "ECI_ID=${elemento.eciId}"
                )

                onEliminar?.invoke(elemento)
            }
        }
    }

    override fun getItemCount(): Int = elementos.size

    fun actualizarElementos(nuevos: List<ElementoComida>) {
        elementos.clear()
        elementos.addAll(nuevos)
        notifyDataSetChanged()
    }
}

