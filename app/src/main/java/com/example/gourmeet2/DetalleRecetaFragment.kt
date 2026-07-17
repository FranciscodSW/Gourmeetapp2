package com.example.gourmeet2

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.gourmeet2.databinding.FragmentDetalleRecetaBinding
import kotlinx.coroutines.launch
import com.example.gourmeet2.data.api.ApiClient
import com.example.gourmeet2.data.models.DetalleRecetaRequest
import com.example.gourmeet2.data.models.PasoPreparacion
import kotlin.math.abs
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Gravity
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import com.example.gourmeet2.data.models.*
import com.example.gourmeet2.utils.SesionUsuario
import java.util.Locale


class DetalleRecetaFragment : Fragment() {
    private lateinit var gestureDetector: GestureDetector
    private var _binding: FragmentDetalleRecetaBinding? = null
    private val binding get() = _binding!!
    private var recetaId: Int = 0
    private var mostrandoTodosLosPasos = false
    private var pasos = emptyList<PasoPreparacion>()
    private var pasoActual = 0
    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var recognizerIntent: Intent
    private var escuchando = false
    private var escuchaContinua = false
    private var lecturaActiva = false
    private var recetaTerminada = false
    private var textToSpeech: TextToSpeech? = null

    companion object {
        private const val ARG_RECETA = "REC_ID"
        fun newInstance(recetaId: Int): DetalleRecetaFragment {
            val fragment = DetalleRecetaFragment()
            val args = Bundle()
            args.putInt(ARG_RECETA, recetaId)
            fragment.arguments = args
            return fragment
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        recetaId = arguments?.getInt(ARG_RECETA) ?: 0
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetalleRecetaBinding.inflate(
            inflater,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        cargarFotoUsuario()
        cargarMiComentario()
        cargarDetalle()
        inicializarTextToSpeech()
        inicializarReconocimiento()
        verificarRecetaTerminada()
        gestureDetector = GestureDetector(
            requireContext(),
            object : GestureDetector.SimpleOnGestureListener() {

                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    val distancia = e2.x - (e1?.x ?: 0f)
                    if (abs(distancia) > 150) {
                        if (distancia < 0) {
                            siguientePaso()
                        } else {
                            pasoAnterior()
                        }
                        return true
                    }
                    return false
                }
            }
        )
        binding.btnAnterior.setOnClickListener {
            pasoAnterior()
        }
        binding.btnSiguiente.setOnClickListener{
            siguientePaso()
        }
        binding.btnVerTodo.setOnClickListener {

            if (mostrandoTodosLosPasos) {

                ocultarTodosLosPasos()

            } else {

                mostrarTodosLosPasos()

            }

        }
        binding.cardPreparacion.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
        binding.btnRecetaTerminada.setOnClickListener{
            marcarRecetaTerminada()
        }
        binding.btnVoz.setOnClickListener {

            if (!escuchaContinua) {

                escuchaContinua = true

                binding.btnVoz.icon =
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_microfono_on
                    )

                iniciarEscucha()

            } else {

                escuchaContinua = false

                binding.btnVoz.icon =
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_microfono
                    )
                detenerEscucha()
            }
        }
        binding.editComentario.addTextChangedListener {

            validarComentario()

        }
        binding.ratingComentario.setOnRatingBarChangeListener {

                _, _, _ ->

            validarComentario()

        }
        binding.btnComentar.setOnClickListener {

            enviarComentario()

        }
        binding.btnParlante.setOnClickListener {
            if (!lecturaActiva) {
                lecturaActiva = true
                binding.btnParlante.icon =
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_audio_on
                    )
                iniciarLecturaPasoActual()
            } else {
                lecturaActiva = false
                binding.btnParlante.icon =
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_audio_off
                    )
                detenerLectura()
            }
        }
    }
    private fun ocultarTodosLosPasos() {

        mostrandoTodosLosPasos = false

        binding.btnVerTodo.text = "Ver todo"

        binding.btnVerTodo.setBackgroundColor(
            ContextCompat.getColor(
                requireContext(),
                android.R.color.white
            )
        )

        binding.btnVerTodo.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.azulgourmeet
            )
        )

        binding.containerTodosLosPasos.visibility = View.GONE

        binding.containerTodosLosPasos.removeAllViews()

    }
    private fun mostrarTodosLosPasos() {

        mostrandoTodosLosPasos = true

        binding.btnVerTodo.text = "Ver menos"

        binding.btnVerTodo.setBackgroundColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.azulgourmeet
            )
        )

        binding.btnVerTodo.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                android.R.color.white
            )
        )

        binding.containerTodosLosPasos.removeAllViews()

        pasos.forEachIndexed { index, paso ->

            val txtPaso = TextView(requireContext())

            txtPaso.text =
                "${index + 1}. ${paso.descripcion}"

            txtPaso.textSize = 16f

            txtPaso.setPadding(
                24,
                20,
                24,
                20
            )

            txtPaso.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    android.R.color.black
                )
            )

            binding.containerTodosLosPasos.addView(txtPaso)

        }

        binding.containerTodosLosPasos.visibility = View.VISIBLE

    }
    private fun cargarDetalle() {

        lifecycleScope.launch {

            try {

                val response = ApiClient.apiService.getDetalleReceta(

                    DetalleRecetaRequest(
                        REC_ID = recetaId
                    )

                )

                if(response.success){

                    val receta = response.receta
                    Glide.with(requireContext())
                        .load(receta.FotoReceta)
                        .placeholder(R.drawable.ic_logo_circular)
                        .error(R.drawable.ic_logo_circular)
                        .into(binding.imgHeaderReceta)
                    binding.txtTitulo.text = receta.REC_NOMBRE
                    binding.tvRecetaDescripcion.text =
                        receta.REC_DESCRIPCION
                    binding.tvDatoGourmet.text =
                        receta.REC_DATOGOUMEET
                    binding.txtTiempo.text =
                        receta.REC_TIEMPO_PREPARACION
                    binding.txtCosto.text = "-"


                    binding.txtNivel.text =
                        receta.Dificultad
                    binding.txtTipo.text =
                        receta.Categoria
                    binding.txtCalificacion.text =
                        String.format("%.1f", response.calificacion.promedio)
                    binding.ratingReceta.rating =
                        response.calificacion.promedio.toFloat()
                    binding.rvIngredientes.layoutManager =
                        androidx.recyclerview.widget.LinearLayoutManager(
                            requireContext(),
                            androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL,
                            false
                        )

                    binding.rvIngredientes.adapter =
                        IngredientesMiniAdapter(

                            response.ingredientes.map {

                                IngredienteReceta(

                                    id = it.ING_ID,

                                    nombre = it.ING_DESCRIPCION,

                                    foto = it.Foto_Ingrediente

                                )

                            }

                        )
                    pasos = response.preparacion

                    pasoActual = 0
                    crearIndicadores()

                    mostrarPaso()


                    val video = response.receta.REC_ENLACEYOUTUBE

                    if (video.isNullOrBlank()) {

                        binding.cardVideo.visibility = View.GONE

                    } else {

                        binding.cardVideo.visibility = View.VISIBLE
                        val idVideo = obtenerYoutubeId(video)

                        if (idVideo != null) {

                            Glide.with(requireContext())
                                .load("https://img.youtube.com/vi/$idVideo/hqdefault.jpg")
                                .into(binding.imgPreviewVideo)
                            binding.cardVideo.setOnClickListener {

                                try {

                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("vnd.youtube:$idVideo")
                                    )

                                    startActivity(intent)

                                } catch (e: Exception) {

                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse(video)
                                    )

                                    startActivity(intent)

                                }

                            }

                        }

                    }

                    // Aquí comenzaremos a llenar el layout
                }

            }
            catch (e: Exception){

                e.printStackTrace()

            }

        }

    }
    private fun inicializarReconocimiento() {
        Log.d("VOZ", "Entró a iniciarEscucha")

        if (!SpeechRecognizer.isRecognitionAvailable(requireContext()))
            return

        speechRecognizer =
            SpeechRecognizer.createSpeechRecognizer(requireContext())

        recognizerIntent = Intent(
            RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        ).apply {

            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )

            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                "es-MX"
            )

            putExtra(
                RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                false
            )
        }
        Log.d("VOZ", "Comenzando reconocimiento")
        speechRecognizer?.setRecognitionListener(
            object : RecognitionListener {

                override fun onResults(results: Bundle?) {

                    escuchando = false

                    val texto =
                        results
                            ?.getStringArrayList(
                                SpeechRecognizer.RESULTS_RECOGNITION
                            )
                            ?.firstOrNull()
                            ?.lowercase()
                            ?: return
                    Log.d("VOZ", "Texto reconocido: " + texto)

                    procesarComando(texto)
                    if (escuchaContinua) {

                        binding.root.postDelayed({

                            iniciarEscucha()

                        },600)

                    }
                }

                override fun onReadyForSpeech(params: Bundle?) {}

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {

                    requireActivity().runOnUiThread {

                        val escala = (1f + (rmsdB / 15f))
                            .coerceIn(1f, 1.35f)

                        binding.btnVoz.scaleX = escala
                        binding.btnVoz.scaleY = escala
                    }

                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {

                    binding.btnVoz.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(150)
                        .start()

                }
                override fun onError(error: Int) {

                    escuchando = false

                    Log.d("VOZ","Error: $error")

                    when(error){

                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> return

                        SpeechRecognizer.ERROR_CLIENT -> return

                    }
                    if(escuchaContinua){
                        binding.root.postDelayed({
                            iniciarEscucha()
                        },800)

                    }

                }

                override fun onPartialResults(partialResults: Bundle?) {}

                override fun onEvent(eventType: Int, params: Bundle?) {}
            }
        )
    }
    private fun inicializarTextToSpeech() {

        textToSpeech = TextToSpeech(requireContext()) { status ->

            if (status == TextToSpeech.SUCCESS) {

                textToSpeech?.language = Locale("es", "MX")
                textToSpeech?.setSpeechRate(0.9f)
            } else {

                Log.e("TTS", "No se pudo inicializar TextToSpeech")

            }

        }

    }
    private fun iniciarLecturaPasoActual() {

        if (pasos.isEmpty()) return

        val texto = pasos[pasoActual].descripcion

        textToSpeech?.speak(
            texto,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "PASO_ACTUAL"
        )

    }
    private fun detenerLectura() {
        textToSpeech?.stop()
    }
    private fun iniciarEscucha() {

        if (escuchando) return

        escuchando = true

        speechRecognizer?.startListening(recognizerIntent)

    }
    private fun detenerEscucha() {

        escuchando = false

        speechRecognizer?.stopListening()

    }
    private fun procesarComando(texto: String) {
        when {
            texto.contains("sig") ||
            texto.contains("siguiente") ||
                    texto.contains("adelante") ||
                    texto.contains("continúa") ||
                    texto.contains("cambiar") ||
                    texto.contains("avanza") ||
                    texto.contains("seguir") ||
                    texto.contains("continuar") -> {
                siguientePaso()
            }
            texto.contains("anterior") ||
                    texto.contains("atrás") ||
                    texto.contains("retrocede") ||
                    texto.contains("regresar") -> {
                pasoAnterior()
            }
            texto.contains("repite") -> {
                mostrarPaso()
            }
            texto.contains("ver todo")|| texto.contains("ver receta") || texto.contains("abrir")  -> {
                mostrarTodosLosPasos()
            }
            texto.contains("ver menos") || texto.contains("cerrar")-> {
                ocultarTodosLosPasos()
            }
            texto.contains("silencio") -> {
                detenerLectura()
            }
            texto.contains("leer") ||
                    texto.contains("léelo") ||
                    texto.contains("lee") ||
                    texto.contains("leer paso") ||
                    texto.contains("reproducir") -> {

                lecturaActiva = true

                binding.btnParlante.icon =
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_audio_on
                    )

                iniciarLecturaPasoActual()
            }

        }
    }
    private fun siguientePaso() {
        if (pasoActual < pasos.lastIndex) {
            pasoActual++
            mostrarPaso()
        }
    }
    private fun pasoAnterior() {
        if (pasoActual > 0) {
            pasoActual--
            mostrarPaso()
        }
    }
    private fun mostrarPaso() {
        iniciarLecturaPasoActual()
        if (pasos.isEmpty()) return
        val paso = pasos[pasoActual]
        binding.tvDescripcionPaso.text =
            paso.descripcion
        actualizarIndicadores()
        actualizarBotonRecetaTerminada()
    }
    private fun crearIndicadores() {

        binding.containerIndicadores.removeAllViews()

        for (i in pasos.indices) {

            //=========================
            // CÍRCULO
            //=========================

            val circulo = ImageView(requireContext())

            val paramsCirculo = LinearLayout.LayoutParams(
                36.dp,
                36.dp
            )

            circulo.layoutParams = paramsCirculo
            circulo.tag = "circulo_$i"

            binding.containerIndicadores.addView(circulo)

            //=========================
            // LÍNEA
            //=========================

            if (i < pasos.lastIndex) {

                val linea = View(requireContext())

                val paramsLinea = LinearLayout.LayoutParams(
                    50.dp,
                    4.dp
                )

                paramsLinea.marginStart = 8.dp
                paramsLinea.marginEnd = 8.dp
                paramsLinea.gravity = Gravity.CENTER_VERTICAL

                linea.layoutParams = paramsLinea
                linea.tag = "linea_$i"

                binding.containerIndicadores.addView(linea)
            }
        }

        actualizarIndicadores()
    }
    private fun actualizarIndicadores() {

        for (i in 0 until pasos.size) {

            //-------------------------
            // CÍRCULO
            //-------------------------

            val circulo =
                binding.containerIndicadores
                    .findViewWithTag<ImageView>("circulo_$i")

            if (i <= pasoActual) {

                circulo.setImageResource(R.drawable.ic_check_white)

                circulo.setBackgroundResource(
                    R.drawable.bg_indicador_completado
                )

            } else {

                circulo.setImageDrawable(null)

                circulo.setBackgroundResource(
                    R.drawable.bg_indicador_pendiente
                )

            }
            //-------------------------
            // LÍNEA
            //-------------------------
            if (i < pasos.lastIndex) {

                val linea =
                    binding.containerIndicadores
                        .findViewWithTag<View>("linea_$i")

                if (i < pasoActual) {

                    linea.setBackgroundColor(
                        0xFF4CAF50.toInt()
                    )

                } else {

                    linea.setBackgroundColor(
                        0xFFDDDDDD.toInt()
                    )

                }

            }

        }

    }
    private fun obtenerYoutubeId(url: String): String? {
        val regex =
            "(?:youtu\\.be/|youtube\\.com.*(?:\\?|&)v=)([^&]+)"
                .toRegex()
        return regex.find(url)?.groupValues?.get(1)
    }
    private fun cargarFotoUsuario() {

        val foto = SesionUsuario.obtenerFoto(requireContext())

        if (!foto.isNullOrEmpty()) {

            Glide.with(this)
                .load(foto)
                .placeholder(R.drawable.ic_user)
                .error(R.drawable.ic_user)
                .circleCrop()
                .into(binding.imgUsuarioComentario)
        } else {
            binding.imgUsuarioComentario.setImageResource(R.drawable.ic_user)
        }
    }
    private fun enviarComentario() {
        if (binding.editComentario.text.toString().trim().isEmpty()) {
            binding.editComentario.error = "Escribe un comentario"
            return
        }

        if (binding.ratingComentario.rating == 0f) {
            Toast.makeText(
                requireContext(),
                "Selecciona una calificación.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        lifecycleScope.launch {
            try {
                val response =
                    ApiClient.apiService.comentarCalificarReceta(
                        ComentarCalificarRequest(
                            CLI_ID = SesionUsuario.obtenerId(requireContext()),
                            REC_ID = recetaId,
                            COMENTARIO = binding.editComentario.text
                                .toString()
                                .trim(),
                            CALIFICACION = binding.ratingComentario.rating
                                .toDouble()
                        )
                    )
                if (response.success) {
                    Toast.makeText(
                        requireContext(),
                        response.mensaje,
                        Toast.LENGTH_SHORT
                    ).show()
                    // Limpiar comentario
                    binding.editComentario.text?.clear()
                    // Reiniciar calificación
                    binding.ratingComentario.rating = 0f
                    // Deshabilitar nuevamente el botón
                    validarComentario()
                } else {
                    Toast.makeText(
                        requireContext(),
                        response.mensaje,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    requireContext(),
                    "Error al enviar el comentario.",
                    Toast.LENGTH_SHORT
                ).show()

            }

        }

    }
    private fun actualizarBotonRecetaTerminada() {

        if (recetaTerminada) {

            binding.btnRecetaTerminada.apply {

                text = "✓ Receta completada"

                isEnabled = false

                alpha = 1f

            }

            return

        }

        val habilitado = pasoActual == pasos.lastIndex

        binding.btnRecetaTerminada.isEnabled = habilitado

        binding.btnRecetaTerminada.alpha =
            if (habilitado) 1f else 0.5f

    }
    private fun cargarMiComentario() {

        lifecycleScope.launch {

            try {

                val response =
                    ApiClient.apiService.obtenerMiComentario(

                        ObtenerMiComentarioRequest(

                            CLI_ID = SesionUsuario.obtenerId(requireContext()),

                            REC_ID = recetaId

                        )

                    )

                if (response.success && response.comentarioExiste) {

                    binding.editComentario.setText(
                        response.comentario
                    )

                    binding.ratingComentario.rating =
                        response.calificacion?.toFloat() ?: 0f

                    binding.btnComentar.text =
                        "Actualizar"

                } else {

                    binding.editComentario.text?.clear()

                    binding.ratingComentario.rating = 0f

                    binding.btnComentar.text =
                        "Comentar"

                }

                validarComentario()

            } catch (e: Exception) {

                e.printStackTrace()

            }

        }

    }
    private fun marcarRecetaTerminada() {

        val cliId = SesionUsuario.obtenerId(requireContext())

        if (cliId <= 0) {

            Toast.makeText(
                requireContext(),
                "Debes iniciar sesión.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val recId = recetaId

        if (recId <= 0) {

            Toast.makeText(
                requireContext(),
                "Receta no válida.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val request = MarcarRecetaTerminadaRequest(

            CLI_ID = cliId,

            REC_ID = recId

        )

        lifecycleScope.launch {

            try {

                val response =
                    ApiClient.apiService.marcarRecetaTerminada(request)

                if (response.success) {

                    Toast.makeText(
                        requireContext(),
                        response.mensaje,
                        Toast.LENGTH_SHORT
                    ).show()

                    //Actualizar botón
                    binding.btnRecetaTerminada.apply {

                        text = "✓ Receta completada"

                        isEnabled = false

                        alpha = 1f

                        setBackgroundColor(
                            ContextCompat.getColor(
                                requireContext(),
                                android.R.color.holo_green_dark
                            )
                        )
                    }

                    //Actualizar sesión
                    SesionUsuario.actualizarNivel(
                        requireContext(),
                        response.nivelNuevo
                    )

                    SesionUsuario.actualizarPuntos(
                        requireContext(),
                        response.xpActual
                    )

                    //¿Subió de nivel?
                    if (response.subioNivel) {

                        mostrarSubidaNivel(
                            response.nivelNuevo
                        )

                    }

                } else {

                    Toast.makeText(
                        requireContext(),
                        response.mensaje,
                        Toast.LENGTH_LONG
                    ).show()

                }

            } catch (e: Exception) {

                e.printStackTrace()

                Toast.makeText(
                    requireContext(),
                    "Error de conexión.",
                    Toast.LENGTH_SHORT
                ).show()

            }

        }

    }
    private fun mostrarSubidaNivel(nivel: Int) {

        val dialog = Dialog(requireContext())

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        dialog.setContentView(R.layout.dialog_subida_nivel)

        dialog.setCancelable(false)

        val imgNivel =
            dialog.findViewById<ImageView>(R.id.imgNivel)

        val btnContinuar =
            dialog.findViewById<Button>(R.id.btnContinuar)

        when (nivel) {
            2 -> imgNivel.setImageResource(R.drawable.ic_subir_nivel)
            3 -> imgNivel.setImageResource(R.drawable.ic_subir_nivel)
            4 -> imgNivel.setImageResource(R.drawable.ic_subir_nivel)
            5 -> imgNivel.setImageResource(R.drawable.ic_subir_nivel)

        }

        btnContinuar.setOnClickListener {

            dialog.dismiss()

        }

        dialog.show()

        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

    }
    private fun verificarRecetaTerminada() {

        val cliId = SesionUsuario.obtenerId(requireContext())

        if (cliId <= 0) return

        val request = VerificarRecetaTerminadaRequest(

            CLI_ID = cliId,

            REC_ID = recetaId

        )

        lifecycleScope.launch {

            try {

                val response =
                    ApiClient.apiService.verificarRecetaTerminada(request)

                if (response.success) {

                    recetaTerminada = response.recetaTerminada

                    actualizarBotonRecetaTerminada()

                }

            } catch (e: Exception) {

                e.printStackTrace()

            }

        }

    }
    private fun validarComentario() {
        val comentario = binding.editComentario.text.toString().trim()
        val calificacion = binding.ratingComentario.rating
        val habilitar =
            comentario.isNotEmpty() &&
                    calificacion > 0f
        binding.btnComentar.isEnabled = habilitar
        binding.btnComentar.alpha =
            if (habilitar) 1f else 0.5f
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}