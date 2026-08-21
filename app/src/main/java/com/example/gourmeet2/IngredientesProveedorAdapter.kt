package com.example.gourmeet2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.gourmeet2.data.models.IngredienteProveedor
import com.example.gourmeet2.databinding.ItemIngredienteMiniBinding

class IngredientesProveedorAdapter(
    private val ingredientes: List<IngredienteProveedor>
) : RecyclerView.Adapter<IngredientesProveedorAdapter.ViewHolder>() {

    inner class ViewHolder(
        val binding:ItemIngredienteMiniBinding
    ) : RecyclerView.ViewHolder(binding.root)

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

        val ingrediente =
            ingredientes[position]

        // ==========================================
        // NOMBRE
        // ==========================================

        holder.binding.txtIngrediente.text =
            ingrediente.NOMBRE ?: "Ingrediente"


        // ==========================================
        // FOTO
        // ==========================================

        Glide.with(
            holder.itemView.context
        )
            .load(ingrediente.FOTO)
            .placeholder(
                R.drawable.logo_blanco_negro
            )
            .error(
                R.drawable.logo_blanco_negro
            )
            .centerCrop()
            .into(holder.binding.imgIngrediente)
    }

    override fun getItemCount(): Int =
        ingredientes.size
}