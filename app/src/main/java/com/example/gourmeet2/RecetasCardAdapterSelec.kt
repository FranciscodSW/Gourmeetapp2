package com.example.gourmeet2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.gourmeet2.data.models.RecetaconFiltro
import com.example.gourmeet2.databinding.ItemRecetaSeleccionableBinding

class RecetasCardAdapterSelec(

    // ==================================================
    // RECETAS QUE ESTE ADAPTER DEBE MOSTRAR
    // ==================================================

    private val recetas:
    MutableList<RecetaconFiltro> = mutableListOf(),

    // ==================================================
    // RECETAS QUE YA ESTÁN SELECCIONADAS
    // ==================================================

    private val recetasSeleccionadas:
    MutableList<RecetaconFiltro>,

    // ==================================================
    // CLICK NORMAL EN LA TARJETA
    // ==================================================

    private val onRecetaClick:
    ((RecetaconFiltro) -> Unit)? = null,

    // ==================================================
    // CLICK EN CÍRCULO ○ / ✓
    // ==================================================

    private val onSeleccionarReceta:
    ((RecetaconFiltro, Boolean) -> Unit)? = null

) : RecyclerView.Adapter<RecetasCardAdapterSelec.ViewHolder>() {


    // ==================================================
    // VIEW HOLDER
    // ==================================================

    inner class ViewHolder(

        val binding:
        ItemRecetaSeleccionableBinding

    ) : RecyclerView.ViewHolder(binding.root)


    // ==================================================
    // CREAR VIEW HOLDER
    // ==================================================

    override fun onCreateViewHolder(

        parent: ViewGroup,

        viewType: Int

    ): ViewHolder {

        val binding =
            ItemRecetaSeleccionableBinding.inflate(

                LayoutInflater.from(
                    parent.context
                ),

                parent,

                false

            )

        return ViewHolder(binding)
    }


    // ==================================================
    // BIND
    // ==================================================

    override fun onBindViewHolder(

        holder: ViewHolder,

        position: Int

    ) {

        val receta =
            recetas[position]


        with(holder.binding) {


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
            // COINCIDENCIA
            // ==========================================

            val coincidencias =
                receta.coincidencias ?: 0


            val totalBuscados =
                receta.totalIngredientesBuscados
                    ?: 0


            val porcentaje =
                if (totalBuscados > 0) {

                    coincidencias * 100 /
                            totalBuscados

                } else {

                    0

                }


            txtCoincidencia.text =
                "$porcentaje%"


            // ==========================================
            // IMAGEN DE LA RECETA
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
                    .into(
                        imgReceta
                    )

            } else {

                imgReceta.setImageResource(
                    R.drawable.ic_nuevo_icono
                )
            }


            // ==========================================
            // INGREDIENTES DE LA RECETA
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
            // COMPROBAR SELECCIÓN
            // ==========================================

            val seleccionada =
                estaSeleccionada(
                    receta
                )


            // ==========================================
            // MOSTRAR ○ O ✓
            // ==========================================

            if (seleccionada) {

                btnSeleccionar.setImageResource(
                    R.drawable.ic_check_white
                )

            } else {

                btnSeleccionar.setImageResource(
                    R.drawable.bg_circulo_seleccion
                )
            }


            // ==========================================
            // CLICK EN CÍRCULO
            // ==========================================

            btnSeleccionar.setOnClickListener {

                val seleccionadaActual =
                    estaSeleccionada(
                        receta
                    )


                // Si estaba seleccionada:
                // false -> quitar
                //
                // Si no estaba seleccionada:
                // true -> agregar

                onSeleccionarReceta?.invoke(

                    receta,

                    !seleccionadaActual

                )
            }


            // ==========================================
            // CLICK EN TARJETA
            // ==========================================

            root.setOnClickListener {

                onRecetaClick?.invoke(
                    receta
                )
            }
        }
    }


    // ==================================================
    // COMPROBAR SI UNA RECETA ESTÁ SELECCIONADA
    // ==================================================

    fun estaSeleccionada(

        receta: RecetaconFiltro

    ): Boolean {

        return recetasSeleccionadas.any {

            it.REC_ID ==
                    receta.REC_ID

        }
    }


    // ==================================================
    // CANTIDAD DE RECETAS
    // ==================================================

    override fun getItemCount(): Int {

        return recetas.size

    }


    // ==================================================
    // ACTUALIZAR RECETAS
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
    // LIMPIAR RECETAS
    // ==================================================

    fun limpiar() {

        recetas.clear()

        notifyDataSetChanged()
    }


    // ==================================================
    // ACTUALIZAR ESTADO DE SELECCIÓN
    // ==================================================

    fun actualizarSeleccionadas() {

        notifyDataSetChanged()
    }

}