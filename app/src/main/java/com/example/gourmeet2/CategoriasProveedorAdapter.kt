package com.example.gourmeet2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.gourmeet2.databinding.ItemResultadoBusquedaBinding

class CategoriasProveedorAdapter(
    private val categorias: List<String>
) : RecyclerView.Adapter<CategoriasProveedorAdapter.ViewHolder>() {

    inner class ViewHolder(
        val binding: ItemResultadoBusquedaBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val binding =
            ItemResultadoBusquedaBinding.inflate(
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

        val categoria = categorias[position]

        holder.binding.txtNombre.text =
            categoria

        holder.binding.imgIcono.setImageResource(
            R.drawable.ic_guardar
        )
    }

    override fun getItemCount(): Int =
        categorias.size
}