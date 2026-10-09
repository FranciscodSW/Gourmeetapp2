package mx.com.gourmeet.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import mx.com.gourmeet.app.data.models.MiembroHogar
import mx.com.gourmeet.app.databinding.ItemEstadoDeInvitacionBinding

class MiembrosHogarAdapter(
    private var miembros: List<MiembroHogar>,
    private val onEliminar: (MiembroHogar) -> Unit
) : RecyclerView.Adapter<MiembrosHogarAdapter.MiembroViewHolder>() {

    inner class MiembroViewHolder(
        private val binding: ItemEstadoDeInvitacionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(miembro: MiembroHogar) {

            // ==========================================
            // NOMBRE
            // ==========================================

            binding.txtNombreMiembro.text =
                miembro.HOG_USU_NOMBRE ?: "Sin nombre"


            // ==========================================
            // ESTADO
            // ==========================================

            val estado = miembro.HOG_USU_ESTADO ?: ""

            if (estado.equals("Aceptado", ignoreCase = true)) {

                // La invitación ya fue aceptada
                binding.txtEstadoMiembro.visibility = View.GONE

                // Nombre en blanco
                binding.txtNombreMiembro.setTextColor(
                    binding.root.context.getColor(R.color.white)
                )

                // Fondo para integrante aceptado
                binding.txtNombreMiembro.setBackgroundResource(
                    R.drawable.bg_usuario_hogar_aceptado
                )

            } else {

                // La invitación todavía no ha sido aceptada
                binding.txtEstadoMiembro.visibility = View.VISIBLE
                binding.txtEstadoMiembro.text = estado

                // Diseño normal
                binding.txtNombreMiembro.setTextColor(
                    binding.root.context.getColor(R.color.azulgourmeet)
                )

                binding.txtNombreMiembro.setBackgroundResource(
                    R.drawable.bg_usuario_hogar
                )
            }
            // ==========================================
            // ELIMINAR
            // ==========================================

            binding.btnEliminarMiembro.setOnClickListener {
                onEliminar(miembro)
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
        nuevosMiembros: List<MiembroHogar>
    ) {

        miembros = nuevosMiembros.toList()

        notifyDataSetChanged()
    }
}