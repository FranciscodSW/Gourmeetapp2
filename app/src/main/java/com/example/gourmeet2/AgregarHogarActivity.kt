package com.example.gourmeet2

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gourmeet2.data.models.IconoHogar
import com.example.gourmeet2.databinding.ItemAgregarHogarBinding

class AgregarHogarActivity : AppCompatActivity() {

    private lateinit var binding: ItemAgregarHogarBinding

    private val iconosHogar = listOf(

        IconoHogar(
            "Casa",
            R.drawable.ic_casa_azul
        ),

        IconoHogar(
            "Casa 2",
            R.drawable.ic_casa2
        ),

        IconoHogar(
            "Edificio",
            R.drawable.ic_casa1
        ),

        IconoHogar(
            "Departamento",
            R.drawable.ic_casa_azul
        ),

        IconoHogar(
            "Familia",
            R.drawable.ic_casa1
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ItemAgregarHogarBinding.inflate(layoutInflater)

        setContentView(binding.root)

        configurarSelectorIconos()
    }

    private fun configurarSelectorIconos() {

        binding.seleccionarimagen.setOnClickListener {

            binding.selecciondeicono.visibility = View.VISIBLE
        }

        binding.selecciondeicono.layoutManager =
            LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL,
                false
            )

        binding.selecciondeicono.adapter =
            IconosHogarAdapter(iconosHogar) { iconoSeleccionado ->

                binding.seleccionarimagen.setImageResource(
                    iconoSeleccionado.recurso
                )

                binding.selecciondeicono.visibility = View.GONE
            }
    }
}