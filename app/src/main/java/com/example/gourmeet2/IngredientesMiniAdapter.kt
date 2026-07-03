package com.example.gourmeet2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.gourmeet2.data.models.IngredienteReceta
import com.example.gourmeet2.databinding.ItemIngredienteMiniBinding

class IngredientesMiniAdapter(
    private val lista: List<IngredienteReceta>
) : RecyclerView.Adapter<IngredientesMiniAdapter.ViewHolder>() {

    inner class ViewHolder(
        private val binding: ItemIngredienteMiniBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(ingrediente: IngredienteReceta) {

            binding.txtIngrediente.text =
                ingrediente.nombre

            Glide.with(binding.root.context)
                .load(ingrediente.foto)
                .placeholder(R.drawable.ic_logo_circular)
                .error(R.drawable.ic_logo_circular)
                .into(binding.imgIngrediente)

        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val binding =
            ItemIngredienteMiniBinding.inflate(
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

    override fun getItemCount(): Int = lista.size
}