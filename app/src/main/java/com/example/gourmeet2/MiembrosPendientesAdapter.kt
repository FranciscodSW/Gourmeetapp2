package com.example.gourmeet2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.gourmeet2.data.models.UsuarioBusqueda
import com.example.gourmeet2.databinding.ItemEstadoDeInvitacionBinding

class MiembrosPendientesAdapter(
    private var miembros: List<UsuarioBusqueda>,
    private val onEliminar: (UsuarioBusqueda) -> Unit
) : RecyclerView.Adapter<MiembrosPendientesAdapter.MiembroViewHolder>() {

    inner class MiembroViewHolder(
        private val binding: ItemEstadoDeInvitacionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(usuario: UsuarioBusqueda) {

            binding.txtNombreMiembro.text =
                usuario.CLI_NOMBRE

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
        holder.bind(miembros[position])
    }

    override fun getItemCount(): Int =
        miembros.size

    fun actualizarMiembros(
        nuevosMiembros: List<UsuarioBusqueda>
    ) {

        miembros =
            nuevosMiembros.toList()

        notifyDataSetChanged()
    }
}