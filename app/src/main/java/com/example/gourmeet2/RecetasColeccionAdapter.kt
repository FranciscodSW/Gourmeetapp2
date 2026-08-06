package com.example.gourmeet2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.gourmeet2.data.models.RecetaColeccion

class RecetasColeccionAdapter(

    private val recetas: MutableList<RecetaColeccion>,

    private val listener: OnRecetaClickListener

) : RecyclerView.Adapter<RecetasColeccionAdapter.ViewHolder>() {

    interface OnRecetaClickListener {

        fun onRecetaClick(
            receta: RecetaColeccion
        )

    }

    inner class ViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        private val txtTitulo =
            itemView.findViewById<TextView>(R.id.txtTitulo)

        private val imgReceta =
            itemView.findViewById<ImageView>(R.id.imgReceta)

        private val txtTiempo =
            itemView.findViewById<TextView>(R.id.txtTiempo)

        private val txtNivel =
            itemView.findViewById<TextView>(R.id.txtNivel)

        private val txtCosto =
            itemView.findViewById<TextView>(R.id.txtCosto)

        private val txtTipo =
            itemView.findViewById<TextView>(R.id.txtTipo)

        private val rvIngredientes =
            itemView.findViewById<RecyclerView>(R.id.rvIngredientes)

        fun bind(
            receta: RecetaColeccion
        ) {

            txtTitulo.text = receta.REC_NOMBRE

            txtTiempo.text =
                receta.REC_TIEMPO_PREPARACION ?: "--"

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

            rvIngredientes.apply {

                layoutManager = LinearLayoutManager(
                    itemView.context,
                    LinearLayoutManager.HORIZONTAL,
                    false
                )

                setHasFixedSize(true)

                isNestedScrollingEnabled = false

                if (itemDecorationCount == 0) {
                    addItemDecoration(
                        SpaceItemDecoration(12)
                    )
                }

                adapter = IngredientesMiniAdapter(
                    receta.Ingredientes
                )

            }

            itemView.setOnClickListener {

                listener.onRecetaClick(receta)

            }

        }

    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val view = LayoutInflater.from(parent.context)
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

        holder.bind(recetas[position])

    }

    override fun getItemCount(): Int {

        return recetas.size

    }

    fun actualizar(
        nuevasRecetas: List<RecetaColeccion>
    ) {

        recetas.clear()

        recetas.addAll(nuevasRecetas)

        notifyDataSetChanged()

    }

}