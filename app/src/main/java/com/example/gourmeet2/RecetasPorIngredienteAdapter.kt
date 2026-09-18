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

class RecetasPorIngredienteAdapter(
    private var recetas: List<RecetaconFiltro>,
    private val onRecetaClick: (RecetaconFiltro) -> Unit
) : RecyclerView.Adapter<RecetasPorIngredienteAdapter.ViewHolder>() {

    inner class ViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        val imgReceta: ImageView =
            itemView.findViewById(R.id.imgReceta)

        val txtTitulo: TextView =
            itemView.findViewById(R.id.txtTitulo)

        val txtTiempo: TextView =
            itemView.findViewById(R.id.txtTiempo)

        val txtNivel: TextView =
            itemView.findViewById(R.id.txtNivel)

        val txtCosto: TextView =
            itemView.findViewById(R.id.txtCosto)

        val txtTipo: TextView =
            itemView.findViewById(R.id.txtTipo)

        val txtCoincidencia: TextView =
            itemView.findViewById(R.id.txtCoincidencia)

        val rvIngredientes: RecyclerView =
            itemView.findViewById(R.id.rvIngredientes)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val view =
            LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.item_receta_new,
                    parent,
                    false
                )

        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        val receta = recetas[position]

        // =========================================
        // NOMBRE
        // =========================================

        holder.txtTitulo.text =
            receta.REC_NOMBRE

        // =========================================
        // TIEMPO
        // =========================================

        holder.txtTiempo.text =
            receta.REC_TIEMPO_PREPARACION
                ?: "--"

        // =========================================
        // DIFICULTAD
        // =========================================

        holder.txtNivel.text =
            receta.Dificultad
                ?: "--"

        // =========================================
        // COSTO
        // =========================================

        holder.txtCosto.text =
            "$" + String.format(
                "%.0f",
                receta.gasto ?: 0.0
            )

        // =========================================
        // CATEGORÍA
        // =========================================

        holder.txtTipo.text =
            receta.Categoria
                ?: "--"

        // =========================================
        // COINCIDENCIA
        // =========================================

        val coincidencias =
            receta.coincidencias ?: 0

        val total =
            receta.totalIngredientesBuscados
                ?: 1

        val porcentaje =
            ((coincidencias.toDouble() / total) * 100)
                .toInt()

        holder.txtCoincidencia.text =
            "$porcentaje%"

        // =========================================
        // IMAGEN
        // =========================================

        Glide.with(holder.itemView.context)
            .load(receta.FotoReceta)
            .placeholder(R.drawable.ic_ingredientes)
            .error(R.drawable.ic_ingredientes)
            .into(holder.imgReceta)

        // =========================================
        // INGREDIENTES
        // =========================================

        holder.rvIngredientes.layoutManager =
            LinearLayoutManager(
                holder.itemView.context,
                LinearLayoutManager.HORIZONTAL,
                false
            )

        holder.rvIngredientes.adapter =
            IngredientesRecetaAdapter(
                receta.Ingredientes
            )

        // =========================================
        // CLICK
        // =========================================

        holder.itemView.setOnClickListener {

            onRecetaClick(receta)
        }
    }

    override fun getItemCount(): Int =
        recetas.size

    fun actualizarLista(
        nuevaLista: List<RecetaconFiltro>
    ) {

        recetas = nuevaLista

        notifyDataSetChanged()
    }
}