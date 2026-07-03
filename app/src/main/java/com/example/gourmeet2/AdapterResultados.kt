package com.example.gourmeet2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gourmeet2.data.models.RecetaconFiltro
import com.example.gourmeet2.data.models.SeccionResultados

class AdapterResultados(

    private val secciones: MutableList<SeccionResultados>,

    private val onClick: (RecetaconFiltro) -> Unit

) : RecyclerView.Adapter<AdapterResultados.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val titulo =
            itemView.findViewById<TextView>(R.id.txtCoincidencia)

        private val flecha =
            itemView.findViewById<ImageView>(R.id.btnToggleCoincidencia)

        private val rv =
            itemView.findViewById<RecyclerView>(R.id.rvCoincidencia)

        fun bind(seccion: SeccionResultados) {

            titulo.text = seccion.titulo

            rv.layoutManager =
                LinearLayoutManager(
                    itemView.context,
                    LinearLayoutManager.HORIZONTAL,
                    false
                )

            rv.adapter =
                CoincidenciaAdapter(
                    seccion.recetas.toMutableList(),
                    onClick
                )
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_resultados, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(secciones[position])
    }

    override fun getItemCount(): Int = secciones.size

    fun actualizar(lista: List<SeccionResultados>) {
        secciones.clear()
        secciones.addAll(lista)
        notifyDataSetChanged()
    }
}