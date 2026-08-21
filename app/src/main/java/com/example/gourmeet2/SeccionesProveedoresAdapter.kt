package com.example.gourmeet2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gourmeet2.data.models.SeccionProveedores
import com.example.gourmeet2.databinding.ItemSeccionProveedoresBinding

class SeccionesProveedoresAdapter(
    private val secciones: MutableList<SeccionProveedores>
) : RecyclerView.Adapter<SeccionesProveedoresAdapter.SeccionViewHolder>() {

    // ==========================================
    // UBICACIÓN DEL USUARIO
    // ==========================================

    private var latitudUsuario: Double? = null
    private var longitudUsuario: Double? = null


    inner class SeccionViewHolder(
        val binding: ItemSeccionProveedoresBinding
    ) : RecyclerView.ViewHolder(binding.root)


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SeccionViewHolder {

        val binding =
            ItemSeccionProveedoresBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

        return SeccionViewHolder(binding)
    }


    override fun onBindViewHolder(
        holder: SeccionViewHolder,
        position: Int
    ) {

        val seccion =
            secciones[position]


        // ==========================================
        // TÍTULO DE LA CATEGORÍA
        // ==========================================

        holder.binding.txtCategoria.text =
            seccion.categoria


        // ==========================================
        // PROVEEDORES DE ESTA CATEGORÍA
        // ==========================================

        val adapterProveedores =
            ProveedorAdapter(
                seccion.proveedores,
                seccion.categoria
            ) { proveedor ->

                // Posteriormente abriremos
                // el detalle del proveedor

            }


        // ==========================================
        // PASAR UBICACIÓN DEL USUARIO
        // ==========================================

        adapterProveedores.latitudUsuario =
            latitudUsuario

        adapterProveedores.longitudUsuario =
            longitudUsuario


        // ==========================================
        // RECYCLER HORIZONTAL
        // ==========================================

        holder.binding.rvProveedoresCategoria.apply {

            layoutManager =
                LinearLayoutManager(
                    context,
                    LinearLayoutManager.HORIZONTAL,
                    false
                )

            adapter =
                adapterProveedores

            setHasFixedSize(true)

            overScrollMode =
                View.OVER_SCROLL_NEVER
        }
    }


    override fun getItemCount(): Int =
        secciones.size


    // ==========================================
    // ACTUALIZAR SECCIONES
    // ==========================================

    fun actualizar(
        nuevasSecciones: List<SeccionProveedores>
    ) {

        secciones.clear()

        secciones.addAll(
            nuevasSecciones
        )

        notifyDataSetChanged()
    }


    // ==========================================
    // ACTUALIZAR UBICACIÓN
    // ==========================================

    fun actualizarUbicacion(
        latitud: Double?,
        longitud: Double?
    ) {

        latitudUsuario =
            latitud

        longitudUsuario =
            longitud

        // Volvemos a crear los adapters
        // internos con la nueva ubicación

        notifyDataSetChanged()
    }
}