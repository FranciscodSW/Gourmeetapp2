package com.example.gourmeet2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.gourmeet2.data.models.BuscarIngredientes

class SeleccionadosAdapter(
    private val lista: MutableList<BuscarIngredientes>,
    private val onRemove: (BuscarIngredientes) -> Unit
) : RecyclerView.Adapter<SeleccionadosAdapter.VH>() {
    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val img = view.findViewById<ImageView>(R.id.img)
        val txt = view.findViewById<TextView>(R.id.txt)
        val btnEliminar = view.findViewById<ImageView>(R.id.btnEliminar)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_seleccionado, parent, false)
        return VH(view)
    }
    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = lista[position]
        holder.txt.text = item.nombre
        Glide.with(holder.itemView)
            .load(item.imagen_url)
            .into(holder.img)
        holder.btnEliminar.setOnClickListener {
            onRemove(item)
        }
    }
    override fun getItemCount() = lista.size
}
