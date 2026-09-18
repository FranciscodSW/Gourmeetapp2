package com.example.gourmeet2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gourmeet2.data.models.IngredienteAlacena

class CategoriaIngredientesAdapter(
    private var categorias: List<CategoriaIngredientes>,
    private val onIngredienteClick: (IngredienteAlacena) -> Unit
) : RecyclerView.Adapter<CategoriaIngredientesAdapter.ViewHolder>() {

    // =========================================================
    // MODELO DE UNA CATEGORÍA
    // =========================================================

    data class CategoriaIngredientes(
        val nombreCategoria: String,
        val ingredientes: List<IngredienteAlacena>
    )

    // =========================================================
    // VIEW HOLDER
    // =========================================================

    inner class ViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val nombreCategoria: TextView =
            itemView.findViewById(R.id.txtNombreCategoria)

        val recyclerIngredientes: RecyclerView =
            itemView.findViewById(R.id.rvIngredientesCategoria)
    }

    // =========================================================
    // CREAR VISTA
    // =========================================================

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.item_categoria_ingredientes,
                parent,
                false
            )

        return ViewHolder(view)
    }

    // =========================================================
    // MOSTRAR CATEGORÍA
    // =========================================================

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        val categoria = categorias[position]

        // -----------------------------------------
        // NOMBRE DE LA CATEGORÍA
        // -----------------------------------------

        holder.nombreCategoria.text =
            categoria.nombreCategoria

        // -----------------------------------------
        // RECYCLERVIEW HORIZONTAL
        // -----------------------------------------

        holder.recyclerIngredientes.layoutManager =
            LinearLayoutManager(
                holder.itemView.context,
                LinearLayoutManager.HORIZONTAL,
                false
            )

        // -----------------------------------------
        // ADAPTER DE INGREDIENTES
        // -----------------------------------------

        val adapterIngredientes =
            IngredienteAlacenaAdapter(
                categoria.ingredientes,
                onIngredienteClick
            )

        holder.recyclerIngredientes.adapter =
            adapterIngredientes
    }

    // =========================================================
    // CANTIDAD DE CATEGORÍAS
    // =========================================================

    override fun getItemCount(): Int {
        return categorias.size
    }

    // =========================================================
    // ACTUALIZAR CATEGORÍAS
    // =========================================================

    fun actualizarLista(
        nuevaLista: List<CategoriaIngredientes>
    ) {

        categorias = nuevaLista

        notifyDataSetChanged()
    }
}