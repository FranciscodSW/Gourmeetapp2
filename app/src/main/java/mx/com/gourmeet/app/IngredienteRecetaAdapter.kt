package mx.com.gourmeet.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import mx.com.gourmeet.app.data.models.IngredienteReceta
import mx.com.gourmeet.app.databinding.ItemIngredienteRecetaBinding

class IngredienteRecetaAdapter(
    private val lista: List<IngredienteReceta>
) : RecyclerView.Adapter<IngredienteRecetaAdapter.ViewHolder>() {

    inner class ViewHolder(
        private val binding: ItemIngredienteRecetaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(ingrediente: IngredienteReceta) {

            // Nombre del ingrediente
            binding.txtIngrediente.text =
                ingrediente.nombre

            // Cantidad recibida desde la API
            binding.txtcantidad.text =
                ingrediente.cantidad
                    ?.takeIf { it.isNotBlank() }
                    ?: "Cantidad no especificada"

            // Fotografía del ingrediente
            Glide.with(binding.root.context)
                .load(ingrediente.foto)
                .placeholder(R.drawable.gm_nav_pantry)
                .error(R.drawable.gm_nav_pantry)
                .into(binding.imgIngrediente)
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val binding = ItemIngredienteRecetaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return ViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        holder.bind(lista[position])
    }

    override fun getItemCount(): Int =
        lista.size
}