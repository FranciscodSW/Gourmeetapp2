package com.example.gourmeet2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.gourmeet2.data.models.IconoHogar
import com.example.gourmeet2.databinding.ItemIconoHogarBinding

class IconosHogarAdapter(
    private val iconos: List<IconoHogar>,
    private val onIconoSeleccionado: (IconoHogar) -> Unit
) : RecyclerView.Adapter<IconosHogarAdapter.IconoViewHolder>() {

    inner class IconoViewHolder(
        private val binding: ItemIconoHogarBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(icono: IconoHogar) {

            binding.imgIconoHogar.setImageResource(
                icono.recurso
            )

            binding.root.setOnClickListener {

                onIconoSeleccionado(icono)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): IconoViewHolder {

        val binding =
            ItemIconoHogarBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

        return IconoViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: IconoViewHolder,
        position: Int
    ) {
        holder.bind(iconos[position])
    }

    override fun getItemCount(): Int {
        return iconos.size
    }
}