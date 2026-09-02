package com.example.gourmeet2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.gourmeet2.databinding.ItemPlanPremiumBinding

class PlanPremiumAdapter(

    private val planes: List<PlanPremium>

) : RecyclerView.Adapter<PlanPremiumAdapter.ViewHolder>() {


    // ==========================================
    // VIEW HOLDER
    // ==========================================

    inner class ViewHolder(

        val binding: ItemPlanPremiumBinding

    ) : RecyclerView.ViewHolder(
        binding.root
    )


    // ==========================================
    // CREAR VIEW HOLDER
    // ==========================================

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val binding =
            ItemPlanPremiumBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

        return ViewHolder(
            binding
        )
    }


    // ==========================================
    // BIND
    // ==========================================

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        val plan =
            planes[position]


        // ==========================================
        // IMAGEN
        // ==========================================

        holder.binding.imgPlanPremium
            .setImageResource(
                plan.imagen
            )


        // ==========================================
        // NOMBRE
        // ==========================================

        holder.binding.txtNombrePlan
            .text =
            plan.nombre


        // ==========================================
        // BOTÓN COMPRAR
        // ==========================================

        holder.binding.btnComprarPlan
            .setOnClickListener {

                // ==================================
                // POR EL MOMENTO NO HACE NADA
                // ==================================

            }
    }


    // ==========================================
    // TOTAL
    // ==========================================

    override fun getItemCount(): Int =
        planes.size
}