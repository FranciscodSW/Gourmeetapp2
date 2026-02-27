package com.example.gourmeet2

import android.content.Intent
import android.graphics.drawable.Drawable
import android.location.Geocoder
import android.net.wifi.WifiManager
import android.os.Bundle
import android.transition.Transition
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

import com.example.gourmeet2.data.api.ApiClient
import com.example.gourmeet2.data.models.UsuarioRegistro
import com.example.gourmeet2.databinding.ActivityRegistroBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.sql.DataSource
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target

class Registro : AppCompatActivity() {
    private val MAP_REQUEST = 100
    private var avatarSeleccionadoUrl: String = ""

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
        binding.btnUbicacion.setOnClickListener {
            val intent = Intent(this, MapaSeleccionActivity::class.java)
            startActivityForResult(intent, MAP_REQUEST)
        }
        binding.imgUsuario.setOnClickListener {
            mostrarSelectorAvatar()
        }

        setupValidaciones()
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == MAP_REQUEST && resultCode == RESULT_OK) {

            val lat = data?.getDoubleExtra("lat", 0.0)
            val lng = data?.getDoubleExtra("lng", 0.0)

            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat!!, lng!!, 1)

            if (!addresses.isNullOrEmpty()) {
                val direccion = addresses[0].getAddressLine(0)

                binding.layoutDireccion.visibility = View.VISIBLE
                binding.txtDireccion.text = direccion
            }
        }
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
            mostrarExito(nombre)
           // registrarUsuario(nombre, correo, pass)
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
                        mostrarExito(nombre)
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

    private fun mostrarExito(nombre: String) {

        binding.layoutRegistro.visibility = View.GONE
        binding.layoutPersonalizar.visibility = View.VISIBLE

        binding.txtNombreUsuario.text = nombre
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
    private fun mostrarSelectorAvatar() {
        Log.d("AVATAR_DEBUG", "Se abrió el selector")

        val dialogView = layoutInflater.inflate(R.layout.dialog_avatar, null)
        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.radioGroupGenero)
        val grid = dialogView.findViewById<GridLayout>(R.id.gridAvatares)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        fun cargarAvatares(genero: String) {
            Log.d("AVATAR_DEBUG", "Cargando avatares género: $genero")

            grid.removeAllViews()

            for (i in 1..5) {

                val imageView = ImageView(this)

                val params = GridLayout.LayoutParams()
                params.width = 250
                params.height = 250
                params.setMargins(16, 16, 16, 16)



                imageView.layoutParams = params
                imageView.scaleType = ImageView.ScaleType.CENTER_CROP

                // 🔥 Obtener nombre dinámico del recurso
                val nombreImagen = if (genero == "M") {
                    "ic_usuario_$i"
                } else {
                    "ic_usuario_f$i"
                }

                val resourceId = resources.getIdentifier(
                    nombreImagen,
                    "drawable",
                    packageName
                )

                imageView.setImageResource(resourceId)

                imageView.setOnClickListener {

                    avatarSeleccionadoUrl = nombreImagen

                    val resourceId = resources.getIdentifier(
                        nombreImagen,
                        "drawable",
                        packageName
                    )

                    binding.imgUsuario.setImageResource(resourceId)

                    dialog.dismiss()
                }

                grid.addView(imageView)
            }

            Log.d("AVATAR_DEBUG", "Total hijos en Grid: ${grid.childCount}")
        }

        // Carga inicial masculino
        cargarAvatares("M")

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbMasculino) {
                cargarAvatares("M")
            } else {
                cargarAvatares("F")
            }
        }

        dialog.show()
    }

}
