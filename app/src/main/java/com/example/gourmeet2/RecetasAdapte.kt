package com.example.gourmeet2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.gourmeet2.data.models.BuscarRecetas
import com.example.gourmeet2.databinding.ItemRecetaBinding

class RecetasAdapter(
    private val lista: List<BuscarRecetas>,
    private val onClick: (BuscarRecetas) -> Unit
) : RecyclerView.Adapter<RecetasAdapter.VH>() {

    inner class VH(val binding: ItemRecetaBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemRecetaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = lista[position]
        holder.binding.txtNombre.text = item.nombre
        Glide.with(holder.itemView)
            .load(item.foto)
            .into(holder.binding.imgReceta)

        holder.itemView.setOnClickListener {
            onClick(item)
        }
    }

    override fun getItemCount() = lista.size
}