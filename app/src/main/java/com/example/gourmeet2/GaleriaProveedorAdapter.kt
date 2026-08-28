package com.example.gourmeet2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.gourmeet2.databinding.ItemGaleriaProveedorBinding

class GaleriaProveedorAdapter(
    private val imagenes: List<String?>
) : RecyclerView.Adapter<GaleriaProveedorAdapter.GaleriaViewHolder>() {


    // ==========================================
    // VIEW HOLDER
    // ==========================================

    inner class GaleriaViewHolder(
        val binding: ItemGaleriaProveedorBinding
    ) : RecyclerView.ViewHolder(binding.root)


    // ==========================================
    // CREAR VIEW HOLDER
    // ==========================================

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): GaleriaViewHolder {

        val binding =
            ItemGaleriaProveedorBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

        return GaleriaViewHolder(binding)
    }


    // ==========================================
    // MOSTRAR IMAGEN
    // ==========================================

    override fun onBindViewHolder(
        holder: GaleriaViewHolder,
        position: Int
    ) {

        val imagen =
            imagenes[position]

        Glide.with(
            holder.itemView.context
        )
            .load(imagen)
            .placeholder(
                R.drawable.ic_proximamente
            )
            .error(
                R.drawable.ic_proximamente
            )
            .centerCrop()
            .into(
                holder.binding.imgGaleriaProveedor1
            )
    }


    // ==========================================
    // CANTIDAD
    // ==========================================

    override fun getItemCount(): Int =
        imagenes.size
}