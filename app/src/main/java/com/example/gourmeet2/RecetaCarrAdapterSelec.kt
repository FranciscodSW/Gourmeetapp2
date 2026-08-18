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

class RecetaCarrAdapterSelec(

    private val recetas:
    MutableList<RecetaconFiltro> = mutableListOf(),

    private val onRecetaClick:
    ((RecetaconFiltro) -> Unit)? = null

) : RecyclerView.Adapter<RecetaCarrAdapterSelec.ViewHolder>() {


    // ==================================================
    // VIEW HOLDER
    // ==================================================

    inner class ViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        private val txtTitulo =
            itemView.findViewById<TextView>(
                R.id.txtTitulo
            )

        private val imgReceta =
            itemView.findViewById<ImageView>(
                R.id.imgReceta
            )

        private val txtTiempo =
            itemView.findViewById<TextView>(
                R.id.txtTiempo
            )

        private val txtNivel =
            itemView.findViewById<TextView>(
                R.id.txtNivel
            )

        private val txtCosto =
            itemView.findViewById<TextView>(
                R.id.txtCosto
            )

        private val txtTipo =
            itemView.findViewById<TextView>(
                R.id.txtTipo
            )

        private val rvIngredientes =
            itemView.findViewById<RecyclerView>(
                R.id.rvIngredientes
            )


        fun bind(
            receta: RecetaconFiltro
        ) {

            // ==========================================
            // NOMBRE
            // ==========================================

            txtTitulo.text =
                receta.REC_NOMBRE


            // ==========================================
            // TIEMPO
            // ==========================================

            txtTiempo.text =
                receta.REC_TIEMPO_PREPARACION
                    ?: "--"


            // ==========================================
            // DIFICULTAD
            // ==========================================

            txtNivel.text =
                receta.Dificultad
                    ?: "--"


            // ==========================================
            // CATEGORÍA
            // ==========================================

            txtTipo.text =
                receta.Categoria
                    ?: "--"


            // ==========================================
            // COSTO
            // ==========================================

            txtCosto.text =
                receta.gasto?.let {

                    "$" +
                            String.format(
                                "%.0f",
                                it
                            )

                } ?: "--"


            // ==========================================
            // IMAGEN
            // ==========================================

            if (
                !receta.FotoReceta.isNullOrEmpty()
            ) {

                Glide.with(
                    imgReceta.context
                )
                    .load(
                        receta.FotoReceta
                    )
                    .centerCrop()
                    .placeholder(
                        R.drawable.ic_logo_circular
                    )
                    .error(
                        R.drawable.ic_logo_circular
                    )
                    .into(
                        imgReceta
                    )

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

                isNestedScrollingEnabled = false

            }


            // ==========================================
            // CLICK RECETA
            // ==========================================

            itemView.setOnClickListener {

                onRecetaClick?.invoke(
                    receta
                )
            }
        }
    }


    // ==================================================
    // CREAR VIEW HOLDER
    // ==================================================

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val view =
            LayoutInflater.from(
                parent.context
            ).inflate(
                R.layout.item_receta_new,
                parent,
                false
            )

        return ViewHolder(view)
    }


    // ==================================================
    // BIND
    // ==================================================

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        holder.bind(
            recetas[position]
        )
    }


    // ==================================================
    // CANTIDAD
    // ==================================================

    override fun getItemCount(): Int {

        return recetas.size
    }


    // ==================================================
    // ACTUALIZAR
    // ==================================================

    fun actualizar(
        nuevasRecetas: List<RecetaconFiltro>
    ) {

        recetas.clear()

        recetas.addAll(
            nuevasRecetas
        )

        notifyDataSetChanged()
    }


    // ==================================================
    // LIMPIAR
    // ==================================================

    fun limpiar() {

        recetas.clear()

        notifyDataSetChanged()
    }
}