
package mx.com.gourmeet.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import mx.com.gourmeet.app.data.models.DiaPlaneador
import java.time.LocalDate
import java.time.DayOfWeek

class AdapterDiasSeleccionados(
    private var dias: List<DiaPlaneador>
) : RecyclerView.Adapter<AdapterDiasSeleccionados.DiaViewHolder>() {

    class DiaViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val txtInicialDia: TextView =
            itemView.findViewById(R.id.txtInicialDia)

        val txtNombreDia: TextView =
            itemView.findViewById(R.id.txtNombreDia)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DiaViewHolder {
        val vista = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dia_repetir, parent, false)

        return DiaViewHolder(vista)
    }

    override fun onBindViewHolder(
        holder: DiaViewHolder,
        position: Int
    ) {
        val dia = dias[position]
        val fecha = LocalDate.parse(dia.fecha)

        holder.txtInicialDia.text = when (fecha.dayOfWeek) {
            DayOfWeek.MONDAY -> "L"
            DayOfWeek.TUESDAY -> "M"
            DayOfWeek.WEDNESDAY -> "M"
            DayOfWeek.THURSDAY -> "J"
            DayOfWeek.FRIDAY -> "V"
            DayOfWeek.SATURDAY -> "S"
            DayOfWeek.SUNDAY -> "D"
        }

        holder.txtNombreDia.text =
            "${dia.nombreDia.substringBefore(" ")} ${fecha.dayOfMonth}"

        if (dia.seleccionado) {
            holder.txtInicialDia.setTextColor(
                ContextCompat.getColor(
                    holder.itemView.context,
                    android.R.color.white
                )
            )
            holder.txtInicialDia.setBackgroundResource(
                R.drawable.bg_ovalo_azul
            )
        } else {
            holder.txtInicialDia.setTextColor(
                ContextCompat.getColor(
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

    fun actualizarDias(nuevosDias: List<DiaPlaneador>) {
        dias = nuevosDias.toList()
        notifyDataSetChanged()
    }
}
