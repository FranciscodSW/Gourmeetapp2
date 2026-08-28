package com.example.gourmeet2

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.gourmeet2.databinding.ActivityPreferenciasDeCuentaBinding

class PreferenciasdeCuentaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPreferenciasDeCuentaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding =
            ActivityPreferenciasDeCuentaBinding.inflate(
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
        // OPCIONES
        // ==========================================

        binding.btnEditarPerfil.setOnClickListener {

            // Posteriormente abrir edición de perfil

        }


        binding.btnCambiarCorreo.setOnClickListener {

            // Posteriormente cambiar correo

        }


        binding.btnCambiarPassword.setOnClickListener {

            // Posteriormente cambiar contraseña

        }


        binding.btnCambiarTelefono.setOnClickListener {

            // Posteriormente cambiar teléfono

        }

        binding.btnEliminarCuenta.setOnClickListener {

            // Posteriormente eliminar cuenta

        }
    }
}