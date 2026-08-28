package com.example.gourmeet2

import android.location.Location
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gourmeet2.data.models.Proveedor
import com.example.gourmeet2.databinding.ItemProveedorBinding
import com.bumptech.glide.Glide

class ProveedorAdapter(
    private val proveedores: List<Proveedor>,
    private val categoriaActual: String = "",
    private val onClick: (Proveedor) -> Unit
) : RecyclerView.Adapter<ProveedorAdapter.ProveedorViewHolder>() {

    // ==========================================
    // UBICACIÓN DEL USUARIO
    // ==========================================

    var latitudUsuario: Double? = null
    var longitudUsuario: Double? = null


    // ==========================================
    // VIEW HOLDER
    // ==========================================

    inner class ProveedorViewHolder(
        val binding: ItemProveedorBinding
    ) : RecyclerView.ViewHolder(binding.root)


    // ==========================================
    // CREAR VIEW HOLDER
    // ==========================================

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ProveedorViewHolder {

        val binding =
            ItemProveedorBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

        return ProveedorViewHolder(binding)
    }


    // ==========================================
    // BIND
    // ==========================================

    override fun onBindViewHolder(
        holder: ProveedorViewHolder,
        position: Int
    ) {

        val proveedor =
            proveedores[position]


        // ==========================================
        // INFORMACIÓN DEL PROVEEDOR
        // ==========================================

        holder.binding.txtTitulo.text =
            proveedor.Pro_nombre
                ?: "Proveedor"

        holder.binding.txtgiro.text =
            proveedor.Pro_Des_Giro
                ?: ""

        holder.binding.txtProGIRO.text =
            proveedor.Pro_GIRO
                ?: ""


        // ==========================================
        // FOTO DEL PROVEEDOR
        // ==========================================

        Glide.with(
            holder.itemView.context
        )
            .load(
                proveedor.Pro_Foto_Perfil
            )
            .placeholder(
                R.drawable.logo_blanco_negro
            )
            .error(
                R.drawable.logo_blanco_negro
            )
            .centerCrop()
            .into(
                holder.binding.imgprovedor
            )


        // ==========================================
        // OBTENER CATEGORÍAS DE RECETAS
        // ==========================================
        //
        // Puede venir null cuando el proveedor
        // proviene de listar_colecciones_proveedores.php
        //

        val categoriasRecetas =
            proveedor.CATEGORIAS_RECETAS
                ?: emptyList()


        // ==========================================
        // OBTENER INGREDIENTES
        // ==========================================
        //
        // Puede venir null cuando el proveedor
        // no tiene ingredientes asociados.
        //

        val ingredientes =
            proveedor.INGREDIENTES
                ?: emptyList()


        // ==========================================
        // DETERMINAR SI ES CATEGORÍA DE RECETA
        // ==========================================

        val esCategoriaReceta =
            categoriaActual.isNotEmpty() &&
                    categoriasRecetas.any { categoria ->

                        categoria.trim().equals(
                            categoriaActual.trim(),
                            ignoreCase = true
                        )
                    }


        // ==========================================
        // CONTENIDO SEGÚN CATEGORÍA
        // ==========================================

        if (esCategoriaReceta) {

            // ======================================
            // ES CATEGORÍA DE RECETA
            // ======================================

            holder.binding.rvIngredientes.visibility =
                View.GONE

            holder.binding.layoutConocePlatillos.visibility =
                View.VISIBLE

        } else {

            // ======================================
            // ES CATEGORÍA DE INGREDIENTES
            // ======================================

            if (ingredientes.isNotEmpty()) {

                holder.binding.layoutConocePlatillos.visibility =
                    View.GONE

                holder.binding.rvIngredientes.visibility =
                    View.VISIBLE


                // ==================================
                // ADAPTER DE INGREDIENTES
                // ==================================

                val adapterIngredientes =
                    IngredientesProveedorAdapter(
                        ingredientes
                    )


                holder.binding.rvIngredientes.apply {

                    layoutManager =
                        LinearLayoutManager(
                            context,
                            LinearLayoutManager.HORIZONTAL,
                            false
                        )

                    adapter =
                        adapterIngredientes

                    setHasFixedSize(true)

                    overScrollMode =
                        View.OVER_SCROLL_NEVER
                }

            } else {

                // ==================================
                // NO HAY INGREDIENTES
                // ==================================

                holder.binding.rvIngredientes.visibility =
                    View.GONE

                holder.binding.layoutConocePlatillos.visibility =
                    View.VISIBLE
            }
        }


        // ==========================================
        // DISTANCIA
        // ==========================================

        calcularDistancia(
            holder,
            proveedor
        )


        // ==========================================
        // CLICK EN PROVEEDOR
        // ==========================================

        holder.itemView.setOnClickListener {

            onClick(
                proveedor
            )
        }
    }


    // ==========================================
    // CALCULAR DISTANCIA
    // ==========================================

    private fun calcularDistancia(
        holder: ProveedorViewHolder,
        proveedor: Proveedor
    ) {

        val latUsuario =
            latitudUsuario

        val lonUsuario =
            longitudUsuario


        val latProveedor =
            proveedor.Pro_Latitud
                ?.toDoubleOrNull()

        val lonProveedor =
            proveedor.Pro_Longitud
                ?.toDoubleOrNull()


        // ==========================================
        // VALIDAR COORDENADAS
        // ==========================================

        if (
            latUsuario != null &&
            lonUsuario != null &&
            latProveedor != null &&
            lonProveedor != null
        ) {

            // ======================================
            // UBICACIÓN DEL USUARIO
            // ======================================

            val ubicacionUsuario =
                Location("usuario").apply {

                    latitude =
                        latUsuario

                    longitude =
                        lonUsuario
                }


            // ======================================
            // UBICACIÓN DEL PROVEEDOR
            // ======================================

            val ubicacionProveedor =
                Location("proveedor").apply {

                    latitude =
                        latProveedor

                    longitude =
                        lonProveedor
                }


            // ======================================
            // CALCULAR DISTANCIA
            // ======================================

            val distancia =
                ubicacionUsuario.distanceTo(
                    ubicacionProveedor
                )


            // ======================================
            // MOSTRAR DISTANCIA
            // ======================================

            holder.binding.txtDistancia.text =

                if (distancia < 1000) {

                    "A ${distancia.toInt()} m de ti"

                } else {

                    String.format(
                        "A %.1f km de ti",
                        distancia / 1000
                    )
                }

        } else {

            // ======================================
            // DISTANCIA NO DISPONIBLE
            // ======================================

            holder.binding.txtDistancia.text =
                "Distancia no disponible"
        }
    }


    // ==========================================
    // CANTIDAD DE PROVEEDORES
    // ==========================================

    override fun getItemCount(): Int =
        proveedores.size
}