package com.example.gourmeet2.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.gourmeet2.R
import com.example.gourmeet2.data.models.BuscarIngredientes
import com.example.gourmeet2.databinding.ItemIngredienteMiniBinding

class IngredienteMiniAdapter(
    private var ingredientes: List<BuscarIngredientes>,
    private val onIngredienteSeleccionado: (BuscarIngredientes) -> Unit
) : RecyclerView.Adapter<IngredienteMiniAdapter.IngredienteViewHolder>() {

    inner class IngredienteViewHolder(
        private val binding: ItemIngredienteMiniBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(ingrediente: BuscarIngredientes) {

            binding.txtIngrediente.text =
                ingrediente.nombre

            // Imagen del ingrediente
            if (!ingrediente.imagen_url.isNullOrEmpty()) {

                Glide.with(binding.imgIngrediente.context)
                    .load(ingrediente.imagen_url)
                    .placeholder(R.drawable.ic_ingredientes)
                    .error(R.drawable.ic_ingredientes)
                    .into(binding.imgIngrediente)

            } else {

                binding.imgIngrediente.setImageResource(
                    R.drawable.ic_ingredientes
                )
            }


            // Seleccionar ingrediente
            binding.root.setOnClickListener {

                onIngredienteSeleccionado(
                    ingrediente
                )
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): IngredienteViewHolder {

        val binding =
            ItemIngredienteMiniBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

        return IngredienteViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: IngredienteViewHolder,
        position: Int
    ) {

        holder.bind(
            ingredientes[position]
        )
    }

    override fun getItemCount(): Int =
        ingredientes.size


    fun actualizarLista(
        nuevaLista: List<BuscarIngredientes>
    ) {

        ingredientes = nuevaLista

        notifyDataSetChanged()
    }
}