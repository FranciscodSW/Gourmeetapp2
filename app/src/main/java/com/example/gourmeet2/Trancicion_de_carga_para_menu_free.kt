package com.example.gourmeet2

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.gourmeet2.databinding.ActivityTrancicionDeCargaParaMenuFreeBinding

class Trancicion_de_carga_para_menu_free : AppCompatActivity() {

    private lateinit var binding: ActivityTrancicionDeCargaParaMenuFreeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        binding = ActivityTrancicionDeCargaParaMenuFreeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // INICIALMENTE EL CONTENEDOR ESTÁ GONE (del XML)
        // binding.transicionContainer.visibility = View.GONE

        // Iniciar animación después de un delay
        Handler(Looper.getMainLooper()).postDelayed({
            iniciarAnimacionSubida()
        }, 500)

        // Configurar botón Continuar
        // Configurar botón Continuar
        binding.btnContinuar.setOnClickListener {
            Log.e("TEST", "=== CLICK INICIADO ===")

            Toast.makeText(this, "Botón clickeado", Toast.LENGTH_SHORT).show()
            Log.e("TEST", "Toast mostrado")

            Log.e("TEST", "Contexto: ${this::class.simpleName}")

            try {
                val intent = Intent(this, Menu_principal_free::class.java)
                startActivity(intent)
                Log.e("TEST", "Nueva interfaz abierta correctamente")
            } catch (e: Exception) {
                Log.e("TEST", "ERROR al abrir nueva interfaz: ${e.message}")
            }
        }


    }

    private fun iniciarAnimacionSubida() {
        // 1. Hacer visible el CONTENEDOR PRINCIPAL
        binding.transicionContainer.visibility = View.VISIBLE

        // 2. Asegurar que el rectángulo está visible
        binding.rectanguloBlanco.visibility = View.VISIBLE

        // 3. Convertir 400dp a pixels
        val alturaDp = 400f
        val densidad = resources.displayMetrics.density
        val alturaPx = alturaDp * densidad

        // 4. Asegurar posición inicial (aunque ya está en 400dp en XML)
        binding.rectanguloBlanco.translationY = alturaPx

        // 5. Animar subida
        binding.rectanguloBlanco.animate()
            .translationY(0f)  // Sube a posición 0
            .setDuration(1000)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withStartAction {
                // Log para debug
                Log.d("ANIMACION", "Iniciando animación")
                Log.d("ANIMACION", "Posición inicial: ${binding.rectanguloBlanco.translationY}")

            }
            .withEndAction {
                Log.d("ANIMACION", "Animación completada")
            }
            .start()
    }
}