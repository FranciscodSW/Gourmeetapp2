package com.example.gourmeet2

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.gourmeet2.data.models.IngredienteProveedor
import com.example.gourmeet2.databinding.ItemIngredienteProveedorBinding

class IngredientesPatrocinadosProveedorAdapter(
    private val ingredientes: List<IngredienteProveedor>
) : RecyclerView.Adapter<IngredientesPatrocinadosProveedorAdapter.ViewHolder>() {

    inner class ViewHolder(
        val binding: ItemIngredienteProveedorBinding
    ) : RecyclerView.ViewHolder(binding.root)


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val binding =
            ItemIngredienteProveedorBinding.inflate(
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

        holder.binding.txtNombreIngrediente.text =
            ingrediente.NOMBRE
                ?: "Ingrediente"


        // ==========================================
        // DESCRIPCIÓN
        // ==========================================

        holder.binding.txtDescripcionIngrediente.text =
            ingrediente.DESCRIPCION
                ?: ""


        // ==========================================
        // FOTO
        // ==========================================

        Glide.with(
            holder.itemView.context
        )
            .load(
                ingrediente.FOTO
            )
            .placeholder(
                R.drawable.logo_blanco_negro
            )
            .error(
                R.drawable.logo_blanco_negro
            )
            .centerCrop()
            .into(
                holder.binding.imgIngrediente)
    }


    override fun getItemCount(): Int =
        ingredientes.size
}