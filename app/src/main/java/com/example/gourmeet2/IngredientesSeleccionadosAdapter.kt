package com.example.gourmeet2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.gourmeet2.data.models.ResultadoBusqueda
import com.example.gourmeet2.databinding.ItemSeleccionadoBinding

class IngredientesSeleccionadosAdapter(

    private val ingredientes: MutableList<ResultadoBusqueda>,

    private val listener: OnIngredienteSeleccionadoListener

) : RecyclerView.Adapter<IngredientesSeleccionadosAdapter.ViewHolder>() {

    interface OnIngredienteSeleccionadoListener {

        fun onEliminarIngrediente(
            ingrediente: ResultadoBusqueda
        )

    }

    inner class ViewHolder(

        private val binding: ItemSeleccionadoBinding

    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            ingrediente: ResultadoBusqueda
        ) {

            // ==========================================
            // NOMBRE
            // ==========================================

            binding.txt.text =
                ingrediente.nombre


            // ==========================================
            // FOTO
            // ==========================================

            if (!ingrediente.foto.isNullOrEmpty()) {

                Glide.with(binding.img.context)
                    .load(ingrediente.foto)
                    .centerCrop()
                    .into(binding.img)

            } else {

                binding.img.setImageResource(
                    R.drawable.ic_ingredientes
                )

            }


            // ==========================================
            // ELIMINAR
            // ==========================================

            binding.btnEliminar.setOnClickListener {

                listener.onEliminarIngrediente(
                    ingrediente
                )

            }

        }

    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val binding =
            ItemSeleccionadoBinding.inflate(
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

        holder.bind(
            ingredientes[position]
        )

    }

    override fun getItemCount(): Int =
        ingredientes.size


    fun actualizar(
        nuevosIngredientes: List<ResultadoBusqueda>
    ) {

        ingredientes.clear()

        ingredientes.addAll(
            nuevosIngredientes
        )

        notifyDataSetChanged()

    }

}