package com.example.gourmeet2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.gourmeet2.data.models.RecetaconFiltro
import com.example.gourmeet2.databinding.ItemRecetaSeleccionableBinding

class RecetasSeleccionadasAdapter(
    private val recetas: MutableList<RecetaconFiltro> = mutableListOf(),
    private val onRecetaClick: ((RecetaconFiltro) -> Unit)? = null,
    private val onEliminarReceta: ((RecetaconFiltro) -> Unit)? = null
) : RecyclerView.Adapter<RecetasSeleccionadasAdapter.ViewHolder>() {

    inner class ViewHolder(
        val binding: ItemRecetaSeleccionableBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val binding =
            ItemRecetaSeleccionableBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

        return ViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        val receta = recetas[position]

        with(holder.binding) {

            txtTitulo.text =
                receta.REC_NOMBRE

            txtTiempo.text =
                receta.REC_TIEMPO_PREPARACION ?: "--"

            txtNivel.text =
                receta.Dificultad ?: "--"

            txtTipo.text =
                receta.Categoria ?: "--"

            txtCosto.text =
                receta.gasto?.let {

                    "$" + String.format(
                        "%.0f",
                        it
                    )

                } ?: "--"

            // ==========================================
            // IMAGEN
            // ==========================================

            if (!receta.FotoReceta.isNullOrEmpty()) {

                Glide.with(imgReceta.context)
                    .load(receta.FotoReceta)
                    .centerCrop()
                    .into(imgReceta)

            } else {

                imgReceta.setImageResource(
                    R.drawable.ic_nuevo_icono
                )
            }

            // ==========================================
            // INGREDIENTES
            // ==========================================

            rvIngredientes.apply {

                layoutManager =
                    LinearLayoutManager(
                        context,
                        LinearLayoutManager.HORIZONTAL,
                        false
                    )

                adapter =
                    IngredientesRecetaAdapter(
                        receta.Ingredientes
                    )

                setHasFixedSize(true)
            }

            // ==========================================
            // MOSTRAR ✓
            // ==========================================
            btnSeleccionar.setImageResource(
                R.drawable.ic_check_white
            )
            // ==========================================
            // QUITAR DE LA COLECCIÓN
            // ==========================================

            btnSeleccionar.setOnClickListener {

                onEliminarReceta?.invoke(
                    receta
                )

            }

            // ==========================================
            // ABRIR RECETA
            // ==========================================

            root.setOnClickListener {

                onRecetaClick?.invoke(
                    receta
                )

            }
        }
    }

    override fun getItemCount(): Int =
        recetas.size

    fun actualizar(
        nuevasRecetas: List<RecetaconFiltro>
    ) {

        recetas.clear()

        recetas.addAll(
            nuevasRecetas
        )

        notifyDataSetChanged()
    }

    fun limpiar() {

        recetas.clear()

        notifyDataSetChanged()
    }
}