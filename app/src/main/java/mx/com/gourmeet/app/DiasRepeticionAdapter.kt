package mx.com.gourmeet.app

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import mx.com.gourmeet.app.data.models.DiaRepeticion
import mx.com.gourmeet.app.databinding.ItemDiaRepetirBinding

class DiasRepeticionAdapter(
    private val dias: List<DiaRepeticion>,
    private val fechaOriginal: String,
    private val onDiasCambiados: (Set<String>) -> Unit
) : RecyclerView.Adapter<DiasRepeticionAdapter.ViewHolder>() {

    private val fechasSeleccionadas = mutableSetOf(fechaOriginal)

    inner class ViewHolder(
        val binding: ItemDiaRepetirBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val binding = ItemDiaRepetirBinding.inflate(
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
        val dia = dias[position]
        val seleccionado = dia.fecha in fechasSeleccionadas
        val esOriginal = dia.fecha == fechaOriginal

        with(holder.binding) {
            txtInicialDia.text = dia.inicial
            txtNombreDia.text = dia.nombre

            txtInicialDia.setTextColor(
                if (seleccionado) Color.WHITE
                else Color.rgb(23, 122, 255)
            )

            txtInicialDia.setBackgroundResource(
                if (seleccionado) {
                    R.drawable.bg_dia_repeticion_seleccionado
                } else {
                    R.drawable.bg_ovalo_sin_fondo
                }
            )

            root.alpha = if (esOriginal) 1f else 1f

            root.setOnClickListener {
                if (esOriginal) return@setOnClickListener

                if (fechasSeleccionadas.contains(dia.fecha)) {
                    fechasSeleccionadas.remove(dia.fecha)
                } else {
                    fechasSeleccionadas.add(dia.fecha)
                }

                notifyItemChanged(position)
                onDiasCambiados(fechasSeleccionadas.toSet())
            }
        }
    }

    override fun getItemCount(): Int = dias.size

    fun obtenerFechasSeleccionadas(): Set<String> {
        return fechasSeleccionadas.toSet()
    }
}
