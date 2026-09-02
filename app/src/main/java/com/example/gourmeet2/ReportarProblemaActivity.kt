package com.example.gourmeet2

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.gourmeet2.data.api.ApiClient
import com.example.gourmeet2.data.models.ReporteProblemaRequest
import com.example.gourmeet2.databinding.ActivityReportarProblemaBinding
import com.example.gourmeet2.utils.SesionUsuario
import kotlinx.coroutines.launch

class ReportarProblemaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportarProblemaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityReportarProblemaBinding.inflate(
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
        // TIPOS DE PROBLEMA
        // ==========================================

        val tiposProblema = arrayOf(

            "Error en la aplicación",
            "Problema con una receta",
            "Problema con el buscador",
            "Problema con un proveedor",
            "Problema con la ubicación",
            "Problema con mi cuenta",
            "Problema con Premium",
            "Sugerencia",
            "Otro"

        )


        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            tiposProblema
        )


        binding.actTipoProblema.setAdapter(adapter)


        binding.actTipoProblema.setOnClickListener {

            binding.actTipoProblema.showDropDown()

        }


        // ==========================================
        // BOTÓN ENVIAR
        // ==========================================

        binding.btnEnviarReporte.setOnClickListener {

            enviarReporte()

        }

    }


    // ==========================================
    // ENVIAR REPORTE
    // ==========================================

    private fun enviarReporte() {

        val tipo = binding.actTipoProblema.text
            .toString()
            .trim()

        val descripcion = binding.edtDescripcion.text
            .toString()
            .trim()


        // ==========================================
        // VALIDAR TIPO
        // ==========================================

        if (tipo.isEmpty()) {

            binding.tilTipoProblema.error =
                "Selecciona un tipo de problema"

            return
        }

        binding.tilTipoProblema.error = null


        // ==========================================
        // VALIDAR DESCRIPCIÓN
        // ==========================================

        if (descripcion.isEmpty()) {

            binding.tilDescripcion.error =
                "Describe el problema"

            return
        }

        binding.tilDescripcion.error = null


        // ==========================================
        // OBTENER USUARIO
        // ==========================================

        val usuarioId = SesionUsuario.obtenerId(this)


        // ==========================================
        // VALIDAR SESIÓN
        // ==========================================

        if (usuarioId <= 0) {

            Toast.makeText(
                this,
                "No se pudo identificar al usuario",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        // ==========================================
        // CREAR REQUEST
        // ==========================================

        val request = ReporteProblemaRequest(

            RP_USUARIO_ID = usuarioId,

            RP_TIPO = tipo,

            RP_DESCRIPCION = descripcion

        )


        // ==========================================
        // DESHABILITAR BOTÓN
        // ==========================================

        binding.btnEnviarReporte.isEnabled = false


        // ==========================================
        // LLAMAR API
        // ==========================================

        lifecycleScope.launch {

            try {

                val response =
                    ApiClient.apiService.crearReporteProblema(request)


                // ==================================
                // RESPUESTA EXITOSA
                // ==================================

                if (response.success) {

                    Toast.makeText(
                        this@ReportarProblemaActivity,
                        response.message
                            ?: "Reporte enviado correctamente",
                        Toast.LENGTH_SHORT
                    ).show()


                    // CERRAR VENTANA
                    finish()

                } else {

                    Toast.makeText(
                        this@ReportarProblemaActivity,
                        response.message
                            ?: "No se pudo enviar el reporte",
                        Toast.LENGTH_LONG
                    ).show()
                    binding.btnEnviarReporte.isEnabled = true
                }
            } catch (e: Exception) {

                Toast.makeText(
                    this@ReportarProblemaActivity,
                    "Error de conexión con el servidor",
                    Toast.LENGTH_LONG
                ).show()


                binding.btnEnviarReporte.isEnabled = true

            }

        }

    }

}