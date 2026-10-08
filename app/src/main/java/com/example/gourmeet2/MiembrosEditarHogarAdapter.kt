package com.example.gourmeet2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.gourmeet2.databinding.ItemEstadoDeInvitacionBinding
import com.example.gourmeet2.data.models.MiembroHogar
import com.example.gourmeet2.data.models.UsuarioBusqueda

class MiembrosEditarHogarAdapter(
    private var items: List<MiembroEditarItem>,
    private val onEliminarNuevo: (UsuarioBusqueda) -> Unit
) : RecyclerView.Adapter<MiembrosEditarHogarAdapter.MiembroViewHolder>() {

    inner class MiembroViewHolder(
        private val binding: ItemEstadoDeInvitacionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MiembroEditarItem) {

            when (item) {

                // ==========================================
                // MIEMBRO QUE YA EXISTE EN EL HOGAR
                // ==========================================
                is MiembroEditarItem.Existente -> {

                    val miembro = item.miembro

                    binding.txtNombreMiembro.text =
                        miembro.HOG_USU_NOMBRE ?: "Sin nombre"

                    val estado = miembro.HOG_USU_ESTADO ?: ""

                    binding.txtEstadoMiembro.visibility = View.VISIBLE
                    binding.txtEstadoMiembro.text = estado

                    if (estado.equals("Aceptado", ignoreCase = true)) {

                        binding.txtEstadoMiembro.visibility = View.GONE

                        binding.txtNombreMiembro.setTextColor(
                            binding.root.context.getColor(R.color.white)
                        )

                        binding.txtNombreMiembro.setBackgroundResource(
                            R.drawable.bg_usuario_hogar_aceptado
                        )

                    } else {

                        binding.txtEstadoMiembro.visibility = View.VISIBLE

                        binding.txtNombreMiembro.setTextColor(
                            binding.root.context.getColor(R.color.azulgourmeet)
                        )

                        binding.txtNombreMiembro.setBackgroundResource(
                            R.drawable.bg_usuario_hogar
                        )
                    }

                    // Los miembros existentes NO se eliminan desde aquí
                    binding.btnEliminarMiembro.visibility = View.GONE

                    binding.btnEliminarMiembro.setOnClickListener(null)
                }


                // ==========================================
                // NUEVO MIEMBRO SELECCIONADO
                // ==========================================
                is MiembroEditarItem.Nuevo -> {

                    val usuario = item.usuario

                    binding.txtNombreMiembro.text =
                        usuario.CLI_NOMBRE

                    // Siempre aparece como pendiente
                    binding.txtEstadoMiembro.visibility = View.VISIBLE
                    binding.txtEstadoMiembro.text = "Pendiente"

                    binding.txtNombreMiembro.setTextColor(
                        binding.root.context.getColor(R.color.azulgourmeet)
                    )

                    binding.txtNombreMiembro.setBackgroundResource(
                        R.drawable.bg_usuario_hogar
                    )

                    // Los nuevos sí se pueden quitar
                    binding.btnEliminarMiembro.visibility = View.VISIBLE

                    binding.btnEliminarMiembro.setOnClickListener {
                        onEliminarNuevo(usuario)
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MiembroViewHolder {

        val binding = ItemEstadoDeInvitacionBinding.inflate(
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
        holder.bind(items[position])
    }

    override fun getItemCount(): Int {
        return items.size
    }

    // ==========================================
    // ACTUALIZAR LISTA COMPLETA
    // ==========================================
    fun actualizarItems(nuevosItems: List<MiembroEditarItem>) {
        items = nuevosItems.toList()
        notifyDataSetChanged()
    }

    // ==========================================
    // OBTENER ITEMS ACTUALES
    // ==========================================
    fun obtenerItems(): List<MiembroEditarItem> {
        return items.toList()
    }
}