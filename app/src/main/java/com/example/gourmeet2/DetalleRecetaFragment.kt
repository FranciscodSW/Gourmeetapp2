package com.example.gourmeet2

import android.text.TextWatcher
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.app.Dialog
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
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.text.Editable
import android.util.Log
import android.view.Gravity
import android.view.Window
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContentProviderCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gourmeet2.adapters.ComentarioAdapter
import com.example.gourmeet2.adapters.OnComentarioClickListener
import com.example.gourmeet2.data.models.*
import com.example.gourmeet2.databinding.DialogReportarComentarioBinding
import com.example.gourmeet2.databinding.DialogReportarRecetaBinding
import com.example.gourmeet2.utils.SesionUsuario
import com.example.gourmeet2.utils.SesionUsuario.obtenerId
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.compareTo

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
    private var animatorSet: AnimatorSet? = null
    private var comentariosVisibles = false
    private val administradorComandosVoz = AdministradorComandosVoz()
    private lateinit var comentarioAdapter: ComentarioAdapter
    private var comentarioSeleccionado: Comentarios? = null
    private var respuestaSeleccionada: RespuestaComentario? = null
    private var modoRespuesta = "CREAR"

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
        verificarFavorito()
        inicializarTextToSpeech()
        inicializarReconocimiento()
        inicializarRecyclerComentarios()
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

            lecturaActiva = !lecturaActiva

            if (lecturaActiva) {

                binding.btnParlante.icon =
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_audio_on
                    )
                iniciarLecturaPasoActual()
            } else {
                binding.btnParlante.icon =
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_audio_off
                    )
                detenerLectura()
            }
        }
        binding.btnVerComentarios.setOnClickListener {

            comentariosVisibles = !comentariosVisibles

            if (comentariosVisibles) {

                binding.rvComentarios.visibility = View.VISIBLE

                binding.btnVerComentarios.text = "Ocultar comentarios"

                cargarComentarios()

            } else {

                binding.rvComentarios.visibility = View.GONE

                binding.btnVerComentarios.text = "Comentarios anteriores"

            }

        }
        binding.btnReportarProblema.setOnClickListener {
            mostrarDialogoReporte()
        }
        binding.rvComentarios.layoutManager = LinearLayoutManager(requireContext())
        binding.rvComentarios.adapter = comentarioAdapter
        binding.layoutFavoritos.setOnClickListener { cambiarFavorito()}
        binding.layoutColeccion.setOnClickListener {

            if (binding.scrollColecciones.visibility == View.VISIBLE) {

                // =========================
                // CERRAR
                // =========================

                binding.imagenguardar.setImageResource(
                    R.drawable.ic_guardar
                )

                ocultarColeccionesAnimadas {

                    // Ya desaparecieron todas las colecciones

                    binding.rectanguloColeccion
                        .animate()
                        .scaleX(0f)
                        .setDuration(300)
                        .withEndAction {

                            binding.rectanguloColeccion.visibility =
                                View.GONE

                            binding.scrollColecciones.visibility =
                                View.GONE

                            // Guardar vuelve a normal
                            binding.layoutColeccion
                                .animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(150)
                                .start()

                            // Mostrar botones
                            binding.layoutPlaneador.visibility =
                                View.VISIBLE

                            binding.layoutCompartir.visibility =
                                View.VISIBLE

                            binding.layoutFavoritos.visibility =
                                View.VISIBLE

                            binding.texguardar.visibility =
                                View.VISIBLE
                        }
                        .start()
                }
            }else {

                // =========================
                // ABRIR
                // =========================

                // Ocultar los otros botones ANTES
                binding.layoutPlaneador.visibility = View.GONE
                binding.layoutCompartir.visibility = View.GONE
                binding.layoutFavoritos.visibility = View.GONE
                binding.texguardar.visibility = View.GONE

                // Mostrar rectángulo
                binding.rectanguloColeccion.visibility = View.VISIBLE

                // Reiniciar posición de animación
                binding.rectanguloColeccion.scaleX = 0f

                // Expandir rectángulo
                binding.rectanguloColeccion
                    .animate()
                    .scaleX(1f)
                    .setDuration(300)
                    .start()

                // Comprimir Guardar
                binding.layoutColeccion
                    .animate()
                    .scaleX(0.75f)
                    .scaleY(0.85f)
                    .setDuration(300)
                    .start()

                // Cambiar icono
                binding.imagenguardar.setImageResource(
                    R.drawable.ic_guardar_on
                )

                // Mostrar colecciones
                mostrarColecciones()

                binding.scrollColecciones.visibility = View.VISIBLE
            }
        }



    }
    private fun cargarComentarios() {

        val usuarioId = obtenerId(requireContext())

        lifecycleScope.launch {
            val response = ApiClient.apiService.listarComentarios(
                ListarComentariosRequest(
                    receta = recetaId,
                    usuario = usuarioId
                )
            )
            if (response.success) {
                comentarioAdapter.actualizarLista(response.comentarios)
            }
        }
    }

    private fun responderComentario(
        comentarioPadre: Int,
        texto: String
    ) {

        viewLifecycleOwner.lifecycleScope.launch {

            try {
                val request = RespuestaComentarioRequest(
                    accion = "CREAR",
                    receta = recetaId,   // <-- tu id de la receta
                    usuario = obtenerId(requireContext()),
                    comentarioPadre = comentarioPadre,
                    comentario = texto
                )
                val response = ApiClient.apiService.responderComentario(request)
                if (response.success) {
                    comentarioAdapter.cerrarCajaRespuesta()
                    Toast.makeText(
                        requireContext(),
                        response.message,
                        Toast.LENGTH_SHORT
                    ).show()
                    cargarComentarios()
                } else {
                    Toast.makeText(
                        requireContext(),
                        response.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    e.message,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    private fun editarRespuesta(
        respuestaId: Int,
        texto: String
    ) {

        viewLifecycleOwner.lifecycleScope.launch {

            try {

                val request = RespuestaComentarioRequest(

                    accion = "EDITAR",

                    usuario = obtenerId(requireContext()),

                    respuesta = respuestaId,

                    comentario = texto

                )

                val response = ApiClient.apiService.responderComentario(request)

                if (response.success) {

                    comentarioAdapter.cerrarCajaRespuesta()

                    Toast.makeText(
                        requireContext(),
                        response.message,
                        Toast.LENGTH_SHORT
                    ).show()

                    cargarComentarios()

                } else {

                    Toast.makeText(
                        requireContext(),
                        response.message,
                        Toast.LENGTH_SHORT
                    ).show()

                }

            } catch (e: Exception) {

                Toast.makeText(
                    requireContext(),
                    e.message,
                    Toast.LENGTH_SHORT
                ).show()

            }

        }

    }
    private fun inicializarRecyclerComentarios() {

        comentarioAdapter = ComentarioAdapter(

            mutableListOf(),

            object : OnComentarioClickListener {
                override fun onActualizarRespuesta(
                    respuesta: RespuestaComentario,
                    nuevoTexto: String
                ) {

                    editarRespuesta(
                        respuesta.id,
                        nuevoTexto
                    )

                }

                override fun onLike(comentario: Comentarios) {

                    reaccionarComentario(
                        comentario.id,
                        "LIKE"
                    )

                }

                override fun onDislike(comentario: Comentarios) {
                    reaccionarComentario(
                        comentario.id,
                        "DISLIKE"
                    )
                }

                override fun onReportar(comentario: Comentarios) {
                    mostrarDialogoReportar(comentario)

                }

                override fun onResponder(comentario: Comentarios) {

                    comentarioSeleccionado = comentario

                    val miRespuesta = comentario.respuestas.firstOrNull {
                        it.esMia
                    }

                    respuestaSeleccionada = miRespuesta

                    if (miRespuesta != null) {

                        modoRespuesta = "EDITAR"

                        comentarioAdapter.mostrarRespuestaExistente(
                            comentario.id,
                            miRespuesta
                        )

                    } else {

                        modoRespuesta = "CREAR"

                        comentarioAdapter.mostrarCajaRespuesta(
                            comentario.id
                        )

                    }

                }
                override fun onEnviarRespuesta(
                    comentario: Comentarios,
                    respuesta: String
                ) {

                    comentarioSeleccionado = comentario

                    if (modoRespuesta == "CREAR") {

                        responderComentario(
                            comentario.id,
                            respuesta
                        )

                    } else {

                        respuestaSeleccionada?.let {

                            editarRespuesta(
                                it.id,
                                respuesta
                            )

                        }

                    }

                }
                override fun onEliminarRespuesta(
                    respuesta: RespuestaComentario
                ) {
                    eliminarRespuesta(respuesta.id)
                }

                override fun onEditarComentario(
                    comentario: Comentarios
                ) {

                    editarComentario(comentario)

                }

                override fun onEliminarComentario(
                    comentario: Comentarios
                ) {

                    confirmarEliminarComentario(comentario)

                }
                override fun onActualizarComentario(
                    comentario: Comentarios,
                    nuevoComentario: String,
                    nuevaCalificacion: Float
                ) {

                    comentarioSeleccionado = comentario

                    guardarComentario(
                        comentario = nuevoComentario,
                        calificacion = nuevaCalificacion.toDouble()
                    )

                }

                override fun onLikeRespuesta(
                    respuesta: RespuestaComentario
                ) {

                    reaccionarRespuesta(
                        respuesta.id,
                        "LIKE"
                    )

                }

                override fun onDislikeRespuesta(
                    respuesta: RespuestaComentario
                ) {

                    reaccionarRespuesta(
                        respuesta.id,
                        "DISLIKE"
                    )

                }

                override fun onReportarRespuesta(
                    respuesta: RespuestaComentario
                ) {

                    reaccionarRespuesta(
                        respuesta.id,
                        "REPORTAR"
                    )

                }

                override fun onEditarRespuesta(
                    respuesta: RespuestaComentario
                ) = Unit
            }
        )
        binding.rvComentarios.layoutManager =
            LinearLayoutManager(requireContext())
        binding.rvComentarios.adapter =
            comentarioAdapter

    }
    private fun reaccionarRespuesta(
        respuestaId: Int,
        tipo: String
    ) {
        val usuarioId = obtenerId(requireContext())

        lifecycleScope.launch {

            try {

                val response = ApiClient.apiService.reaccionarRespuesta(

                    ReaccionRespuestaRequest(

                        respuesta = respuestaId,

                        usuario = usuarioId,

                        tipo = tipo

                    )

                )

                if (response.success) {

                    comentarioAdapter.actualizarReaccionRespuesta(

                        respuestaId = response.respuesta,

                        likes = response.likes,

                        dislikes = response.dislikes,

                        reportes = response.reportes,

                        miReaccion = response.miReaccion

                    )

                } else {

                    Toast.makeText(
                        requireContext(),
                        response.message ?: "No fue posible reaccionar.",
                        Toast.LENGTH_SHORT
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
    private fun mostrarDialogoReportar(
        comentario: Comentarios
    ) {
        val dialog = Dialog(requireContext())
        val binding = DialogReportarComentarioBinding.inflate(layoutInflater)
        dialog.setContentView(binding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        binding.imgReporte.setImageResource(R.drawable.ic_reportar_comentario)
        binding.btnContinuar.setOnClickListener {
            dialog.dismiss()
            reaccionarComentario(
                comentario.id,
                "REPORTAR"
            )
        }
        dialog.show()
    }

    private fun confirmarEliminarComentario(
        comentario: Comentarios
    ) {

        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar comentario")
            .setMessage("¿Deseas eliminar tu comentario?")
            .setPositiveButton("Eliminar") { _, _ ->

                eliminarComentario(comentario)

            }
            .setNegativeButton("Cancelar", null)
            .show()

    }
    private fun eliminarComentario(
        comentario: Comentarios
    ) {

        viewLifecycleOwner.lifecycleScope.launch {

            try {

                val response =
                    ApiClient.apiService.eliminarComentario(

                        EliminarComentarioRequest(

                            CLI_ID = obtenerId(requireContext()),

                            REC_ID = recetaId

                        )

                    )

                if (response.success) {

                    Toast.makeText(
                        requireContext(),
                        response.mensaje,
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.editComentario.text?.clear()
                    binding.ratingComentario.rating = 0f
                    binding.btnComentar.text = "Comentar"

                    comentarioSeleccionado = null

                    validarComentario()

                    cargarComentarios()

                    cargarMiComentario()

                } else {

                    Toast.makeText(
                        requireContext(),
                        response.mensaje,
                        Toast.LENGTH_SHORT
                    ).show()

                }

            } catch (e: Exception) {

                Toast.makeText(
                    requireContext(),
                    e.message,
                    Toast.LENGTH_SHORT
                ).show()

            }

        }

    }
    private fun editarComentario(
        comentario: Comentarios
    ) {
        comentarioSeleccionado = comentario
        comentarioAdapter.editarComentario(
            comentario.id
        )
    }
    private fun reaccionarComentario(comentarioId: Int, tipo: String) {
        val usuarioId = obtenerId(requireContext())
        if (usuarioId == 0) {
            Toast.makeText(
                requireContext(),
                "Debes iniciar sesión",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {

            try {
                val response = ApiClient.apiService.reaccionarComentario(

                    ReaccionComentarioRequest(

                        comentario = comentarioId,

                        usuario = usuarioId,

                        tipo = tipo

                    )

                )

                if (response.success) {

                    comentarioAdapter.actualizarReaccion(

                        comentarioId = response.comentario,

                        likes = response.likes,

                        dislikes = response.dislikes,

                        reportes = response.reportes,

                        miReaccion = response.miReaccion

                    )

                } else {

                    Toast.makeText(
                        requireContext(),
                        "No se pudo registrar la reacción",
                        Toast.LENGTH_SHORT
                    ).show()

                }

            } catch (e: Exception) {

                e.printStackTrace()

                Toast.makeText(
                    requireContext(),
                    "Error de conexión",
                    Toast.LENGTH_SHORT
                ).show()

            }

        }

    }
    private fun eliminarRespuesta(respuestaId: Int) {

        viewLifecycleOwner.lifecycleScope.launch {

            try {

                val request = RespuestaComentarioRequest(

                    accion = "ELIMINAR",

                    usuario = obtenerId(requireContext()),

                    respuesta = respuestaId

                )

                val response = ApiClient.apiService.responderComentario(request)

                if (response.success) {

                    comentarioAdapter.cerrarCajaRespuesta()

                    Toast.makeText(
                        requireContext(),
                        response.message,
                        Toast.LENGTH_SHORT
                    ).show()

                    cargarComentarios()

                } else {

                    Toast.makeText(
                        requireContext(),
                        response.message,
                        Toast.LENGTH_SHORT
                    ).show()

                }

            } catch (e: Exception) {

                Toast.makeText(
                    requireContext(),
                    e.message,
                    Toast.LENGTH_SHORT
                ).show()

            }

        }

    }
    private fun iniciarAnimacionMicrofono() {
        animatorSet?.cancel()
        fun crearOnda(view: View, delay: Long): AnimatorSet {
            val escalaX = ObjectAnimator.ofFloat(view, View.SCALE_X, 1f, 2.6f)
            val escalaY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f, 2.6f)
            val alpha = ObjectAnimator.ofFloat(view, View.ALPHA, 0.45f, 0f)
            return AnimatorSet().apply {
                playTogether(escalaX, escalaY, alpha)
                duration = 1200
                startDelay = delay
                interpolator = AccelerateDecelerateInterpolator()
            }
        }
        animatorSet = AnimatorSet().apply {
            playTogether(
                crearOnda(binding.onda1,0),
                crearOnda(binding.onda2,350),
                crearOnda(binding.onda3,700)
            )
            addListener(object: AnimatorListenerAdapter(){
                override fun onAnimationEnd(animation: Animator) {
                    if (escuchando) {
                        iniciarAnimacionMicrofono()
                    }
                }
            })
        }
        animatorSet?.start()
    }
    private fun detenerAnimacionMicrofono(){
        animatorSet?.cancel()
        listOf(binding.onda1,binding.onda2,binding.onda3).forEach{
            it.alpha = 0f
            it.scaleX = 1f
            it.scaleY = 1f
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
                }
            }
            catch (e: Exception){

                e.printStackTrace()

            }

        }

    }
    private fun ejecutarComandoVoz(comando: ComandoVoz) {
        when (comando) {
            ComandoVoz.SIGUIENTE -> {
                Log.d("VOZ", "Comando: SIGUIENTE")
                siguientePaso()
            }
            ComandoVoz.ANTERIOR -> {
                Log.d("VOZ", "Comando: ANTERIOR")
                pasoAnterior()
            }
            ComandoVoz.REPETIR -> {
                Log.d("VOZ", "Comando: REPETIR")
                iniciarLecturaPasoActual()
            }
            ComandoVoz.DETENER -> {
                Log.d("VOZ", "Comando: DETENER")
                detenerLectura()
            }
            ComandoVoz.CONTINUAR -> {
                Log.d("VOZ", "Comando: CONTINUAR")
                iniciarLecturaPasoActual()
            }
            ComandoVoz.VER_TODO -> {
                Log.d("VOZ", "Comando: VER TODO")
                mostrarTodosLosPasos()
            }
            ComandoVoz.OCULTAR -> {
                Log.d("VOZ", "Comando: OCULTAR")
                ocultarTodosLosPasos()
            }
            ComandoVoz.DESCONOCIDO -> {
                Log.d("VOZ", "No se reconoció ningún comando")

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
                    val texto = results
                        ?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )
                        ?.firstOrNull()
                        ?: return
                    Log.d("VOZ", "Texto reconocido: $texto")
                    // Analizar el texto reconocido
                    val comando = administradorComandosVoz.analizarTexto(texto)
                    // Ejecutar el comando encontrado
                    ejecutarComandoVoz(comando)
                    if (escuchaContinua) {
                        binding.root.postDelayed({
                            iniciarEscucha()
                        }, 600)
                    }
                }
                override fun onReadyForSpeech(params: Bundle?) {
                    escuchando = true
                    iniciarAnimacionMicrofono()
                }
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
                    escuchando = false
                    detenerAnimacionMicrofono()
                }
                override fun onError(error: Int) {
                    escuchando = false
                    detenerAnimacionMicrofono()
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

                val voz = textToSpeech?.voices?.firstOrNull {
                    it.name == "es-es-x-eec-network"
                }

                if (voz != null) {

                    Log.d("TTS", "Usando voz: ${voz.name}")

                    textToSpeech?.voice = voz

                } else {

                    Log.d("TTS", "No se encontró la voz.")

                    textToSpeech?.language = Locale("es", "MX")

                }

                textToSpeech?.setSpeechRate(0.9f)

                textToSpeech?.setPitch(1.0f)

                textToSpeech?.setOnUtteranceProgressListener(
                    object : UtteranceProgressListener() {

                        override fun onStart(utteranceId: String?) {
                            Log.d("TTS", "Comenzó la lectura")
                        }

                        override fun onDone(utteranceId: String?) {
                            Log.d("TTS", "Terminó la lectura")
                        }

                        override fun onError(utteranceId: String?) {
                            Log.d("TTS", "Error al leer")
                        }
                    }
                )

            } else {

                Log.e("TTS", "No se pudo inicializar TextToSpeech")

            }
        }
    }
    private fun iniciarLecturaPasoActual() {
        if (!lecturaActiva) {
            return
        }
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
        val cantidad = pasos.size
        val tamañoCirculo: Int
        val tamañoLinea: Int
        val margen: Int
        when {
            cantidad <= 4 -> {
                tamañoCirculo = 33.dp
                tamañoLinea = 45.dp
                margen = 8.dp
            }
            cantidad <= 6 -> {
                tamañoCirculo = 24.dp
                tamañoLinea = 27.dp
                margen = 4.dp
            }
            cantidad <= 8 -> {
                tamañoCirculo = 21.dp
                tamañoLinea = 24.dp
                margen = 2.dp
            }
            else -> {
                tamañoCirculo = 15.dp
                tamañoLinea = 12.dp
                margen = 1.dp
            }
        }
        for (i in pasos.indices) {
            val circulo = ImageView(requireContext())
            circulo.layoutParams = LinearLayout.LayoutParams(
                tamañoCirculo,
                tamañoCirculo
            )
            circulo.tag = "circulo_$i"
            circulo.setOnClickListener {
                pasoActual = i
                mostrarPaso()
                actualizarIndicadores()
                if (lecturaActiva) {
                    iniciarLecturaPasoActual()
                }
            }
            binding.containerIndicadores.addView(circulo)
            if (i < pasos.lastIndex) {
                val linea = View(requireContext())
                val paramsLinea = LinearLayout.LayoutParams(
                    tamañoLinea,
                    4.dp
                )
                paramsLinea.marginStart = margen
                paramsLinea.marginEnd = margen
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

        val comentario = binding.editComentario.text
            .toString()
            .trim()

        val calificacion = binding.ratingComentario.rating

        if (comentario.isEmpty()) {
            binding.editComentario.error = "Escribe un comentario"
            return
        }

        if (calificacion == 0f) {
            Toast.makeText(
                requireContext(),
                "Selecciona una calificación.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        guardarComentario(
            comentario = comentario,
            calificacion = calificacion.toDouble()
        )

    }
    private fun guardarComentario(comentario: String, calificacion: Double) {

        lifecycleScope.launch {

            try {

                val response =
                    ApiClient.apiService.comentarCalificarReceta(
                        ComentarCalificarRequest(
                            CLI_ID = obtenerId(requireContext()),
                            REC_ID = recetaId,
                            COMENTARIO = comentario,
                            CALIFICACION = calificacion
                        )
                    )

                if (response.success) {

                    comentarioAdapter.cancelarEdicionComentario()

                    comentarioSeleccionado = null

                    Toast.makeText(
                        requireContext(),
                        response.mensaje,
                        Toast.LENGTH_SHORT
                    ).show()

                    cargarComentarios()

                    cargarMiComentario()

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
    private fun mostrarDialogoReporte() {
        val bindingDialog = DialogReportarRecetaBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(bindingDialog.root)
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.item_reporte_motivo,
            motivosReporte
        )
        bindingDialog.spMotivo.setAdapter(adapter)
        bindingDialog.btnCancelar.setOnClickListener {
            dialog.dismiss()
        }
        bindingDialog.edtDescripcion.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }
            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                bindingDialog.txtContador.text = "${s?.length ?: 0} / 300"
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        bindingDialog.btnEnviarReporte.setOnClickListener {

            val motivo = bindingDialog.spMotivo.text.toString().trim()

            val descripcion = bindingDialog.edtDescripcion.text.toString().trim()

            if (motivo.isEmpty()) {
                bindingDialog.layoutMotivo.error = "Seleccione un motivo"
                return@setOnClickListener
            } else {
                bindingDialog.layoutMotivo.error = null
            }
            if (descripcion.isEmpty()) {
                bindingDialog.layoutDescripcion.error = "Describe el problema"
                return@setOnClickListener
            } else {
                bindingDialog.layoutDescripcion.error = null
            }
            val codigoProblema = obtenerCodigoProblema(motivo)
            reportarReceta(
                codigoProblema,
                descripcion,
                dialog
            )
        }
        dialog.show()
    }
    private fun reportarReceta(problema: String,descripcion: String, dialog: BottomSheetDialog) {

        lifecycleScope.launch {

            try {

                val request = ReportarRecetaRequest(
                    cliId = obtenerId(requireContext()),
                    recId = recetaId,
                    problema = problema,
                    descripcion = descripcion
                )
                val response =
                    ApiClient.apiService.reportarReceta(request)

                if (response.success) {
                    Toast.makeText(
                        requireContext(),
                        response.mensaje,
                        Toast.LENGTH_SHORT
                    ).show()
                    dialog.dismiss()
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
                    "Error de conexión.",
                    Toast.LENGTH_SHORT
                ).show()

            }

        }

    }
    private fun obtenerCodigoProblema(motivo: String): String {

        return when (motivo) {

            "Información incorrecta o engañosa" -> "INFORMACION_INCORRECTA"

            "Ingredientes erróneos o incompletos" -> "INGREDIENTES_ERRONEOS"

            "Pasos incompletos o desordenados" -> "PASOS_INCOMPLETOS"

            "Faltas ortográficas" -> "FALTAS_ORTOGRAFICAS"

            "Contenido inapropiado" -> "CONTENIDO_INAPROPIADO"

            "Fotografías engañosas" -> "FOTOGRAFIAS_ENGANOSAS"

            "Receta duplicada" -> "RECETA_DUPLICADA"

            "Sabor desagradable" -> "SABOR_DESAGRADABLE"

            "Mala redacción de la receta" -> "MALA_REDACCION"

            else -> ""
        }

    }
    private val motivosReporte = listOf(

        "Información incorrecta o engañosa",

        "Ingredientes erróneos o incompletos",

        "Pasos incompletos o desordenados",

        "Faltas ortográficas",

        "Contenido inapropiado",

        "Fotografías engañosas",

        "Receta duplicada",

        "Sabor desagradable",

        "Mala redacción de la receta"

    )
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
                            CLI_ID = obtenerId(requireContext()),
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
                    binding.btnComentar.text = "Comentar"
                }
                validarComentario()
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }

    }
    private fun marcarRecetaTerminada() {
        val cliId = obtenerId(requireContext())
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

        val cliId = obtenerId(requireContext())

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
    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()
    private fun cambiarFavorito() {

        val cliId =
            obtenerId(requireContext())

        if (cliId <= 0) {

            Toast.makeText(
                requireContext(),
                "No se encontró el usuario",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (recetaId <= 0) {

            Toast.makeText(
                requireContext(),
                "Receta inválida",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        lifecycleScope.launch {

            try {

                val request =
                    GestionarFavoritoRequest(

                        CLI_ID = cliId,

                        REC_ID = recetaId

                    )

                val response =
                    ApiClient.apiService
                        .gestionarFavorito(request)


                if (response.success) {

                    // ==========================================
                    // ACTUALIZAR ICONO
                    // ==========================================

                    if (response.guardada) {

                        binding.imgFavorito.setImageResource(
                            R.drawable.ic_favorito_on
                        )

                    } else {

                        binding.imgFavorito.setImageResource(
                            R.drawable.ic_favorito
                        )
                    }


                    Toast.makeText(
                        requireContext(),
                        response.message,
                        Toast.LENGTH_SHORT
                    ).show()

                } else {

                    Toast.makeText(
                        requireContext(),
                        response.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {

                e.printStackTrace()

                Toast.makeText(
                    requireContext(),
                    "Error al actualizar favoritos",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    private fun verificarFavorito() {

        val cliId =
            obtenerId(
                requireContext()
            )

        if (cliId <= 0 || recetaId <= 0) {
            return
        }

        lifecycleScope.launch {

            try {

                Log.d(
                    "FAVORITO",
                    "Verificando favorito - CLI_ID: $cliId REC_ID: $recetaId"
                )

                val response =
                    ApiClient.apiService.verificarFavorito(

                        VerificarFavoritoRequest(

                            CLI_ID = cliId,

                            REC_ID = recetaId

                        )

                    )

                if (!response.success) {

                    Log.d(
                        "FAVORITO",
                        "No se pudo verificar: ${response.message}"
                    )

                    return@launch
                }

                if (response.guardada) {

                    binding.imgFavorito.setImageResource(
                        R.drawable.ic_favorito_on
                    )

                    Log.d(
                        "FAVORITO",
                        "Receta guardada en favoritos"
                    )

                } else {

                    binding.imgFavorito.setImageResource(
                        R.drawable.ic_favorito
                    )

                    Log.d(
                        "FAVORITO",
                        "Receta no está guardada en favoritos"
                    )
                }

            } catch (e: Exception) {

                e.printStackTrace()

                Log.e(
                    "FAVORITO",
                    "Error verificando favorito",
                    e
                )
            }
        }
    }
    private fun mostrarColecciones() {

        binding.contenedorColecciones.removeAllViews()

        lifecycleScope.launch {

            try {

                val cliId = obtenerId(requireContext())

                if (cliId <= 0) {
                    return@launch
                }

                val response =
                    ApiClient.apiService.listarColeccionesConRecetas(
                        ListarColeccionesRecetasRequest(cliId)
                    )

                if (!response.success) {
                    return@launch
                }

                response.colecciones.forEachIndexed { index, coleccion ->

                    // Crear el elemento pero inicialmente invisible
                    val item = crearColeccionVisual(coleccion)

                    item.alpha = 0f
                    item.scaleX = 0.7f
                    item.scaleY = 0.7f

                    binding.contenedorColecciones.addView(item)

                    // Tiempo entre cada colección
                    delay(index * 80L)

                    // Aparece
                    item.animate()
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(180)
                        .start()
                }

            } catch (e: Exception) {

                e.printStackTrace()

                Toast.makeText(
                    requireContext(),
                    "No fue posible cargar las colecciones",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    private fun crearColeccionVisual(
        coleccion: ColeccionConRecetas
    ): View {

        val item =
            layoutInflater.inflate(
                R.layout.item_seleccionado_con_circulo_azul,
                binding.contenedorColecciones,
                false
            )

        val img =
            item.findViewById<ImageView>(
                R.id.img
            )

        val txt =
            item.findViewById<TextView>(
                R.id.txt
            )

        val circuloAzul =
            item.findViewById<View>(
                R.id.circuloAzul
            )

        txt.text = coleccion.COL_NOMBRE

        // ==========================================
        // ICONO
        // ==========================================

        val icono =
            when (coleccion.COL_PORTADA) {

                "Favoritos" ->
                    R.drawable.ic_favoritos

                "Desayuno" ->
                    R.drawable.ic_desayuno

                "Pasta" ->
                    R.drawable.ic_pasta

                "Guardar" ->
                    R.drawable.ic_guardar

                else ->
                    R.drawable.ic_nuevo_icono
            }

        img.setImageResource(icono)

        // ==========================================
        // ¿YA ESTÁ GUARDADA ESTA RECETA?
        // ==========================================

        val estaGuardada =
            coleccion.RECETAS.any {
                it.REC_ID == recetaId
            }

        if (estaGuardada) {

            circuloAzul.visibility =
                View.VISIBLE

        } else {

            circuloAzul.visibility =
                View.GONE
        }

        // ==========================================
        // CLICK
        // ==========================================

        item.setOnClickListener {

            gestionarRecetaEnColeccion(
                coleccion
            )
        }

        return item
    }
    private fun ocultarColeccionesAnimadas(
        onComplete: () -> Unit
    ) {
        val contenedor = binding.contenedorColecciones

        val cantidad = contenedor.childCount

        if (cantidad == 0) {
            onComplete()
            return
        }

        var terminadas = 0

        for (i in cantidad - 1 downTo 0) {

            val item = contenedor.getChildAt(i)

            item.animate()
                .alpha(0f)
                .scaleX(0.7f)
                .scaleY(0.7f)
                .setStartDelay((cantidad - 1 - i) * 60L)
                .setDuration(150)
                .withEndAction {

                    terminadas++

                    if (terminadas == cantidad) {
                        contenedor.removeAllViews()
                        onComplete()
                    }
                }
                .start()
        }
    }
    private fun gestionarRecetaEnColeccion(
        coleccion: ColeccionConRecetas
    ) {

        val cliId =
            obtenerId(requireContext())

        if (cliId <= 0 || recetaId <= 0) {
            return
        }

        val yaEstaGuardada =
            coleccion.RECETAS.any {
                it.REC_ID == recetaId
            }

        lifecycleScope.launch {

            try {

                Log.d(
                    "COLECCION",
                    "Colección: ${coleccion.COL_ID}"
                )

                Log.d(
                    "COLECCION",
                    "Receta: $recetaId"
                )

                Log.d(
                    "COLECCION",
                    "Ya guardada: $yaEstaGuardada"
                )

                val request =
                    GestionarRecetaColeccionRequest(

                        CLI_ID = cliId,

                        COL_ID = coleccion.COL_ID,

                        REC_ID = recetaId,

                        ACCION =
                            if (yaEstaGuardada) {
                                "ELIMINAR"
                            } else {
                                "AGREGAR"
                            }
                    )

                val response =
                    ApiClient.apiService.gestionarRecetaColeccion(
                        request
                    )

                if (!response.success) {

                    Toast.makeText(
                        requireContext(),
                        response.message,
                        Toast.LENGTH_SHORT
                    ).show()

                    return@launch
                }

                // ==========================================
                // ACTUALIZAR CÍRCULO AZUL
                // ==========================================

                mostrarColecciones()

                Toast.makeText(
                    requireContext(),
                    if (yaEstaGuardada)
                        "Receta eliminada de ${coleccion.COL_NOMBRE}"
                    else
                        "Receta agregada a ${coleccion.COL_NOMBRE}",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {

                e.printStackTrace()

                Log.e(
                    "COLECCION",
                    "Error gestionando receta",
                    e
                )

                Toast.makeText(
                    requireContext(),
                    "No fue posible actualizar la colección",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}