package com.example.gourmeet2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.gourmeet2.data.models.RecetaconFiltro

class CoincidenciaAdapter(
    private val lista: MutableList<RecetaconFiltro>,
    private val onClick: ((RecetaconFiltro) -> Unit)? = null
) : RecyclerView.Adapter<CoincidenciaAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val txtNombre: TextView = itemView.findViewById(R.id.txtNombre)
        val imgReceta: ImageView = itemView.findViewById(R.id.imgReceta)

        fun bind(receta: RecetaconFiltro) {

            txtNombre.text = receta.REC_NOMBRE

            Glide.with(itemView.context)
                .load(receta.FotoReceta)
                .placeholder(R.drawable.ic_logo_circular)
                .error(R.drawable.ic_logo_circular)
                .into(imgReceta)

            itemView.setOnClickListener {
                onClick?.invoke(receta)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_receta, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(lista[position])
    }

    override fun getItemCount(): Int = lista.size

    fun actualizarDatos(nuevaLista: List<RecetaconFiltro>) {
        lista.clear()
        lista.addAll(nuevaLista)
        notifyDataSetChanged()
    }
}