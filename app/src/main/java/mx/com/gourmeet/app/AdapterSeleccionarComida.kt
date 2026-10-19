
package mx.com.gourmeet.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import mx.com.gourmeet.app.data.models.EspacioComida
import mx.com.gourmeet.app.databinding.ItemSeleccionarComidaBinding

class AdapterSeleccionarComida(
    private val espacios: List<EspacioComida>,
    private val onAgregarComida: (EspacioComida) -> Unit,
    private val onAsignarHora: (EspacioComida) -> Unit
) : RecyclerView.Adapter<AdapterSeleccionarComida.ComidaViewHolder>() {

    inner class ComidaViewHolder(
        val binding: ItemSeleccionarComidaBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ComidaViewHolder {
        val binding = ItemSeleccionarComidaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ComidaViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ComidaViewHolder,
        position: Int
    ) {
        val espacio = espacios[position]
        val binding = holder.binding

        binding.txtTitulo.text = when (espacio.TPC_ID) {
            1 -> "Desayuno"
            2 -> "Comida"
            3 -> "Cena"
            4 -> "Colación ${espacio.ECM_NUMERO_COLACION}"
            else -> "Comida"
        }
        binding.listadecomidas.layoutManager =
            LinearLayoutManager(binding.root.context)

        binding.listadecomidas.adapter =
            AdapterNombreComida(
                recetas = espacio.recetas
                    .map { it.REC_NOMBRE }
                    .toMutableList()
            )

        actualizarTextoHora(binding.btnAsignarhora, espacio)

        binding.btnAgregarComida.setOnClickListener {
            val posicion = holder.bindingAdapterPosition
            if (posicion != RecyclerView.NO_POSITION) {
                onAgregarComida(espacios[posicion])
            }
        }

        binding.btnAsignarhora.setOnClickListener {
            val posicion = holder.bindingAdapterPosition
            if (posicion != RecyclerView.NO_POSITION) {
                onAsignarHora(espacios[posicion])
            }
        }
    }

    private fun actualizarTextoHora(
        boton: android.widget.Button,
        espacio: EspacioComida
    ) {
        val hora = espacio.ECM_HORA
            ?.take(5)
            ?.takeIf { it.matches(Regex("\\d{2}:\\d{2}")) }

        boton.text = if (hora != null) {
            "Hora: $hora"
        } else {
            "Asignar hora"
        }
    }

    override fun getItemCount(): Int = espacios.size
}
