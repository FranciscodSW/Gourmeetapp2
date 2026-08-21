package com.example.gourmeet2

import android.location.Location
import android.util.Log
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
    private val categoriaActual: String,
    private val onClick: (Proveedor) -> Unit
) : RecyclerView.Adapter<ProveedorAdapter.ProveedorViewHolder>() {

    var latitudUsuario: Double? = null
    var longitudUsuario: Double? = null


    inner class ProveedorViewHolder(
        val binding: ItemProveedorBinding
    ) : RecyclerView.ViewHolder(binding.root)


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


    override fun onBindViewHolder(
        holder: ProveedorViewHolder,
        position: Int
    ) {

        val proveedor =
            proveedores[position]


        // ==========================================
        // INFORMACIÓN
        // ==========================================

        holder.binding.txtTitulo.text =
            proveedor.Pro_nombre ?: "Proveedor"

        holder.binding.txtgiro.text =
            proveedor.Pro_Des_Giro ?: ""

        holder.binding.txtProGIRO.text =
            proveedor.Pro_GIRO ?: ""


        // ==========================================
        // FOTO DEL PROVEEDOR
        // ==========================================

        Glide.with(
            holder.itemView.context
        )
            .load(proveedor.Pro_Foto_Perfil)
            .placeholder(
                R.drawable.logo_blanco_negro
            )
            .error(
                R.drawable.logo_blanco_negro
            )
            .centerCrop()
            .into(holder.binding.imgprovedor)


        // ==========================================
        // INGREDIENTES
        // ==========================================

        // ==========================================
// CONTENIDO SEGÚN CATEGORÍA
// ==========================================

        val esCategoriaReceta =
            proveedor.CATEGORIAS_RECETAS.any { categoria ->

                categoria.trim().equals(
                    categoriaActual.trim(),
                    ignoreCase = true
                )
            }


// ==========================================
// SI LA CATEGORÍA ES DE RECETAS
// ==========================================

        if (esCategoriaReceta) {

            // NO mostrar ingredientes
            holder.binding.rvIngredientes.visibility =
                View.GONE

            // Mostrar "Conoce sus platillos"
            holder.binding.layoutConocePlatillos.visibility =
                View.VISIBLE

        } else {

            // ==========================================
            // CATEGORÍA DE INGREDIENTES
            // ==========================================

            if (proveedor.INGREDIENTES.isNotEmpty()) {

                holder.binding.layoutConocePlatillos.visibility =
                    View.GONE

                holder.binding.rvIngredientes.visibility =
                    View.VISIBLE

                val adapterIngredientes =
                    IngredientesProveedorAdapter(
                        proveedor.INGREDIENTES
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
        // CLICK
        // ==========================================

        holder.itemView.setOnClickListener {

            onClick(proveedor)

        }
    }


    private fun calcularDistancia(
        holder: ProveedorViewHolder,
        proveedor: Proveedor
    ) {

        val latUsuario =
            latitudUsuario

        val lonUsuario =
            longitudUsuario

        val latProveedor =
            proveedor.Pro_Latitud?.toDoubleOrNull()

        val lonProveedor =
            proveedor.Pro_Longitud?.toDoubleOrNull()


        if (
            latUsuario != null &&
            lonUsuario != null &&
            latProveedor != null &&
            lonProveedor != null
        ) {

            val ubicacionUsuario =
                Location("usuario").apply {

                    latitude =
                        latUsuario

                    longitude =
                        lonUsuario
                }


            val ubicacionProveedor =
                Location("proveedor").apply {

                    latitude =
                        latProveedor

                    longitude =
                        lonProveedor
                }


            val distancia =
                ubicacionUsuario.distanceTo(
                    ubicacionProveedor
                )


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

            holder.binding.txtDistancia.text =
                "Distancia no disponible"
        }
    }


    override fun getItemCount(): Int =
        proveedores.size
}