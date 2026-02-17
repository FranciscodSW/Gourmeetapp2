package com.example.gourmeet2

import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.gourmeet2.data.api.ApiClient
import com.example.gourmeet2.data.models.UsuarioRegistro
import com.example.gourmeet2.databinding.ActivityRegistroBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Registro : AppCompatActivity() {

    private lateinit var binding: ActivityRegistroBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        binding = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupValidaciones()
    }

    private fun setupValidaciones() {

        binding.btnRegistrar.setOnClickListener {

            val nombre = binding.editNombre.text.toString().trim()
            val correo = binding.editCorreo.text.toString().trim()
            val pass = binding.editPassword.text.toString()
            val confirmPass = binding.editConfirmPassword.text.toString()

            if (!validarNombre(nombre)) return@setOnClickListener
            if (!validarCorreo(correo)) return@setOnClickListener
            if (!validarPassword(pass)) return@setOnClickListener

            if (pass != confirmPass) {
                mostrarError("Las contraseñas no coinciden")
                return@setOnClickListener
            }

            registrarUsuario(nombre, correo, pass)
        }
    }
    private fun registrarUsuario(nombre: String, correo: String, pass: String) {

        val ip = obtenerIP()   // 🔥 AQUÍ

        binding.txtError.visibility = View.GONE

        CoroutineScope(Dispatchers.IO).launch {

            try {

                val usuario = UsuarioRegistro(
                    nombre = nombre,
                    correo = correo,
                    password = pass,
                    cliPrimerIp = ip   // 🔥 enviada
                )

                val response = ApiClient.apiService.registrarUsuario(usuario)

                withContext(Dispatchers.Main) {

                    if (response.success) {
                        mostrarExito()
                    } else {
                        mostrarError(response.error ?: "Error desconocido")
                    }
                }

            } catch (e: Exception) {

                withContext(Dispatchers.Main) {

                    mostrarError("Error de conexión")

                }
            }
        }
    }




    // =========================
    // ✅ VALIDAR NOMBRE
    // =========================
    private fun validarNombre(nombre: String): Boolean {

        if (nombre.isEmpty()) {
            mostrarError("Ingresa tu nombre")
            return false
        }

        val regex = Regex("^[A-ZÁÉÍÓÚÑ][a-záéíóúñA-ZÁÉÍÓÚÑ ]*$")

        if (!regex.matches(nombre)) {
            mostrarError("Nombre inválido. Solo letras y debe iniciar con mayúscula")
            return false
        }

        return true
    }

    // =========================
    // ✅ VALIDAR CORREO
    // =========================
    private fun validarCorreo(correo: String): Boolean {

        if (correo.isEmpty()) {
            mostrarError("Ingresa tu correo")
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            mostrarError("Correo inválido")
            return false
        }

        return true
    }

    // =========================
    // ✅ VALIDAR PASSWORD
    // =========================
    private fun validarPassword(pass: String): Boolean {

        if (pass.length < 8) {
            mostrarError("La contraseña debe tener mínimo 8 caracteres")
            return false
        }

        val regexMayuscula = Regex(".*[A-Z].*")

        if (!regexMayuscula.matches(pass)) {
            mostrarError("La contraseña debe contener al menos una mayúscula")
            return false
        }

        return true
    }

    // =========================
    // ✅ MENSAJES
    // =========================
    private fun mostrarError(msg: String) {

        binding.txtError.apply {
            text = msg
            visibility = View.VISIBLE
            alpha = 0f
            animate().alpha(1f).setDuration(250).start()
        }
    }

    private fun mostrarExito() {

        binding.txtError.visibility = View.GONE

        Toast.makeText(this, "Cuenta creada 🎉", Toast.LENGTH_SHORT).show()

        finish()
    }
    private fun obtenerIP(): String {
        return try {
            val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
            val ipInt = wifiManager.connectionInfo.ipAddress

            String.format(
                "%d.%d.%d.%d",
                ipInt and 0xff,
                ipInt shr 8 and 0xff,
                ipInt shr 16 and 0xff,
                ipInt shr 24 and 0xff
            )
        } catch (e: Exception) {
            "0.0.0.0"
        }
    }

}
