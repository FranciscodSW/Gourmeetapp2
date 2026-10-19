package mx.com.gourmeet.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import mx.com.gourmeet.app.data.models.DiaPlaneador


class AdapterDiasRepeticion(
    private val dias: List<DiaPlaneador>
) : RecyclerView.Adapter<AdapterDiasRepeticion.DiaViewHolder>() {

    inner class DiaViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {
        val txtInicialDia: TextView =
            itemView.findViewById(R.id.txtInicialDia)
        val txtNombreDia: TextView =
            itemView.findViewById(R.id.txtNombreDia)
    }
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DiaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dia_repetir, parent, false)

        return DiaViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: DiaViewHolder,
        position: Int
    ) {
        val dia = dias[position]

        val fecha = java.time.LocalDate.parse(dia.fecha)
        val inicial = when (fecha.dayOfWeek) {
            java.time.DayOfWeek.MONDAY -> "L"
            java.time.DayOfWeek.TUESDAY -> "M"
            java.time.DayOfWeek.WEDNESDAY -> "M"
            java.time.DayOfWeek.THURSDAY -> "J"
            java.time.DayOfWeek.FRIDAY -> "V"
            java.time.DayOfWeek.SATURDAY -> "S"
            java.time.DayOfWeek.SUNDAY -> "D"
        }

        holder.txtInicialDia.text = inicial
        holder.txtNombreDia.text =
            "${dia.nombreDia} ${fecha.dayOfMonth}"

        if (dia.seleccionado) {
            holder.txtInicialDia.setTextColor(
                android.graphics.Color.WHITE
            )
            holder.txtInicialDia.setBackgroundResource(
                R.drawable.bg_ovalo_azul
            )
        } else {
            holder.txtInicialDia.setTextColor(
                androidx.core.content.ContextCompat.getColor(
                    holder.itemView.context,
                    R.color.azulgourmeet
                )
            )
            holder.txtInicialDia.setBackgroundResource(
                R.drawable.bg_ovalo_sin_fondo
            )
        }
    }

    override fun getItemCount(): Int = dias.size
}
