package com.example.gourmeet2

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.gourmeet2.databinding.ActivityMenuPrincipalFreeBinding
import com.example.gourmeet2.databinding.ItemDelCarruselBinding

class Menu_principal_free : AppCompatActivity() {

    private lateinit var binding: ActivityMenuPrincipalFreeBinding

    // Datos del carrusel
    private val categorias = listOf(
        Categoria("Ensalada", "#4CAF50"),
        Categoria("Entrada", "#2196F3"),
        Categoria("Especialidad", "#FF9800"),
        Categoria("Bebida","#4CAF50"),
        Categoria("Plato fuerte","#2196F3"),
        Categoria("Postre", "#FF9800"),
        Categoria("Snack", "#4CAF50")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMenuPrincipalFreeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupCarrusel()
    }

    private fun setupCarrusel() {

        // Estado inicial
        //binding.txtOpcionSeleccionada.text = categorias[0].nombre
        binding.fondoDinamico.setBackgroundColor(Color.parseColor(categorias[0].color))


        // Adapter
        val adapter = CarruselAdapter(categorias)
        binding.viewPagerCarrusel.adapter = adapter

        // Listener de cambio
        binding.viewPagerCarrusel.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    val categoria = categorias[position]
                   // binding.txtOpcionSeleccionada.text = categoria.nombre
                    binding.fondoDinamico.setBackgroundColor(Color.parseColor(categoria.color))
                }
            }
        )
    }

    // Data class
    data class Categoria(
        val nombre: String,
        val color: String
    )

    // Adapter conectado al XML
    inner class CarruselAdapter(
        private val categorias: List<Categoria>
    ) : RecyclerView.Adapter<CarruselAdapter.CarruselViewHolder>() {

        inner class CarruselViewHolder(
            val binding: ItemDelCarruselBinding
        ) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): CarruselViewHolder {
            val binding = ItemDelCarruselBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return CarruselViewHolder(binding)
        }

        override fun onBindViewHolder(holder: CarruselViewHolder, position: Int) {
            val categoria = categorias[position]

            holder.binding.txtCategoria.text = categoria.nombre
            holder.binding.cardCategoria.setCardBackgroundColor(
                Color.parseColor(categoria.color)
            )

            holder.binding.root.setOnClickListener {
                binding.viewPagerCarrusel.currentItem = position
            }
        }

        override fun getItemCount(): Int = categorias.size
    }
}
