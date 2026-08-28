package com.example.gourmeet2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gourmeet2.data.models.ColeccionProveedor
import com.example.gourmeet2.data.models.Proveedor
import com.example.gourmeet2.databinding.ItemColeccionProveedorBinding

class ColeccionesProveedoresAdapter(
    private val colecciones: List<ColeccionProveedor>,
    private val onProveedorClick: (Proveedor) -> Unit
) : RecyclerView.Adapter<ColeccionesProveedoresAdapter.ViewHolder>() {


    inner class ViewHolder(
        val binding: ItemColeccionProveedorBinding
    ) : RecyclerView.ViewHolder(binding.root)


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val binding =
            ItemColeccionProveedorBinding.inflate(
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

        val coleccion =
            colecciones[position]


        // ==========================================
        // NOMBRE DE LA COLECCIÓN
        // ==========================================

        holder.binding.txtNombreColeccion.text =
            coleccion.COP_NOMBRE


        // ==========================================
        // PROVEEDORES
        // ==========================================

        val proveedores =
            coleccion.proveedores
                ?: emptyList()


        if (proveedores.isEmpty()) {

            holder.binding.rvProveedoresColeccion.visibility =
                View.GONE

            return
        }


        holder.binding.rvProveedoresColeccion.visibility =
            View.VISIBLE


        // ==========================================
        // ADAPTER DE PROVEEDORES
        // ==========================================

        val adapterProveedores =
            ProveedorAdapter(
                proveedores = proveedores,

                // En Mis colecciones no necesitamos
                // una categoría externa.
                categoriaActual = "",

                onClick = { proveedor ->

                    onProveedorClick(
                        proveedor
                    )
                }
            )


        // ==========================================
        // RECYCLER DE PROVEEDORES
        // ==========================================

        holder.binding.rvProveedoresColeccion.apply {

            layoutManager =
                GridLayoutManager(
                    context,
                    2
                )

            adapter =
                adapterProveedores

            setHasFixedSize(false)

            isNestedScrollingEnabled =
                false

            overScrollMode =
                View.OVER_SCROLL_NEVER
        }
    }


    override fun getItemCount(): Int =
        colecciones.size
}