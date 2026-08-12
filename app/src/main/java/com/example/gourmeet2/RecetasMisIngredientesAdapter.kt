package com.example.gourmeet2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.gourmeet2.data.models.RecetaconFiltro
import com.example.gourmeet2.databinding.ItemRecetaSeleccionableBinding

class RecetasMisIngredientesAdapter(

    private val recetas: MutableList<RecetaconFiltro>,

    private val onRecetaClick: (RecetaconFiltro) -> Unit

) : RecyclerView.Adapter<RecetasMisIngredientesAdapter.ViewHolder>() {

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

        val receta =
            recetas[position]

        with(holder.binding) {

            // =========================
            // NOMBRE
            // =========================

            txtTitulo.text =
                receta.REC_NOMBRE


            // =========================
            // TIEMPO
            // =========================

            txtTiempo.text =
                receta.REC_TIEMPO_PREPARACION ?: "--"


            // =========================
            // DIFICULTAD
            // =========================

            txtNivel.text =
                receta.Dificultad ?: "--"


            // =========================
            // CATEGORIA
            // =========================

            txtTipo.text =
                receta.Categoria ?: "--"


            // =========================
            // COSTO
            // =========================

            txtCosto.text =
                receta.gasto?.let {

                    "$" + String.format(
                        "%.0f",
                        it
                    )

                } ?: "--"


            // =========================
            // COINCIDENCIA
            // =========================

            val coincidencias =
                receta.coincidencias ?: 0

            val total =
                receta.totalIngredientesBuscados ?: 0

            val porcentaje =
                if (total > 0) {

                    coincidencias * 100 / total

                } else {

                    0
                }

            txtCoincidencia.text =
                "$porcentaje%"


            // =========================
            // IMAGEN
            // =========================

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


            // =========================
            // INGREDIENTES
            // =========================

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


            // =========================
            // CLICK
            // =========================

            root.setOnClickListener {

                onRecetaClick(receta)

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