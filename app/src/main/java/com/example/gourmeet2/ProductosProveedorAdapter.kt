package com.example.gourmeet2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gourmeet2.data.models.SeccionProductosProveedor
import com.example.gourmeet2.databinding.ItemSeccionProductosProveedorBinding

class ProductosProveedorAdapter(
    private val secciones: List<SeccionProductosProveedor>,

    // ==========================================
    // CLICK EN GUARDAR PROVEEDOR
    // ==========================================

    private val onGuardarProveedor:
        (SeccionProductosProveedor, Int) -> Unit,

    // ==========================================
    // CLICK EN RECETA
    // ==========================================

    private val onRecetaClick:
        (Int) -> Unit

) : RecyclerView.Adapter<ProductosProveedorAdapter.ViewHolder>() {


    // ==========================================
    // ESTADO DE CADA SECCIÓN
    // ==========================================

    private val estadosGuardado =
        mutableMapOf<Int, Boolean>()


    // ==========================================
    // VIEW HOLDER
    // ==========================================

    inner class ViewHolder(
        val binding: ItemSeccionProductosProveedorBinding
    ) : RecyclerView.ViewHolder(binding.root)


    // ==========================================
    // CREAR VIEW HOLDER
    // ==========================================

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val binding =
            ItemSeccionProductosProveedorBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

        return ViewHolder(binding)
    }


    // ==========================================
    // BIND
    // ==========================================

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        val seccion =
            secciones[position]


        // ==========================================
        // TÍTULO
        // ==========================================

        holder.binding.txtTituloSeccion.text =
            seccion.titulo


        // ==========================================
        // ESTADO DEL BOTÓN GUARDAR
        // ==========================================

        val guardado =
            estadosGuardado[position] ?: false


        holder.binding.btnGuardarProveedorSeccion
            .setImageResource(

                if (guardado) {

                    R.drawable.ic_guardar_on

                } else {

                    R.drawable.ic_guardar
                }
            )


        // ==========================================
        // CLICK GUARDAR
        // ==========================================

        holder.binding.btnGuardarProveedorSeccion
            .setOnClickListener {

                onGuardarProveedor(
                    seccion,
                    position
                )
            }


        // ==========================================
        // CONTENIDO DE LA SECCIÓN
        // ==========================================

        if (!seccion.esRecetas) {

            // ======================================
            // INGREDIENTES
            // ======================================

            holder.binding.rvIngredientesSeccion.visibility =
                View.VISIBLE

            holder.binding.rvRecetasSeccion.visibility =
                View.GONE


            val adapterIngredientes =
                IngredientesPatrocinadosProveedorAdapter(
                    seccion.ingredientes
                )


            holder.binding.rvIngredientesSeccion.apply {

                layoutManager =
                    GridLayoutManager(
                        context,
                        2
                    )

                adapter =
                    adapterIngredientes

                setHasFixedSize(false)

                isNestedScrollingEnabled =
                    false

                overScrollMode =
                    View.OVER_SCROLL_NEVER
            }


        } else {

            // ======================================
            // RECETAS
            // ======================================

            holder.binding.rvIngredientesSeccion.visibility =
                View.GONE

            holder.binding.rvRecetasSeccion.visibility =
                View.VISIBLE


            // ======================================
            // ADAPTER DE RECETAS
            // ======================================

            val adapterRecetas =
                RecetaCarrAdapterSelec(

                    seccion.recetas.toMutableList()

                ) { receta ->

                    // ==================================
                    // OBTENER ID DE LA RECETA
                    // ==================================

                    val recetaId =
                        receta.REC_ID


                    // ==================================
                    // VALIDAR ID
                    // ==================================

                    if (recetaId <= 0) {

                        return@RecetaCarrAdapterSelec
                    }


                    // ==================================
                    // ENVIAR ID AL MENU PRINCIPAL
                    // ==================================

                    onRecetaClick(
                        recetaId
                    )
                }


            // ======================================
            // CONFIGURAR RECYCLER DE RECETAS
            // ======================================

            holder.binding.rvRecetasSeccion.apply {

                layoutManager =
                    GridLayoutManager(
                        context,
                        2
                    )

                adapter =
                    adapterRecetas

                setHasFixedSize(false)

                isNestedScrollingEnabled =
                    false

                overScrollMode =
                    View.OVER_SCROLL_NEVER
            }
        }
    }


    // ==========================================
    // CANTIDAD DE SECCIONES
    // ==========================================

    override fun getItemCount(): Int =
        secciones.size


    // ==========================================
    // ACTUALIZAR ESTADO DE UNA SECCIÓN
    // ==========================================

    fun actualizarEstadoGuardado(
        position: Int,
        guardado: Boolean
    ) {

        estadosGuardado[position] =
            guardado

        notifyItemChanged(position)
    }


    // ==========================================
    // ACTUALIZAR COLECCIONES GUARDADAS
    // ==========================================

    fun actualizarColeccionesGuardadas(
        coleccionesGuardadas: Set<String>
    ) {

        estadosGuardado.clear()


        secciones.forEachIndexed { position, seccion ->

            val nombreColeccion =
                seccion.nombreColeccion
                    ?.trim()
                    .orEmpty()


            val guardado =
                coleccionesGuardadas.any { nombreGuardado ->

                    nombreGuardado
                        .trim()
                        .equals(
                            nombreColeccion,
                            ignoreCase = true
                        )
                }


            estadosGuardado[position] =
                guardado
        }


        notifyDataSetChanged()
    }
}