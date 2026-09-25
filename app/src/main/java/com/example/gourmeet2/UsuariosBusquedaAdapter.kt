package com.example.gourmeet2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.gourmeet2.data.models.UsuarioBusqueda
import com.example.gourmeet2.databinding.ItemUsuarioBusquedaBinding

class UsuariosBusquedaAdapter(
    private var usuarios: List<UsuarioBusqueda>,
    private val onInvitar: (UsuarioBusqueda) -> Unit
) : RecyclerView.Adapter<UsuariosBusquedaAdapter.UsuarioViewHolder>() {

    inner class UsuarioViewHolder(
        private val binding: ItemUsuarioBusquedaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(usuario: UsuarioBusqueda) {

            binding.txtNombreUsuario.text =
                usuario.CLI_NOMBRE

            binding.txtCorreoUsuario.text =
                usuario.CLI_CORREO

            if (!usuario.FotoUsuario.isNullOrEmpty()) {

                Glide.with(binding.imgFotoUsuario.context)
                    .load(usuario.FotoUsuario)
                    .placeholder(R.drawable.ic_user)
                    .error(R.drawable.ic_user)
                    .circleCrop()
                    .into(binding.imgFotoUsuario)

            } else {
                binding.imgFotoUsuario.setImageResource(
                    R.drawable.ic_user
                )
            }

            binding.btnInvitarUsuario.setOnClickListener {
                onInvitar(usuario)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): UsuarioViewHolder {

        val binding =
            ItemUsuarioBusquedaBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

        return UsuarioViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: UsuarioViewHolder,
        position: Int
    ) {
        holder.bind(usuarios[position])
    }

    override fun getItemCount(): Int {
        return usuarios.size
    }

    fun actualizarUsuarios(
        nuevosUsuarios: List<UsuarioBusqueda>
    ) {
        usuarios = nuevosUsuarios
        notifyDataSetChanged()
    }
}