package mx.com.gourmeet.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import mx.com.gourmeet.app.databinding.FragmentDetalleRecetaBinding
import kotlinx.coroutines.launch
import mx.com.gourmeet.app.data.api.ApiClient
import mx.com.gourmeet.app.data.models.*
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
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import mx.com.gourmeet.app.databinding.DialogReportarComentarioBinding
import mx.com.gourmeet.app.databinding.DialogReportarRecetaBinding
import mx.com.gourmeet.app.utils.SesionUsuario
import mx.com.gourmeet.app.utils.SesionUsuario.obtenerId
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.compareTo

class DetalleRecetaFragment : Fragment() {
    private lateinit var gestureDetector: GestureDetector
    private var _binding: FragmentDetalleRecetaBinding? = null
    private val binding get() = _binding!!
    private var recetaId: Int = 0
    private var recetaActual: RecetaDetalle? = null
    private var actualizandoFavorito = false
    private var verificandoFavorito = false
    private var mostrandoTodosLosPasos = false
    private var pasos = emptyList<PasoPreparacion>()
    private var pasoActual = 0
    private var speechRecognizer: SpeechRecognizer? = null
    private val vozHandler = Handler(Looper.getMainLooper())
    private val reanudarEscucha = Runnable { iniciarEscucha() }
    private var reconocimientoEnCurso = false
    private var lecturaId: String? = null
    private var secuenciaLectura = 0
    private var sintetizadorListo = false
    private val permisoMicrofono = registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido && _binding != null &&
            viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        ) {
            activarEscucha()
        }
    }
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
            when {
                escuchaContinua -> desactivarEscucha()
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) !=
                    PackageManager.PERMISSION_GRANTED -> permisoMicrofono.launch(Manifest.permission.RECORD_AUDIO)
                else -> activarEscucha()
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
        binding.layoutColeccion.setOnClickListener { mostrarColecciones() }
        binding.layoutCompartir.setOnClickListener { compartirReceta() }
        binding.layoutPlaneador.setOnClickListener { agendarReceta() }
        binding.btnCerrarDetalle.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.layoutCompartir.isEnabled = false
        binding.layoutPlaneador.isEnabled = false
    }
    private fun cargarComentarios() {

        val usuarioId = obtenerId(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
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

        viewLifecycleOwner.lifecycleScope.launch {

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
        recetaActual = null
        binding.recipeLoadStatus.text = "Cargando receta…"
        binding.recipeLoadStatus.visibility = View.VISIBLE
        binding.recipeLoadStatus.isEnabled = false
        listOf(binding.layoutCompartir, binding.layoutPlaneador, binding.layoutColeccion, binding.layoutFavoritos).forEach {
            it.isEnabled = false
            it.alpha = 0.5f
        }

        viewLifecycleOwner.lifecycleScope.launch {

            try {

                val response = ApiClient.apiService.getDetalleReceta(

                    DetalleRecetaRequest(
                        REC_ID = recetaId
                    )

                )
                if(response.success){
                    if (_binding == null) return@launch
                    val receta = response.receta
                    recetaActual = receta
                    binding.recipeLoadStatus.visibility = View.GONE
                    listOf(binding.layoutCompartir, binding.layoutPlaneador, binding.layoutColeccion, binding.layoutFavoritos).forEach {
                        it.isEnabled = true
                        it.alpha = 1f
                    }
                    binding.layoutFavoritos.isEnabled = !actualizandoFavorito && !verificandoFavorito
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
                        "${receta.REC_TIEMPO_PREPARACION} min"
                    (binding.txtCosto.parent as View).visibility = View.GONE
                    binding.txtNivel.text =
                        receta.Dificultad
                    binding.txtTipo.text =
                        receta.Categoria
                    binding.txtCalificacion.text = if (response.calificacion.total > 0)
                        "${String.format("%.1f", response.calificacion.promedio)} · ${response.calificacion.total} calificaciones"
                    else "Sin calificaciones todavía"
                    binding.ratingReceta.visibility = if (response.calificacion.total > 0) View.VISIBLE else View.GONE
                    binding.ratingReceta.rating = response.calificacion.promedio
                    binding.rvIngredientes.layoutManager =
                        LinearLayoutManager(
                            requireContext(),
                            LinearLayoutManager.VERTICAL,
                            false
                        )
                    binding.rvIngredientes.layoutManager =
                        LinearLayoutManager(
                            requireContext(),
                            LinearLayoutManager.VERTICAL,
                            false
                        )
                    binding.rvIngredientes.adapter =
                        IngredienteRecetaAdapter(
                            response.ingredientes.map { ingrediente ->
                                IngredienteReceta(
                                    id = ingrediente.ING_ID,
                                    nombre = ingrediente.ING_DESCRIPCION,
                                    foto = ingrediente.Foto_Ingrediente,
                                    cantidad = ingrediente.cantidad
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
                } else {
                    mostrarErrorCargaReceta()
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mostrarErrorCargaReceta()
            }

        }

    }
    private fun mostrarErrorCargaReceta() {
        val current = _binding ?: return
        current.recipeLoadStatus.text = "No pudimos cargar la receta. Toca aquí para reintentar."
        current.recipeLoadStatus.isEnabled = true
        current.recipeLoadStatus.setOnClickListener { cargarDetalle() }
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
    private fun crearReconocedor(): SpeechRecognizer {
        val reconocedor = SpeechRecognizer.createSpeechRecognizer(requireContext())
        reconocedor.setRecognitionListener(object : RecognitionListener {
            private fun turnoVigente() =
                speechRecognizer === reconocedor && reconocimientoEnCurso && lecturaId == null

            override fun onResults(results: Bundle?) {
                if (!turnoVigente()) return
                finalizarTurnoEscucha()
                val texto = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (texto != null) {
                    ejecutarComandoVoz(administradorComandosVoz.analizarTexto(texto))
                }
                programarEscucha()
            }

            override fun onReadyForSpeech(params: Bundle?) {
                if (!turnoVigente()) return
                escuchando = true
                iniciarAnimacionMicrofono()
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {
                if (!turnoVigente() || !escuchando) return
                val boton = _binding?.btnVoz ?: return
                val escala = (1f + rmsdB / 15f).coerceIn(1f, 1.35f)
                boton.scaleX = escala
                boton.scaleY = escala
            }

            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                if (!turnoVigente()) return
                apagarAnimacionEscucha()
            }

            override fun onError(error: Int) {
                if (!turnoVigente()) return
                finalizarTurnoEscucha()
                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                        programarEscucha()
                    else -> {
                        desactivarEscucha()
                        Toast.makeText(context, "No se pudo usar el micrófono. Toca para reintentar.",
                            Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        return reconocedor
    }

    private fun inicializarTextToSpeech() {
        val vista = binding
        textToSpeech = TextToSpeech(requireContext()) { status ->
            vozHandler.post {
                if (_binding !== vista || status != TextToSpeech.SUCCESS) return@post
                val motor = textToSpeech ?: return@post
                val voz = motor.voices?.firstOrNull { it.name == "es-es-x-eec-network" }
                if (voz != null) motor.voice = voz else motor.language = Locale.forLanguageTag("es-MX")
                motor.setSpeechRate(0.9f)
                motor.setPitch(1.0f)
                sintetizadorListo = motor.setOnUtteranceProgressListener(
                    object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {}
                        override fun onDone(utteranceId: String?) = terminarLectura(utteranceId)
                        override fun onError(utteranceId: String?) = terminarLectura(utteranceId)
                        override fun onStop(utteranceId: String?, interrupted: Boolean) =
                            terminarLectura(utteranceId)
                    }
                ) == TextToSpeech.SUCCESS
                iniciarLecturaPasoActual()
            }
        }
    }

    private fun iniciarLecturaPasoActual() {
        if (!lecturaActiva || !sintetizadorListo) return
        val paso = pasos.getOrNull(pasoActual) ?: return
        val motor = textToSpeech ?: return
        val id = "paso-${++secuenciaLectura}"
        lecturaId = id
        cancelarEscucha()
        if (motor.speak(paso.descripcion, TextToSpeech.QUEUE_FLUSH, null, id) != TextToSpeech.SUCCESS) {
            terminarLectura(id)
        }
    }

    private fun terminarLectura(id: String?) {
        vozHandler.post {
            if (id == null || id != lecturaId) return@post
            lecturaId = null
            programarEscucha()
        }
    }

    private fun detenerLectura() {
        val estabaLeyendo = lecturaId != null
        lecturaId = null
        textToSpeech?.stop()
        if (estabaLeyendo) programarEscucha()
    }

    private fun activarEscucha() {
        if (!SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            Toast.makeText(context, "El reconocimiento de voz no está disponible.", Toast.LENGTH_SHORT).show()
            return
        }
        escuchaContinua = true
        binding.btnVoz.setIconResource(R.drawable.ic_microfono_on)
        programarEscucha()
    }

    private fun iniciarEscucha() {
        if (_binding == null || !escuchaContinua || reconocimientoEnCurso || lecturaId != null) return
        val reconocedor = speechRecognizer ?: crearReconocedor().also { speechRecognizer = it }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-MX")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        reconocimientoEnCurso = true
        reconocedor.startListening(intent)
    }

    private fun programarEscucha() {
        if (!escuchaContinua || lecturaId != null) return
        vozHandler.removeCallbacks(reanudarEscucha)
        vozHandler.postDelayed(reanudarEscucha, 600L)
    }

    private fun finalizarTurnoEscucha() {
        reconocimientoEnCurso = false
        apagarAnimacionEscucha()
    }

    private fun apagarAnimacionEscucha() {
        escuchando = false
        val boton = _binding?.btnVoz ?: return
        detenerAnimacionMicrofono()
        boton.scaleX = 1f
        boton.scaleY = 1f
    }

    private fun cancelarEscucha() {
        vozHandler.removeCallbacks(reanudarEscucha)
        val reconocedor = speechRecognizer
        speechRecognizer = null
        finalizarTurnoEscucha()
        reconocedor?.cancel()
        reconocedor?.destroy()
    }

    private fun desactivarEscucha() {
        escuchaContinua = false
        _binding?.btnVoz?.setIconResource(R.drawable.ic_microfono)
        cancelarEscucha()
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
        binding.btnAnterior.isEnabled = pasoActual > 0
        binding.btnSiguiente.isEnabled = pasoActual < pasos.lastIndex
        binding.btnVerTodo.isEnabled = pasos.isNotEmpty()
        if (pasos.isEmpty()) {
            binding.tvDescripcionPaso.text = "Esta receta aún no tiene pasos de preparación."
            return
        }
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

        viewLifecycleOwner.lifecycleScope.launch {

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
        viewLifecycleOwner.lifecycleScope.launch {
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
        viewLifecycleOwner.lifecycleScope.launch {
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

        viewLifecycleOwner.lifecycleScope.launch {

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

        viewLifecycleOwner.lifecycleScope.launch {

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
    override fun onStop() {
        desactivarEscucha()
        lecturaActiva = false
        _binding?.btnParlante?.setIconResource(R.drawable.ic_audio_off)
        detenerLectura()
        super.onStop()
    }

    override fun onDestroyView() {
        desactivarEscucha()
        detenerLectura()
        sintetizadorListo = false
        textToSpeech?.shutdown()
        textToSpeech = null
        vozHandler.removeCallbacksAndMessages(null)
        animatorSet = null
        recetaActual = null
        super.onDestroyView()
        _binding = null
    }
    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()
    private fun pedirSesion(): Boolean {
        if (obtenerId(requireContext()) > 0) return true
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Guarda tus recetas")
            .setMessage("Inicia sesión para guardar recetas en tus colecciones y favoritos.")
            .setNegativeButton("Ahora no", null)
            .setPositiveButton("Iniciar sesión") { _, _ -> startActivity(Intent(requireContext(), Login::class.java)) }
            .show()
        return false
    }

    private fun compartirReceta() {
        val receta = recetaActual ?: return
        val intent = mx.com.gourmeet.app.ui.RecipeIntents.share(receta, pasos, BuildConfig.LOCAL_ONLY)
        try {
            startActivity(Intent.createChooser(intent, "Compartir receta"))
        } catch (_: android.content.ActivityNotFoundException) {
            Toast.makeText(requireContext(), "No hay una aplicación disponible para compartir.", Toast.LENGTH_LONG).show()
        }
    }

    private fun agendarReceta() {
        val receta = recetaActual ?: return
        try {
            startActivity(mx.com.gourmeet.app.ui.RecipeIntents.calendar(receta, pasos, BuildConfig.LOCAL_ONLY))
        } catch (_: android.content.ActivityNotFoundException) {
            Toast.makeText(requireContext(), "Instala o activa una aplicación de calendario para agendar la receta.", Toast.LENGTH_LONG).show()
        }
    }

    private fun actualizarFavorito(guardada: Boolean) {
        val current = _binding ?: return
        current.layoutFavoritos.isSelected = guardada
        current.imgFavorito.setImageResource(if (guardada) R.drawable.gm_favorite_on else R.drawable.gm_favorite)
        current.txtAccionFavorito.text = if (guardada) "En favoritos" else "Guardar en favoritos"
        current.layoutFavoritos.contentDescription = if (guardada) "Quitar de favoritos" else "Guardar en favoritos"
    }

    private fun cambiarFavorito() {
        if (!pedirSesion() || recetaId <= 0 || actualizandoFavorito) return
        actualizandoFavorito = true
        verificandoFavorito = true
        binding.layoutFavoritos.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.gestionarFavorito(
                    GestionarFavoritoRequest(CLI_ID = obtenerId(requireContext()), REC_ID = recetaId)
                )
                if (response.success) actualizarFavorito(response.guardada)
                Toast.makeText(requireContext(), response.message, Toast.LENGTH_SHORT).show()
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (isAdded) Toast.makeText(requireContext(), "No se pudo actualizar favoritos. Intenta de nuevo.", Toast.LENGTH_LONG).show()
            } finally {
                actualizandoFavorito = false
                _binding?.layoutFavoritos?.isEnabled = true
            }
        }
    }

    private fun verificarFavorito() {
        val cliId = obtenerId(requireContext())
        if (cliId <= 0 || recetaId <= 0) {
            actualizarFavorito(false)
            return
        }
        verificandoFavorito = true
        binding.layoutFavoritos.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.verificarFavorito(VerificarFavoritoRequest(CLI_ID = cliId, REC_ID = recetaId))
                if (response.success) actualizarFavorito(response.guardada)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _binding?.txtAccionFavorito?.text = "Guardar en favoritos"
            } finally {
                verificandoFavorito = false
                if (!actualizandoFavorito) _binding?.layoutFavoritos?.isEnabled = recetaActual != null
            }
        }
    }

    private fun mostrarColecciones() {
        if (!pedirSesion()) return
        val context = requireContext()
        val sheet = BottomSheetDialog(context)
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24.dp, 20.dp, 24.dp, 24.dp)
        }
        content.addView(TextView(context).apply {
            text = "Guardar en colección"
            textSize = 22f
            setTextColor(ContextCompat.getColor(context, R.color.gm_ink))
        })
        val status = TextView(context).apply {
            id = R.id.recipeCollectionsStatus
            text = "Cargando tus colecciones…"
            textSize = 15f
            setPadding(0, 12.dp, 0, 12.dp)
            setTextColor(ContextCompat.getColor(context, R.color.gm_muted))
        }
        content.addView(status)
        val list = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; id = R.id.recipeCollectionsList }
        val scroll = android.widget.ScrollView(context).apply { addView(list) }
        content.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 220.dp))
        val create = com.google.android.material.button.MaterialButton(context).apply {
            id = R.id.recipeCreateCollection
            text = "Crear colección"
            isAllCaps = false
            backgroundTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(context, R.color.gm_accent))
            setTextColor(Color.WHITE)
            setOnClickListener { crearColeccionDesdeReceta { cargarColeccionesEnSelector(sheet, list, status) } }
        }
        content.addView(create)
        sheet.setContentView(content)
        sheet.show()
        cargarColeccionesEnSelector(sheet, list, status)
    }

    private fun cargarColeccionesEnSelector(sheet: BottomSheetDialog, list: LinearLayout, status: TextView) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.listarColeccionesConRecetas(ListarColeccionesRecetasRequest(obtenerId(requireContext())))
                if (!sheet.isShowing || _binding == null) return@launch
                list.removeAllViews()
                if (!response.success) {
                    status.text = "No se pudieron cargar las colecciones. Cierra y vuelve a intentar."
                    return@launch
                }
                status.text = if (response.colecciones.isEmpty()) "Crea una colección para guardar esta receta." else "Toca una colección para guardar o quitar esta receta."
                response.colecciones.forEach { coleccion ->
                    val guardada = coleccion.RECETAS.any { it.REC_ID == recetaId }
                    val button = com.google.android.material.button.MaterialButton(requireContext()).apply {
                        tag = coleccion.COL_ID
                        text = if (guardada) "${coleccion.COL_NOMBRE} · Guardada" else coleccion.COL_NOMBRE
                        isAllCaps = false
                        isSelected = guardada
                        cornerRadius = 14.dp
                        minHeight = 52.dp
                        backgroundTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(context,
                            if (guardada) R.color.gm_accent else R.color.gm_accent_soft))
                        setTextColor(if (guardada) Color.WHITE else ContextCompat.getColor(context, R.color.gm_accent))
                        contentDescription = if (guardada) "Quitar de ${coleccion.COL_NOMBRE}" else "Guardar en ${coleccion.COL_NOMBRE}"
                        setOnClickListener {
                            for (i in 0 until list.childCount) list.getChildAt(i).isEnabled = false
                            viewLifecycleOwner.lifecycleScope.launch {
                                try {
                                    val result = ApiClient.apiService.gestionarRecetaColeccion(
                                        GestionarRecetaColeccionRequest(
                                            CLI_ID = obtenerId(requireContext()), COL_ID = coleccion.COL_ID,
                                            REC_ID = recetaId, ACCION = if (guardada) "ELIMINAR" else "AGREGAR"
                                        )
                                    )
                                    if (!result.success) Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
                                    verificarFavorito()
                                    cargarColeccionesEnSelector(sheet, list, status)
                                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                                    if (isAdded) Toast.makeText(requireContext(), "No se pudo guardar el cambio. Intenta de nuevo.", Toast.LENGTH_LONG).show()
                                    for (i in 0 until list.childCount) list.getChildAt(i).isEnabled = true
                                }
                            }
                        }
                    }
                    list.addView(button, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (sheet.isShowing) status.text = "Sin conexión. Cierra y vuelve a intentar."
            }
        }
    }

    private fun crearColeccionDesdeReceta(onCreated: () -> Unit) {
        val field = android.widget.EditText(requireContext()).apply {
            id = R.id.recipeCollectionName
            hint = "Nombre de la colección"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            filters = arrayOf(android.text.InputFilter.LengthFilter(100))
            setSingleLine()
        }
        val box = android.widget.FrameLayout(requireContext()).apply {
            setPadding(24.dp, 8.dp, 24.dp, 0)
            addView(field)
        }
        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Nueva colección").setView(box)
            .setNegativeButton("Cancelar", null).setPositiveButton("Crear y guardar", null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val nombre = field.text.toString().trim()
                if (nombre.isBlank()) {
                    field.error = "Escribe un nombre"
                    return@setOnClickListener
                }
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val response = ApiClient.apiService.crearColeccion(
                            CrearColeccionRequest(obtenerId(requireContext()), nombre, "ic_guardar", 1, listOf(recetaId))
                        )
                        if (response.success && _binding != null) {
                            dialog.dismiss()
                            onCreated()
                            Toast.makeText(requireContext(), "Receta guardada en $nombre", Toast.LENGTH_SHORT).show()
                        } else {
                            field.error = response.message
                        }
                    } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                        field.error = "No se pudo crear la colección. Intenta de nuevo."
                    } finally {
                        if (dialog.isShowing) dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                    }
                }
            }
        }
        dialog.show()
    }

}
