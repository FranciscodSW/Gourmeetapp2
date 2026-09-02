package com.example.gourmeet2

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gourmeet2.databinding.ActivityPremiumBinding

class PremiumActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPremiumBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding =
            ActivityPremiumBinding.inflate(
                layoutInflater
            )

        setContentView(binding.root)


        // ==========================================
        // BOTÓN REGRESAR
        // ==========================================

        binding.btnRegresar.setOnClickListener {

            finish()

        }


        // ==========================================
        // PLANES
        // ==========================================

        val planes = listOf(

            PlanPremium(
                R.drawable.ic_plan_basico,
                "Plan Básico"
            ),

            PlanPremium(
                R.drawable.ic_plan_medio,
                "Plan Medio"
            ),

            PlanPremium(
                R.drawable.ic_plan_pro,
                "Plan Pro"
            ),

            PlanPremium(
                R.drawable.ic_plan_pro_max,
                "Plan Pro Max"
            )

        )


        // ==========================================
        // ADAPTER
        // ==========================================

        val adapter =
            PlanPremiumAdapter(
                planes
            )


        // ==========================================
        // RECYCLER HORIZONTAL
        // ==========================================

        binding.rvPlanesPremium.apply {

            layoutManager =
                LinearLayoutManager(
                    this@PremiumActivity,
                    LinearLayoutManager.HORIZONTAL,
                    false
                )

            this.adapter =
                adapter

            setHasFixedSize(true)

            isNestedScrollingEnabled =
                false

            overScrollMode =
                android.view.View.OVER_SCROLL_NEVER
        }
    }
}