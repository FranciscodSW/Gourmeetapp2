package com.example.gourmeet2

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
import android.net.Uri
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.gourmeet2.data.models.IngredienteReceta


class DetalleRecetaFragment : Fragment() {
    private lateinit var gestureDetector: GestureDetector
    private var _binding: FragmentDetalleRecetaBinding? = null
    private val binding get() = _binding!!
    private var recetaId: Int = 0
    private var mostrandoTodosLosPasos = false
    private var pasos = emptyList<PasoPreparacion>()

    private var pasoActual = 0


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

        cargarDetalle()
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

        if (pasos.isEmpty()) return

        val paso = pasos[pasoActual]

        binding.tvDescripcionPaso.text =
            paso.descripcion

        actualizarIndicadores()

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
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

}