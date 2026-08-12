package com.example.gourmeet2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.gourmeet2.data.models.IconoColeccion
import com.example.gourmeet2.databinding.ItemSeleccionadoBinding

class IconosAdapter(

    private val lista: List<IconoColeccion>,

    private val listener: (IconoColeccion) -> Unit

) : RecyclerView.Adapter<IconosAdapter.ViewHolder>() {

    inner class ViewHolder(

        val binding: ItemSeleccionadoBinding

    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val binding = ItemSeleccionadoBinding.inflate(

            LayoutInflater.from(parent.context),

            parent,

            false

        )

        return ViewHolder(binding)

    }

    override fun getItemCount() = lista.size

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        val icono = lista[position]

        holder.binding.img.setImageResource(

            icono.drawable

        )

        holder.binding.txt.text = icono.nombre

        holder.itemView.setOnClickListener {

            listener(icono)

        }

    }

}