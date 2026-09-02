package com.example.gourmeet2

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.gourmeet2.data.api.ApiClient
import com.example.gourmeet2.data.models.CambiarNombreRequest
import com.example.gourmeet2.databinding.ActivityPreferenciasDeCuentaBinding
import com.example.gourmeet2.utils.SesionUsuario
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class PreferenciasdeCuentaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPreferenciasDeCuentaBinding

    private var nombreUsuarioActual = ""


    // ==========================================================
    // ON CREATE
    // ==========================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding =
            ActivityPreferenciasDeCuentaBinding.inflate(
                layoutInflater
            )

        setContentView(binding.root)


        // ======================================================
        // OBTENER NOMBRE ACTUAL
        // ======================================================

        nombreUsuarioActual =
            SesionUsuario.obtenerNombre(this)

        Log.d(
            "PREFERENCIAS",
            "Activity iniciada"
        )

        Log.d(
            "PREFERENCIAS",
            "Nombre actual: $nombreUsuarioActual"
        )


        // ======================================================
        // BOTÓN REGRESAR
        // ======================================================

        binding.btnRegresar.setOnClickListener {

            Log.d(
                "PREFERENCIAS",
                "CLICK REGRESAR"
            )

            finish()
        }


        // ======================================================
        // CAMBIAR NOMBRE
        // ======================================================

        binding.btnCambiarNombre.setOnClickListener {

            Log.d(
                "PREFERENCIAS",
                "CLICK CAMBIAR NOMBRE"
            )

            mostrarDialogoCambiarNombre()
        }


        // ======================================================
        // TÉRMINOS Y CONDICIONES
        // ======================================================

        binding.btnTerminosCondiciones.setOnClickListener {

            Log.d(
                "PREFERENCIAS",
                "CLICK TÉRMINOS"
            )

            mostrarTerminosCompletos()
        }


        // ======================================================
        // PREMIUM
        // ======================================================

        binding.btnPremium.setOnClickListener {

            Log.d(
                "PREFERENCIAS",
                "CLICK PREMIUM"
            )

            startActivity(
                Intent(
                    this,
                    PremiumActivity::class.java
                )
            )
        }


        // ======================================================
        // REPORTAR PROBLEMA
        // ======================================================

        binding.btnReportarproblemas.setOnClickListener {

            Log.d(
                "PREFERENCIAS",
                "CLICK REPORTAR PROBLEMA"
            )

            // Por el momento solamente comprobamos
            // que el botón funciona.

            Toast.makeText(
                this,
                "Reportar problema",
                Toast.LENGTH_SHORT
            ).show()

            /*
            // Posteriormente:

            startActivity(
                Intent(
                    this,
                    ReportarProblemaActivity::class.java
                )
            )
            */
        }
    }


    // ==========================================================
    // CAMBIAR NOMBRE
    // ==========================================================

    private fun mostrarDialogoCambiarNombre() {

        val dialog =
            Dialog(this)


        // ======================================================
        // CARGAR DISEÑO
        // ======================================================

        val vista =
            layoutInflater.inflate(
                R.layout.dialog_cambiar_nombre,
                null
            )


        dialog.setContentView(vista)


        // ======================================================
        // FONDO TRANSPARENTE
        // ======================================================

        dialog.window?.setBackgroundDrawableResource(
            android.R.color.transparent
        )


        // ======================================================
        // ELEMENTOS
        // ======================================================

        val edtNombre =
            vista.findViewById<EditText>(
                R.id.edtNuevoNombre
            )


        val btnGuardar =
            vista.findViewById<MaterialButton>(
                R.id.btnGuardarNombre
            )


        val btnCancelar =
            vista.findViewById<MaterialButton>(
                R.id.btnCancelarNombre
            )


        // ======================================================
        // NOMBRE ACTUAL
        // ======================================================

        edtNombre.setText(
            nombreUsuarioActual
        )

        edtNombre.setSelection(
            edtNombre.text.length
        )


        // ======================================================
        // CANCELAR
        // ======================================================

        btnCancelar.setOnClickListener {

            Log.d(
                "PREFERENCIAS",
                "CANCELAR CAMBIO DE NOMBRE"
            )

            dialog.dismiss()
        }


        // ======================================================
        // GUARDAR
        // ======================================================

        btnGuardar.setOnClickListener {

            val nuevoNombre =
                edtNombre.text
                    .toString()
                    .trim()


            // ==================================================
            // VALIDAR
            // ==================================================

            if (nuevoNombre.isEmpty()) {

                edtNombre.error =
                    "Escribe un nombre"

                return@setOnClickListener
            }


            if (nuevoNombre.length < 2) {

                edtNombre.error =
                    "El nombre debe tener al menos 2 caracteres"

                return@setOnClickListener
            }


            if (nuevoNombre.length > 50) {

                edtNombre.error =
                    "El nombre no puede superar los 50 caracteres"

                return@setOnClickListener
            }


            // ==================================================
            // OBTENER ID DEL USUARIO
            // ==================================================

            val clienteId =
                SesionUsuario.obtenerId(this)


            if (clienteId <= 0) {

                Toast.makeText(
                    this,
                    "No se encontró el usuario.",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }


            // ==================================================
            // DESHABILITAR BOTÓN
            // ==================================================

            btnGuardar.isEnabled = false


            // ==================================================
            // CREAR REQUEST
            // ==================================================

            val datos =
                CambiarNombreRequest(

                    CLI_ID =
                        clienteId,

                    CLI_NOMBRE =
                        nuevoNombre
                )


            Log.d(
                "PREFERENCIAS",
                "Actualizando nombre: $nuevoNombre"
            )


            // ==================================================
            // LLAMAR API
            // ==================================================

            lifecycleScope.launch {

                try {

                    val respuesta =
                        ApiClient.apiService
                            .cambiarNombreUsuario(
                                datos
                            )


                    // ==================================================
                    // RESPUESTA EXITOSA
                    // ==================================================

                    if (respuesta.success) {

                        val nombreActualizado =
                            respuesta.CLI_NOMBRE
                                ?: nuevoNombre


                        // ==============================================
                        // ACTUALIZAR VARIABLE
                        // ==============================================

                        nombreUsuarioActual =
                            nombreActualizado


                        // ==============================================
                        // ACTUALIZAR SESIÓN
                        // ==============================================

                        SesionUsuario.actualizarNombre(
                            this@PreferenciasdeCuentaActivity,
                            nombreActualizado
                        )


                        Log.d(
                            "PREFERENCIAS",
                            "Nombre actualizado: $nombreActualizado"
                        )


                        // ==============================================
                        // MENSAJE
                        // ==============================================

                        Toast.makeText(
                            this@PreferenciasdeCuentaActivity,
                            respuesta.mensaje
                                ?: "Nombre actualizado correctamente.",
                            Toast.LENGTH_SHORT
                        ).show()


                        // ==============================================
                        // CERRAR
                        // ==============================================

                        dialog.dismiss()

                    } else {

                        Toast.makeText(
                            this@PreferenciasdeCuentaActivity,
                            respuesta.mensaje
                                ?: "No se pudo actualizar el nombre.",
                            Toast.LENGTH_SHORT
                        ).show()


                        btnGuardar.isEnabled =
                            true
                    }

                } catch (e: Exception) {

                    Log.e(
                        "PREFERENCIAS",
                        "Error al actualizar nombre",
                        e
                    )


                    Toast.makeText(
                        this@PreferenciasdeCuentaActivity,
                        "Error al actualizar el nombre.",
                        Toast.LENGTH_SHORT
                    ).show()


                    btnGuardar.isEnabled =
                        true
                }
            }
        }


        // ======================================================
        // MOSTRAR DIÁLOGO
        // ======================================================

        dialog.show()


        // ======================================================
        // TAMAÑO
        // ======================================================

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }


    // ==========================================================
    // TÉRMINOS Y CONDICIONES
    // ==========================================================

    private fun mostrarTerminosCompletos() {

        val dialogView =
            layoutInflater.inflate(
                R.layout.terminos_completos,
                null
            )


        val builder =
            AlertDialog.Builder(this)


        builder.setView(dialogView)


        // ======================================================
        // ELEMENTOS
        // ======================================================

        val scrollView =
            dialogView.findViewById<ScrollView>(
                R.id.scrollView
            )


        val txtTerminosCompletos =
            dialogView.findViewById<TextView>(
                R.id.txtTerminosCompletos
            )


        val btnCerrar =
            dialogView.findViewById<Button>(
                R.id.btnCerrar
            )


        // ======================================================
        // TÉRMINOS
        // ======================================================

        val terminosTexto = """
        
        TÉRMINOS Y CONDICIONES DE USO
        
        1. TÉRMINOS Y CONDICIONES DE USO DE GOURMEET
        
        Los presentes Términos y Condiciones de Uso (en lo sucesivo, los “TÉRMINOS Y CONDICIONES”) regulan de manera integral el acceso, navegación, uso y, en su caso, la interacción del usuario con el sitio web www.gourmeet.com.mx (en lo sucesivo, el “SITIO WEB”), el cual es propiedad y está operado por GourMeet (en lo sucesivo, el “TITULAR”).
        
        Al acceder, navegar o utilizar el SITIO WEB, toda persona (en lo sucesivo, el “USUARIO”) manifiesta expresa e inequívocamente que ha leído, entendido y aceptado sujetarse a lo dispuesto en los presentes TÉRMINOS Y CONDICIONES, así como al Aviso de Privacidad, la Política de Cookies y cualesquiera otras políticas, lineamientos o avisos legales complementarios que el TITULAR publique o ponga a disposición.
        
        El uso del SITIO WEB constituye un consentimiento expreso para vincularse jurídicamente conforme a lo aquí estipulado. En caso de no estar de acuerdo, el USUARIO deberá abstenerse de acceder o utilizar el SITIO WEB y sus funcionalidades.
        
        I. OBJETO
        
        El presente documento tiene como finalidad establecer los derechos, obligaciones, limitaciones y alcances que rigen el acceso, navegación y uso del SITIO WEB, incluyendo, de forma enunciativa pero no limitativa:
        
        1. Recetas de cocina de acceso libre y gratuito para consulta pública.
        
        2. Recomendaciones personalizadas de restaurantes y establecimientos gastronómicos cercanos al USUARIO, obtenidas mediante algoritmos y criterios del TITULAR.
        
        3. Contenido editorial y multimedia relacionado con gastronomía, técnicas culinarias, cultura gastronómica y estilo de vida culinario.
        
        El acceso general al SITIO WEB es gratuito, no obstante, algunos servicios, funcionalidades o contenidos podrían requerir registro previo, autenticación de cuenta, aceptación de condiciones particulares o pago de tarifas que se informarán previamente.
        
        II. TITULARIDAD Y LEGISLACIÓN APLICABLE
        
        Titular: Job Isaac Gutierrez Hernandez
        
        Dirección:
        Francisco I. Madero 15, Delegación Santa María Totoltepec, 50240 Santa María Totoltepec, Méx.
        
        Página web:
        https://www.gourmeet.com.mx
        
        Correo electrónico:
        soporte@gourmeet.com.mx
        
        Teléfono:
        722 889 1315
        
        El SITIO WEB y sus contenidos se rigen, interpretan y ejecutan de conformidad con la legislación vigente en los Estados Unidos Mexicanos.
        
        Cualquier acto jurídico, transacción, reclamación o controversia que derive directa o indirectamente del acceso, navegación o uso del SITIO WEB se someterá, para su interpretación y cumplimiento, a las leyes mexicanas.
        
        III. CONDICIÓN DE USUARIO Y RESPONSABILIDADES
        
        El acceso y/o uso del SITIO WEB confiere la condición de USUARIO, lo que implica la aceptación plena y sin reservas de lo aquí establecido.
        
        El USUARIO se compromete a:
        
        1. Cumplir con la ley, la moral, el orden público y las buenas costumbres.
        
        2. No realizar actos ilícitos, ofensivos, difamatorios, fraudulentos o lesivos.
        
        3. Proporcionar información veraz, completa y actualizada.
        
        4. No introducir virus, malware, código malicioso o cualquier mecanismo que pueda dañar el funcionamiento del SITIO WEB.
        
        IV. FUNCIONALIDADES DEL SITIO WEB
        
        1. Recetas de cocina.
        
        Las recetas pueden ser elaboradas por el equipo del TITULAR, usuarios colaboradores o fuentes autorizadas.
        
        El TITULAR no garantiza la exactitud, seguridad o resultados de la preparación.
        
        2. Recomendaciones gastronómicas.
        
        Las sugerencias de restaurantes u otros establecimientos son meramente informativas.
        
        V. USO DE UBICACIÓN Y DATOS
        
        El SITIO WEB podrá solicitar acceso a la ubicación del USUARIO para ofrecer recomendaciones personalizadas.
        
        Este acceso es opcional y puede deshabilitarse en la configuración del dispositivo.
        
        El tratamiento de datos personales se realizará conforme a lo establecido en el Aviso de Privacidad y en la Ley Federal de Protección de Datos Personales en Posesión de los Particulares.
        
        VI. CONTENIDO GENERADO POR USUARIOS
        
        El SITIO WEB podrá permitir que los USUARIOS publiquen recetas, comentarios, fotografías u otros materiales.
        
        El USUARIO garantiza que posee todos los derechos necesarios sobre dicho CONTENIDO.
        
        VII. PROPIEDAD INTELECTUAL E INDUSTRIAL
        
        Todos los elementos del SITIO WEB, incluidos textos, recetas, fotografías, videos, diseños, logotipos, marcas, nombres comerciales y código fuente son propiedad del TITULAR o de terceros con licencia.
        
        Queda estrictamente prohibida su reproducción, distribución, modificación o explotación con fines comerciales sin autorización previa.
        
        VIII. DISPONIBILIDAD Y LIMITACIÓN DE RESPONSABILIDAD
        
        El TITULAR realizará esfuerzos razonables para mantener el SITIO WEB disponible y operativo, sin embargo, no garantiza disponibilidad ininterrumpida ni libre de errores.
        
        IX. POLÍTICA DE ENLACES
        
        El SITIO WEB puede contener enlaces a sitios externos.
        
        El TITULAR no es responsable de su contenido, seguridad o disponibilidad.
        
        X. USO DE COOKIES
        
        El SITIO WEB utiliza cookies y tecnologías similares para mejorar la experiencia del USUARIO.
        
        XI. MODIFICACIONES
        
        El TITULAR podrá modificar en cualquier momento los presentes TÉRMINOS Y CONDICIONES.
        
        XII. CONTACTO
        
        Para dudas, aclaraciones o comentarios:
        
        Correo electrónico:
        soporte@gourmeet.com.mx
        
        Ubicación:
        Francisco I. Madero 15, Delegación Santa María Totoltepec, 50240 Santa María Totoltepec, Méx.
        
        Teléfono:
        722 889 1315
        
        Última actualización:
        14 de agosto de 2025
        
        """.trimIndent()


        txtTerminosCompletos.text =
            terminosTexto


        // ======================================================
        // CREAR DIÁLOGO
        // ======================================================

        val dialog =
            builder.create()


        // ======================================================
        // CERRAR
        // ======================================================

        btnCerrar.setOnClickListener {

            dialog.dismiss()
        }


        // ======================================================
        // MOSTRAR
        // ======================================================

        dialog.show()


        // ======================================================
        // TAMAÑO
        // ======================================================

        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (resources.displayMetrics.heightPixels * 0.85).toInt()
        )
    }
}