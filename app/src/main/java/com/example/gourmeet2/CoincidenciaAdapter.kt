package com.example.gourmeet2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.gourmeet2.data.models.RecetaconFiltro

class CoincidenciaAdapter(
    private val lista: MutableList<RecetaconFiltro>,
    private val onClick: ((RecetaconFiltro) -> Unit)? = null
) : RecyclerView.Adapter<CoincidenciaAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtTitulo =
            itemView.findViewById<TextView>(R.id.txtTitulo)

        val imgReceta =
            itemView.findViewById<ImageView>(R.id.imgReceta)

        val txtTiempo =
            itemView.findViewById<TextView>(R.id.txtTiempo)

        val txtNivel =
            itemView.findViewById<TextView>(R.id.txtNivel)

        val txtCosto =
            itemView.findViewById<TextView>(R.id.txtCosto)

        val txtTipo =
            itemView.findViewById<TextView>(R.id.txtTipo)

        val rvIngredientes =
            itemView.findViewById<RecyclerView>(R.id.rvIngredientes)

        fun bind(receta: RecetaconFiltro) {

            txtTitulo.text = receta.REC_NOMBRE

            txtTiempo.text =
                "${receta.REC_TIEMPO_PREPARACION ?: "--"} minutos"

            txtNivel.text =
                receta.Dificultad ?: "--"

            txtTipo.text =
                receta.Categoria ?: "--"

            txtCosto.text = "--"

            Glide.with(itemView.context)
                .load(receta.FotoReceta)
                .placeholder(R.drawable.ic_logo_circular)
                .error(R.drawable.ic_logo_circular)
                .into(imgReceta)

            rvIngredientes.layoutManager =
                LinearLayoutManager(
                    itemView.context,
                    LinearLayoutManager.HORIZONTAL,
                    false
                )

            // ✅ Optimización
            rvIngredientes.setHasFixedSize(true)
            rvIngredientes.isNestedScrollingEnabled = false

            // ✅ Separación entre ingredientes
            if (rvIngredientes.itemDecorationCount == 0) {
                rvIngredientes.addItemDecoration(
                    SpaceItemDecoration(12)
                )
            }

            // ✅ Adapter
            rvIngredientes.adapter =
                IngredientesMiniAdapter(
                    receta.Ingredientes ?: emptyList()
                )

            itemView.setOnClickListener {
                onClick?.invoke(receta)
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_receta_new, parent, false)


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