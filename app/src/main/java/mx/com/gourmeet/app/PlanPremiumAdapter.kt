package mx.com.gourmeet.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import mx.com.gourmeet.app.databinding.ItemPlanPremiumBinding

class PlanPremiumAdapter(

    private val planes: List<PlanPremium>,

    private val onSeleccionarPlan: (
        productoId: String,
        periodicidad: String,
        nombrePlan: String
    ) -> Unit

) : RecyclerView.Adapter<PlanPremiumAdapter.ViewHolder>() {


    inner class ViewHolder(
        val binding: ItemPlanPremiumBinding
    ) : RecyclerView.ViewHolder(binding.root)


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val binding = ItemPlanPremiumBinding.inflate(
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

        val plan = planes[position]

        // IMAGEN
        holder.binding.imgPlanPremium
            .setImageResource(plan.imagen)

        // NOMBRE
        holder.binding.txtNombrePlan.text =
            plan.nombre

        // PRECIO MENSUAL
        holder.binding.txtPrecioMensual.text =
            plan.precioMensual

        // PRECIO ANUAL
        holder.binding.txtPrecioAnual.text =
            plan.precioAnual


        // COMPRAR MENSUAL
        holder.binding.btnComprarPlan.setOnClickListener {
            onSeleccionarPlan(
                plan.productoId,
                plan.planBasicoMensual,
                plan.nombre
            )
        }


        // COMPRAR ANUAL
        holder.binding.btnComprarPlanAN.setOnClickListener {

            onSeleccionarPlan(
                plan.productoId,
                plan.planBasicoAnual,
                plan.nombre
            )

        }

    }

    override fun getItemCount(): Int =
        planes.size

}