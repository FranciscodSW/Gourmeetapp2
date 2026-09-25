package com.example.gourmeet2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.gourmeet2.data.models.UsuarioBusqueda
import com.example.gourmeet2.databinding.ItemEstadoDeInvitacionBinding

class MiembrosHogarAdapter(
    private var miembros: List<UsuarioBusqueda>,
    private val onEliminar: (UsuarioBusqueda) -> Unit
) : RecyclerView.Adapter<MiembrosHogarAdapter.MiembroViewHolder>() {

    inner class MiembroViewHolder(
        private val binding: ItemEstadoDeInvitacionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(usuario: UsuarioBusqueda) {

            binding.txtNombreMiembro.text =
                usuario.CLI_NOMBRE

            // Mientras el hogar todavía no se guarda,
            // todos los usuarios están pendientes.
            binding.txtEstadoMiembro.text =
                "Por confirmar"

            binding.txtEstadoMiembro.visibility =
                View.VISIBLE

            binding.btnEliminarMiembro.setOnClickListener {

                onEliminar(usuario)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MiembroViewHolder {

        val binding =
            ItemEstadoDeInvitacionBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

        return MiembroViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: MiembroViewHolder,
        position: Int
    ) {

        holder.bind(
            miembros[position]
        )
    }

    override fun getItemCount(): Int {
        return miembros.size
    }

    fun actualizarMiembros(
        nuevosMiembros: List<UsuarioBusqueda>
    ) {

        // IMPORTANTE:
        // Creamos una copia de la lista.
        miembros = nuevosMiembros.toList()

        notifyDataSetChanged()
    }
}