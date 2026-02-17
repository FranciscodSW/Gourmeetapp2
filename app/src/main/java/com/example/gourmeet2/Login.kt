package com.example.gourmeet2


import android.animation.*
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.gourmeet2.databinding.ActivityLoginBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior

class Login : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var behavior: BottomSheetBehavior<LinearLayout>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val anim = AnimationUtils.loadAnimation(this, R.anim.mover)
        val animSubir = AnimationUtils.loadAnimation(this, R.anim.subir)

        binding.imgTopLeft.startAnimation(anim)
        binding.imgTopRight.startAnimation(anim)
        binding.imgFondo.startAnimation(animSubir)

        behavior = BottomSheetBehavior.from(binding.bottomSheet)

        // ===============================
        // 🔴 Animaciones dinámicas
        // ===============================
        val containerAnimator = ObjectAnimator.ofFloat(
            binding.bottomSheet,
            View.TRANSLATION_Y,
            0f,
            -150f   // Ajusta la altura del brinco aquí
        ).apply {
            duration = 900
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = ObjectAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }

        containerAnimator.start()




        val titleNormal = 30f
        val subtitleNormal = 30f
        val titleMin = 20f
        val subtitleMin = 22f
        behavior.peekHeight = 100
        binding.btnRegistrar.setOnClickListener {

            val intent = Intent(this, Registro::class.java)
            startActivity(intent)

            overridePendingTransition(
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
        }


        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
              // altura visible cuando está cerrado


            override fun onSlide(bottomSheet: View, slideOffset: Float) {

                binding.txtBienvenido.textSize =
                    titleNormal - (titleNormal - titleMin) * slideOffset

                binding.txtSubtitulo.textSize =
                    subtitleNormal - (subtitleNormal - subtitleMin) * slideOffset

                binding.imgTopSheet.alpha = 1f - slideOffset

                val textTranslation = -50f * slideOffset
                binding.txtBienvenido.translationY = textTranslation
                binding.txtSubtitulo.translationY = textTranslation

                if (containerAnimator.isRunning) containerAnimator.cancel()


                binding.sheetContent.translationY = 0f
                binding.bottomSheet.scaleX = 1f
                binding.bottomSheet.scaleY = 1f
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {

                when (newState) {

                    BottomSheetBehavior.STATE_COLLAPSED -> {

                        binding.txtTituloSheet.text = "Bienvenido"

                        if (!containerAnimator.isRunning) containerAnimator.start()

                    }

                    BottomSheetBehavior.STATE_EXPANDED -> {

                        binding.txtTituloSheet.text = "Iniciar sesión"

                        containerAnimator.cancel()

                    }
                }
            }
        })

        binding.btnsinCuenta.setOnClickListener {
            mostrarDialogoPrivacidad()
        }
    }


    private fun mostrarDialogoPrivacidad() {
        val dialogView = layoutInflater.inflate(R.layout.aceptar_terminos, null)

        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
        builder.setCancelable(false) // No permite cerrar tocando fuera

        val checkAcepto = dialogView.findViewById<CheckBox>(R.id.checkAcepto)
        val btnContinuar = dialogView.findViewById<Button>(R.id.btnContinuar)
        val txtTerminos = dialogView.findViewById<TextView>(R.id.txtTerminos)

        // Configurar el texto con enlace que abre términos COMPLETOS
        configurarTextoConEnlaceCompleto(txtTerminos)

        btnContinuar.isEnabled = false

        checkAcepto.setOnCheckedChangeListener { _, isChecked ->
            btnContinuar.isEnabled = isChecked
        }

        val dialog = builder.create()

        btnContinuar.setOnClickListener {
            dialog.dismiss()
            ocultarPaginaAnterior()
        }

        dialog.show()
    }

    private fun ocultarPaginaAnterior() {
        // Crear un conjunto de animaciones para ocultar todos los elementos
        val animatorSet = AnimatorSet()

        // Lista de animaciones
        val animaciones = mutableListOf<Animator>()

        // Animación para el BottomSheet
        val bottomSheetAnim = ObjectAnimator.ofPropertyValuesHolder(
            binding.bottomSheet,
            PropertyValuesHolder.ofFloat("alpha", 1f, 0f),
            PropertyValuesHolder.ofFloat("translationY", 0f, 100f)
        ).apply {
            duration = 500
            interpolator = AccelerateInterpolator()
        }
        animaciones.add(bottomSheetAnim)

        // Animación para la imagen superior
        val imgTopAnim = ObjectAnimator.ofFloat(
            binding.imgTopSheet,
            "alpha",
            1f, 0f
        ).apply {
            duration = 400
        }
        animaciones.add(imgTopAnim)

        // Animación para las imágenes decorativas
        val imgTopLeftAnim = ObjectAnimator.ofFloat(
            binding.imgTopLeft,
            "alpha",
            1f, 0f
        ).apply {
            duration = 400
        }
        animaciones.add(imgTopLeftAnim)

        val imgTopRightAnim = ObjectAnimator.ofFloat(
            binding.imgTopRight,
            "alpha",
            1f, 0f
        ).apply {
            duration = 400
        }
        animaciones.add(imgTopRightAnim)

        val imgFondoAnim = ObjectAnimator.ofFloat(
            binding.imgFondo,
            "alpha",
            1f, 0f
        ).apply {
            duration = 400
        }
        val tituloAnim = ObjectAnimator.ofFloat(
            binding.txtTituloSheet,
            "alpha",
            1f, 0f
        ).apply {
            duration =400
        }


        val subtituloAnim = ObjectAnimator.ofFloat(
            binding.txtSubtitulo,
            "alpha",
            1f, 0f
        ).apply {
            duration =400
        }
        //animaciones.add(subtituloAnim)
        // animaciones.add(tituloAnim)
        animaciones.add(imgFondoAnim)
        // Ejecutar todas las animaciones juntas
        animatorSet.playTogether(animaciones)

        // Cuando terminen, iniciar la transición con vapor
        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // Ocultar todos los elementos
                binding.bottomSheet.visibility = View.GONE
                binding.imgTopSheet.visibility = View.GONE
                binding.imgTopLeft.visibility = View.GONE
                binding.imgTopRight.visibility = View.GONE
                binding.imgFondo.visibility = View.GONE
                //binding.txtTituloSheet.visibility= View.GONE
                //binding.txtSubtitulo.visibility= View.GONE
                iniciarTransicionDeCarga()


            }
        })

        animatorSet.start()
    }


    private fun configurarTextoConEnlaceCompleto(textView: TextView) {
        val textoCompleto = "Acepto los términos y condiciones de uso"
        val spannable = SpannableString(textoCompleto)

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                // Mostrar términos COMPLETOS en un nuevo diálogo
                mostrarTerminosCompletos()
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = Color.parseColor("#0E90E4") // Color azul
                ds.isUnderlineText = true
            }
        }

        // Hacer "términos y condiciones de uso" clickeable
        val startIndex = textoCompleto.indexOf("términos")
        val endIndex = textoCompleto.length

        if (startIndex != -1) {
            spannable.setSpan(
                clickableSpan,
                startIndex,
                endIndex,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        textView.text = spannable
        textView.movementMethod = LinkMovementMethod.getInstance()
        textView.highlightColor = Color.TRANSPARENT
    }


    private fun iniciarTransicionDeCarga() {
        // Crear intent para la nueva actividad
        val intent = Intent(this, Trancicion_de_carga_para_menu_free::class.java)

        // Iniciar la actividad
        startActivity(intent)

        // Transición suave entre actividades
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

        // Opcional: Cerrar esta actividad si ya no es necesaria
        // finish()
    }

    private fun mostrarTerminosCompletos() {
        val dialogView = layoutInflater.inflate(R.layout.terminos_completos, null)

        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)

        val scrollView = dialogView.findViewById<ScrollView>(R.id.scrollView)
        val txtTerminosCompletos = dialogView.findViewById<TextView>(R.id.txtTerminosCompletos)
        val btnCerrar = dialogView.findViewById<Button>(R.id.btnCerrar)

        // TEXTO COMPLETO DE TÉRMINOS Y CONDICIONES
        val terminosTexto = """
        TÉRMINOS Y CONDICIONES DE USO 
        
        1TÉRMINOS Y CONDICIONES DE USO DE GOURMEET
        Los presentes Términos y Condiciones de Uso (en lo sucesivo, los “TÉRMINOS Y CONDICIONES”) regulan de manera integral el acceso, navegación, uso y, en su caso, la interacción del usuario con el sitio web www.gourmeet.com.mx (en lo sucesivo, el “SITIO WEB”), el cual es propiedad y está operado por GourMeet (en lo sucesivo, el “TITULAR”).
        Al acceder, navegar o utilizar el SITIO WEB, toda persona (en lo sucesivo, el “USUARIO”) manifiesta expresa e inequívocamente que ha leído, entendido y aceptado sujetarse a lo dispuesto en los presentes TÉRMINOS Y CONDICIONES, así como al Aviso de Privacidad, la Política de Cookies y cualesquiera otras políticas, lineamientos o avisos legales complementarios que el TITULAR publique o ponga a disposición.
        El uso del SITIO WEB constituye un consentimiento expreso para vincularse jurídicamente conforme a lo aquí estipulado. En caso de no estar de acuerdo, el USUARIO deberá abstenerse de acceder o utilizar el SITIO WEB y sus funcionalidades.
        I. OBJETO
        El presente documento tiene como finalidad establecer los derechos, obligaciones, limitaciones y alcances que rigen el acceso, navegación y uso del SITIO WEB, incluyendo, de forma enunciativa pero no limitativa:
        1.	Recetas de cocina de acceso libre y gratuito para consulta pública.
        2.	Recomendaciones personalizadas de restaurantes y establecimientos gastronómicos cercanos al USUARIO, obtenidas mediante algoritmos y criterios del TITULAR.
        3.	Contenido editorial y multimedia relacionado con gastronomía, técnicas culinarias, cultura gastronómica y estilo de vida culinario.
        El acceso general al SITIO WEB es gratuito, no obstante, algunos servicios, funcionalidades o contenidos podrían requerir registro previo, autenticación de cuenta, aceptación de condiciones particulares o pago de tarifas que se informarán previamente.
        II. TITULARIDAD Y LEGISLACIÓN APLICABLE
        Titular: Job Isaac Gutierrez Hernandez                                                                                      Dirección: Francisco I. Madero 15, Delegación Santa María Totoltepec, 50240 Santa María Totoltepec, Méx.                                                                                                                 Página web: https://www.gourmeet.com.mx                                                                                                         
        Correo electrónico de contacto: soporte@gourmeet.com.mx
        Teléfono de contacto: 722 889 1315
        El SITIO WEB y sus contenidos se rigen, interpretan y ejecutan de conformidad con la legislación vigente en los Estados Unidos Mexicanos.
        Cualquier acto jurídico, transacción, reclamación o controversia que derive directa o indirectamente del acceso, navegación o uso del SITIO WEB se someterá, para su interpretación y cumplimiento, a las leyes mexicanas.
        Las partes acuerdan que cualquier disputa será resuelta por los tribunales competentes en el Estado de México, renunciando expresamente a cualquier otro fuero que pudiera corresponderles en razón de su domicilio presente o futuro o por cualquier otra causa.
        III. CONDICIÓN DE USUARIO Y RESPONSABILIDADES
        El acceso y/o uso del SITIO WEB confiere la condición de USUARIO, lo que implica la aceptación plena y sin reservas de lo aquí establecido. El USUARIO se compromete a:
        1.	Cumplir con la ley, la moral, el orden público y las buenas costumbres, así como con lo dispuesto en los presentes TÉRMINOS Y CONDICIONES.
        2.	No realizar actos ilícitos, ofensivos, difamatorios, fraudulentos o lesivos contra el TITULAR, otros usuarios o terceros.
        3.	Proporcionar información veraz, completa y actualizada en los procesos de registro o interacción con el SITIO WEB.
        4.	No introducir virus, malware, código malicioso o cualquier mecanismo que pueda dañar o alterar el funcionamiento del SITIO WEB o los sistemas del TITULAR.
        El uso del SITIO WEB no genera relación contractual, laboral, comercial, de asociación o sociedad entre el TITULAR y el USUARIO, salvo disposición expresa en contrario.
        IV. FUNCIONALIDADES DEL SITIO WEB
        1.	Recetas de cocina. Las recetas pueden ser elaboradas por el equipo del TITULAR, usuarios colaboradores o fuentes autorizadas. El TITULAR no garantiza la exactitud, seguridad o resultados de la preparación, ni se hace responsable de reacciones adversas o resultados distintos a los esperados.
        2.	Recomendaciones gastronómicas. Las sugerencias de restaurantes u otros establecimientos son meramente informativas y no constituyen garantía, aval, patrocinio ni responsabilidad sobre calidad, disponibilidad o seguridad de los servicios ofrecidos por dichos terceros.
        V. USO DE UBICACIÓN Y DATOS
        El SITIO WEB podrá solicitar acceso a la ubicación del USUARIO para ofrecer recomendaciones personalizadas. Este acceso es opcional y puede deshabilitarse en la configuración del dispositivo o navegador.
        El tratamiento de datos personales se realizará conforme a lo establecido en el Aviso de Privacidad y en la Ley Federal de Protección de Datos Personales en Posesión de los Particulares.
        VI. CONTENIDO GENERADO POR USUARIOS
        El SITIO WEB podrá permitir que los USUARIOS publiquen recetas, comentarios, fotografías u otros materiales (en adelante, el “CONTENIDO”).
        El USUARIO garantiza que posee todos los derechos necesarios sobre dicho CONTENIDO y concede al TITULAR una licencia no exclusiva, gratuita, transferible y por tiempo indefinido para reproducir, distribuir, transformar, comunicar públicamente y explotar dicho material en el SITIO WEB y en los canales oficiales del TITULAR.
        El TITULAR podrá eliminar, sin previo aviso, cualquier CONTENIDO que:
        •	Sea ilícito, ofensivo, difamatorio o inadecuado.
        •	Vulnere derechos de terceros.
        •	Contravenga estos TÉRMINOS Y CONDICIONES.
        VII. PROPIEDAD INTELECTUAL E INDUSTRIAL
        Todos los elementos del SITIO WEB, incluidos textos, recetas, fotografías, videos, diseños, logotipos, marcas, nombres comerciales, código fuente y demás elementos protegidos por la ley, son propiedad del TITULAR o de terceros con licencia.
        Queda estrictamente prohibida su reproducción, distribución, modificación o explotación con fines comerciales sin autorización previa y por escrito.
        El USUARIO solo podrá descargar o imprimir contenidos para uso estrictamente personal y no comercial.
        VIII. DISPONIBILIDAD Y LIMITACIÓN DE RESPONSABILIDAD
        El TITULAR realizará esfuerzos razonables para mantener el SITIO WEB disponible y operativo, sin embargo, no garantiza disponibilidad ininterrumpida ni libre de errores.
        El TITULAR no será responsable por:
        •	Fallos técnicos, interrupciones o pérdida de datos.
        •	Daños derivados del uso del SITIO WEB o de la confianza en su contenido.
        •	Actos u omisiones de terceros, incluidos establecimientos recomendados.
        IX. POLÍTICA DE ENLACES
        El SITIO WEB puede contener enlaces a sitios externos. El TITULAR no es responsable de su contenido, seguridad o disponibilidad. El acceso a dichos enlaces será bajo riesgo exclusivo del USUARIO.
        X. USO DE COOKIES
        El SITIO WEB utiliza cookies y tecnologías similares para mejorar la experiencia del USUARIO. El uso de cookies puede gestionarse desde el navegador. Más detalles se encuentran en la Política de Cookies.
        XI. MODIFICACIONES
        El TITULAR podrá modificar en cualquier momento los presentes TÉRMINOS Y CONDICIONES. Las modificaciones serán publicadas en el SITIO WEB y entrarán en vigor desde su fecha de publicación.
        XII. CONTACTO
        Para dudas, aclaraciones o comentarios, el USUARIO podrá comunicarse a:
        •	Correo electrónico: soporte@gourmeet.com.mx
        •	Ubicación: Francisco I. Madero 15, Delegación Santa María Totoltepec, 50240 Santa María Totoltepec, Méx.
        •	Teléfono: 722 889 1315
        •   Última actualización: 14 de agosto de 2025

    """.trimIndent()

        txtTerminosCompletos.text = terminosTexto

        val dialog = builder.create()

        btnCerrar.setOnClickListener {
            dialog.dismiss()
        }

        // Tamaño del diálogo
        dialog.window?.setLayout(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            (resources.displayMetrics.heightPixels * 0.85).toInt()
        )

        dialog.show()
    }


}