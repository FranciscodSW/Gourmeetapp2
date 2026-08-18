package com.example.gourmeet2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.gourmeet2.data.models.RecetaconFiltro
import com.example.gourmeet2.data.models.ResultadoBusqueda
import com.example.gourmeet2.databinding.ItemResultadoBusquedaBinding
import com.example.gourmeet2.databinding.ItemResultadosRecetasBinding
import com.example.gourmeet2.databinding.ItemTituloBusquedaBinding

class BusquedaColeccionAdapter(

    private val lista: MutableList<ResultadoBusqueda>,
    private val recetasSeleccionadas: MutableList<RecetaconFiltro>,

    private val listener: OnResultadoClickListener

) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {


    // ==================================================
    // LISTENER
    // ==================================================

    interface OnResultadoClickListener {

        fun onIngredienteClick(
            resultado: ResultadoBusqueda
        )

        fun onRecetaClick(
            receta: RecetaconFiltro
        )

        fun onCategoriaClick(
            resultado: ResultadoBusqueda
        )
        fun onSeleccionarReceta(
            receta: RecetaconFiltro,
            seleccionada: Boolean
        )

    }


    // ==================================================
    // VIEW HOLDER TÍTULO
    // ==================================================

    inner class TituloViewHolder(

        val binding: ItemTituloBusquedaBinding

    ) : RecyclerView.ViewHolder(binding.root)


    // ==================================================
    // VIEW HOLDER RESULTADO NORMAL
    // ==================================================

    inner class ResultadoViewHolder(

        val binding: ItemResultadoBusquedaBinding

    ) : RecyclerView.ViewHolder(binding.root)


    // ==================================================
    // VIEW HOLDER RECETAS
    // ==================================================

    inner class RecetasViewHolder(

        val binding: ItemResultadosRecetasBinding

    ) : RecyclerView.ViewHolder(binding.root)


    // ==================================================
    // TIPO DE ELEMENTO
    // ==================================================

    override fun getItemViewType(
        position: Int
    ): Int {

        return lista[position].tipo

    }


    // ==================================================
    // CREAR VIEW HOLDER
    // ==================================================

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {

        return when (viewType) {


            // ------------------------------------------
            // TÍTULO
            // ------------------------------------------

            ResultadoBusqueda.TITULO -> {

                val binding =
                    ItemTituloBusquedaBinding.inflate(

                        LayoutInflater.from(
                            parent.context
                        ),

                        parent,

                        false

                    )

                TituloViewHolder(binding)

            }


            // ------------------------------------------
            // RECETAS HORIZONTALES
            // ------------------------------------------

            ResultadoBusqueda.RECETAS_HORIZONTAL -> {

                val binding =
                    ItemResultadosRecetasBinding.inflate(

                        LayoutInflater.from(
                            parent.context
                        ),

                        parent,

                        false

                    )

                RecetasViewHolder(binding)

            }


            // ------------------------------------------
            // INGREDIENTES / CATEGORÍAS
            // ------------------------------------------

            else -> {

                val binding =
                    ItemResultadoBusquedaBinding.inflate(

                        LayoutInflater.from(
                            parent.context
                        ),

                        parent,

                        false

                    )

                ResultadoViewHolder(binding)

            }

        }

    }


    // ==================================================
    // CANTIDAD
    // ==================================================

    override fun getItemCount(): Int {

        return lista.size

    }


    // ==================================================
    // BIND
    // ==================================================

    override fun onBindViewHolder(

        holder: RecyclerView.ViewHolder,

        position: Int

    ) {

        val item =
            lista[position]


        when (holder) {


            // ==========================================
            // TÍTULO
            // ==========================================

            is TituloViewHolder -> {

                holder.binding.txtTitulo.text =
                    item.titulo

            }


            // ==========================================
// RECETAS HORIZONTALES
// ==========================================

            is RecetasViewHolder -> {

                holder.binding.txtTituloRecetas.text =
                    item.titulo

                holder.binding.recyclerRecetas.apply {

                    layoutManager =
                        LinearLayoutManager(
                            holder.itemView.context,
                            LinearLayoutManager.HORIZONTAL,
                            false
                        )

                    adapter =
                        RecetasCardAdapterSelec(

                            item.recetas.toMutableList(),

                            recetasSeleccionadas,

                            // CLICK NORMAL DE LA RECETA
                            onRecetaClick = { receta ->

                                listener.onRecetaClick(
                                    receta
                                )
                            },

                            // SELECCIÓN ○ / ✓
                            onSeleccionarReceta = { receta, seleccionada ->

                                listener.onSeleccionarReceta(
                                    receta,
                                    seleccionada
                                )
                            }
                        )
                }
            }


            // ==========================================
            // RESULTADO NORMAL
            // ==========================================

            is ResultadoViewHolder -> {

                holder.binding.txtNombre.text =
                    item.nombre


                // --------------------------------------
                // ICONO
                // --------------------------------------

                when (item.tipo) {

                    ResultadoBusqueda.INGREDIENTE -> {

                        Glide.with(holder.itemView.context)
                            .load(item.foto)
                            .placeholder(R.drawable.ic_ingredientes)
                            .error(R.drawable.ic_ingredientes)
                            .into(holder.binding.imgIcono)

                    }

                    ResultadoBusqueda.CATEGORIA -> {

                        holder.binding.imgIcono.setImageResource(
                            R.drawable.ic_gorrito
                        )

                    }

                }


                // --------------------------------------
                // CLICK
                // --------------------------------------

                holder.itemView.setOnClickListener {

                    when (item.tipo) {


                        ResultadoBusqueda.INGREDIENTE -> {

                            listener.onIngredienteClick(
                                item
                            )

                        }


                        ResultadoBusqueda.CATEGORIA -> {

                            listener.onCategoriaClick(
                                item
                            )

                        }

                    }

                }

            }

        }

    }


    // ==================================================
    // ACTUALIZAR
    // ==================================================

    fun actualizar(

        nuevaLista: List<ResultadoBusqueda>

    ) {

        lista.clear()

        lista.addAll(
            nuevaLista
        )

        notifyDataSetChanged()

    }

}