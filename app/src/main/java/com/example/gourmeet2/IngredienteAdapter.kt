package com.example.gourmeet2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.gourmeet2.data.models.BuscarIngredientes

class IngredienteAdapter(
    private var lista: List<BuscarIngredientes>,
    private val onClick: (BuscarIngredientes) -> Unit
) : RecyclerView.Adapter<IngredienteAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val img = view.findViewById<ImageView>(R.id.img)
        val txt = view.findViewById<TextView>(R.id.txt)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ingrediente, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = lista[position]

        holder.txt.text = item.nombre

        Glide.with(holder.itemView)
            .load(item.imagen_url)
            .into(holder.img)

        holder.itemView.setOnClickListener {
            onClick(item)
        }
    }

    override fun getItemCount() = lista.size

    fun updateData(newList: List<BuscarIngredientes>) {
        lista = newList
        notifyDataSetChanged()
    }
}