package com.example.gourmeet2
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.example.gourmeet2.data.api.ApiClient
import com.example.gourmeet2.data.models.ConsultarColeccionesProveedor
import com.example.gourmeet2.data.models.CrearColeccionProveedor
import com.example.gourmeet2.data.models.Proveedor
import com.example.gourmeet2.data.models.RecetaconFiltro
import com.example.gourmeet2.data.models.SeccionProductosProveedor
import com.example.gourmeet2.databinding.FragmentDetalleProvedorBinding
import com.example.gourmeet2.utils.SesionUsuario
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.launch
class DetalleProveedorFragment : Fragment(), OnMapReadyCallback {
    private var googleMap: GoogleMap? = null
    private var _binding: FragmentDetalleProvedorBinding? = null
    private val binding get() = _binding!!
    var latitudUsuario: Double? = null
    var longitudUsuario: Double? = null
    private var proveedor: Proveedor? = null
    private var coleccionesProveedorGuardado: Set<String> =
        emptySet()
    private lateinit var productosProveedorAdapter: ProductosProveedorAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var galeriaProveedorAdapter: GaleriaProveedorAdapter
    private val categoriasIngredientes = listOf(
        "Carne",
        "Verduras",
        "Frutas",
        "Lacteos",
        "Cereales",
        "Leguminosas",
        "Especias",
        "Pastas",
        "Semillas",
        "Abarrotes",
        "Licores y destilados",
        "Industrializados",
        "Embutidos",
        "Hierbas aromáticas"
    )

    private var coleccionSeleccionadaId: Int? = null
    private var recetasProveedor = emptyList<RecetaconFiltro>()
    private var estaGuardado = false
    private var coleccionesGuardadas = emptySet<String>()

    private val REQUEST_LOCATION = 1001

    companion object {

        private const val ARG_ID_PROVEEDOR =
            "ID_PROVEEDOR"

        private const val ARG_LATITUD_USUARIO =
            "LATITUD_USUARIO"

        private const val ARG_LONGITUD_USUARIO =
            "LONGITUD_USUARIO"


        fun newInstance(
            idProveedor: String,
            latitudUsuario: Double?,
            longitudUsuario: Double?
        ): DetalleProveedorFragment {

            val fragment =
                DetalleProveedorFragment()

            val args =
                Bundle()


            // ==========================================
            // ID PROVEEDOR
            // ==========================================

            args.putString(
                ARG_ID_PROVEEDOR,
                idProveedor
            )


            // ==========================================
            // UBICACIÓN USUARIO
            // ==========================================

            if (latitudUsuario != null) {

                args.putDouble(
                    ARG_LATITUD_USUARIO,
                    latitudUsuario
                )
            }


            if (longitudUsuario != null) {

                args.putDouble(
                    ARG_LONGITUD_USUARIO,
                    longitudUsuario
                )
            }


            fragment.arguments =
                args

            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentDetalleProvedorBinding.inflate(
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

        super.onViewCreated(
            view,
            savedInstanceState
        )
        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(
                requireActivity()
            )

        binding.btnCerrarDetalleProveedor.setOnClickListener {

            parentFragmentManager.popBackStack()
        }
        binding.btnMapaProveedor.setOnClickListener {

            mostrarMapaProveedor()
        }
        binding.btnGaleriaProveedor.setOnClickListener {
            mostrarGaleriaProveedor()

        }
        binding.btnProductosProveedor.setOnClickListener {
            mostrarProductosProveedor()
        }


        val idProveedor =
            arguments?.getString(
                ARG_ID_PROVEEDOR
            )
        latitudUsuario =
            if (
                arguments?.containsKey(
                    ARG_LATITUD_USUARIO
                ) == true
            ) {
                arguments?.getDouble(
                    ARG_LATITUD_USUARIO
                )
            } else {
                null
            }


        longitudUsuario =
            if (
                arguments?.containsKey(
                    ARG_LONGITUD_USUARIO
                ) == true
            ) {
                arguments?.getDouble(
                    ARG_LONGITUD_USUARIO
                )
            } else {
                null
            }
        if (idProveedor.isNullOrEmpty()) {

            Toast.makeText(
                requireContext(),
                "No se encontró el proveedor.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        cargarProveedor(
            idProveedor.toIntOrNull() ?: 0
        )
    }

    // ==========================================
    // CARGAR PROVEEDOR
    // ==========================================

    private fun cargarProveedor(
        idProveedor: Int
    ) {

        lifecycleScope.launch {

            try {

                val respuesta =
                    ApiClient.apiService
                        .obtenerDetalleProveedor(
                            idProveedor
                        )

                if (
                    respuesta.success &&
                    respuesta.proveedor != null
                ) {

                    proveedor =
                        respuesta.proveedor

                    mostrarInformacionProveedor(
                        respuesta.proveedor,
                        latitudUsuario,
                        longitudUsuario
                    )
                    mostrarProductosProveedor()
                    cargarColeccionesProveedor(
                        respuesta.proveedor.Id_Proveedor.toInt()
                    )
                    cargarRecetasProveedor(
                        respuesta.proveedor.Id_Proveedor.toInt()
                    )

                } else {

                    Toast.makeText(
                        requireContext(),
                        respuesta.mensaje
                            ?: "No se pudo obtener la información.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {

                Toast.makeText(
                    requireContext(),
                    "Error al cargar el proveedor.",
                    Toast.LENGTH_SHORT
                ).show()

                e.printStackTrace()
            }
        }
    }

    // ==========================================
    // MOSTRAR INFORMACIÓN
    // ==========================================

    private fun mostrarInformacionProveedor(
        proveedor: Proveedor,
        latitudUsuario: Double?,
        longitudUsuario: Double?
    ) {

        // ==========================================
        // NOMBRE
        // ==========================================

        binding.txtNombreProveedor.text =
            proveedor.Pro_nombre
                ?: "Proveedor"


        // ==========================================
        // GIRO
        // ==========================================

        binding.txtGiroProveedor.text =
            proveedor.Pro_GIRO
                ?: ""


        // ==========================================
        // DESCRIPCIÓN DEL GIRO
        // ==========================================

        binding.txtDescripcionGiroProveedor.text =
            proveedor.Pro_Des_Giro
                ?: ""


        // ==========================================
        // LOGO
        // ==========================================

        Glide.with(this)
            .load(proveedor.Pro_Foto_Perfil)
            .placeholder(
                R.drawable.logo_blanco_negro
            )
            .error(
                R.drawable.logo_blanco_negro
            )
            .centerCrop()
            .into(
                binding.imgLogoProveedor
            )


        // ==========================================
        // PORTADA
        // ==========================================

        Glide.with(this)
            .load(proveedor.Pro_Foto)
            .placeholder(
                R.drawable.ic_logo_circular
            )
            .error(
                R.drawable.ic_logo_circular
            )
            .centerInside()
            .into(
                binding.imgPortadaProveedor
            )


        // ==========================================
        // HORARIO
        // ==========================================

        binding.txtHorarioProveedor.text =
            construirHorario(
                proveedor
            )


        // ==========================================
        // DISTANCIA
        // ==========================================

        // Por el momento dejamos el texto.
        // Después podemos calcularla usando
        //
        calcularDistanciaProveedor(
            proveedor,
            latitudUsuario,
            longitudUsuario
        )




        // ==========================================
        // REDES SOCIALES
        // ==========================================

        binding.btnFacebookProveedor.visibility =
            if (
                proveedor.Pro_Facebook.isNullOrEmpty()
            ) {
                View.GONE
            } else {
                View.VISIBLE
            }


        binding.btnInstagramProveedor.visibility =
            if (
                proveedor.Pro_Instagram.isNullOrEmpty()
            ) {
                View.GONE
            } else {
                View.VISIBLE
            }


        binding.btnTikTokProveedor.visibility =
            if (
                proveedor.Pro_Tiktok.isNullOrEmpty()
            ) {
                View.GONE
            } else {
                View.VISIBLE
            }


        binding.btnWhatsappProveedor.visibility =
            if (
                proveedor.Pro_Telefono.isNullOrEmpty()
            ) {
                View.GONE
            } else {
                View.VISIBLE
            }
        // ==========================================
// FACEBOOK
// ==========================================

        binding.btnFacebookProveedor.setOnClickListener {

            proveedor.Pro_Facebook?.let { url ->

                abrirEnlace(url)
            }
        }


// ==========================================
// INSTAGRAM
// ==========================================

        binding.btnInstagramProveedor.setOnClickListener {

            proveedor.Pro_Instagram?.let { url ->

                abrirEnlace(url)
            }
        }


// ==========================================
// TIKTOK
// ==========================================

        binding.btnTikTokProveedor.setOnClickListener {

            proveedor.Pro_Tiktok?.let { url ->

                abrirEnlace(url)
            }
        }



// ==========================================
// WHATSAPP
// ==========================================

        binding.btnWhatsappProveedor.setOnClickListener {

            proveedor.Pro_Telefono?.let { telefono ->

                abrirWhatsApp(
                    telefono
                )
            }
        }
    }

    // ==========================================
    // HORARIO
    // ==========================================

    private fun abreviarDias(
        dias: String
    ): String {

        val ordenDias =
            listOf(
                "lunes",
                "martes",
                "miércoles",
                "jueves",
                "viernes",
                "sábado",
                "domingo"
            )

        val nombresDias =
            mapOf(
                "lunes" to "Lunes",
                "martes" to "Martes",
                "miércoles" to "Miércoles",
                "jueves" to "Jueves",
                "viernes" to "Viernes",
                "sábado" to "Sábado",
                "domingo" to "Domingo"
            )

        // ==========================================
        // SEPARAR DÍAS
        // ==========================================

        val diasSeleccionados =
            dias.split(",")
                .map {
                    it.trim()
                        .lowercase()
                        .replace("miercoles", "miércoles")
                        .replace("sabado", "sábado")
                }
                .filter {
                    it in ordenDias
                }
                .distinct()


        if (diasSeleccionados.isEmpty()) {
            return dias
        }


        // ==========================================
        // ORDENAR SEGÚN LA SEMANA
        // ==========================================

        val diasOrdenados =
            ordenDias.filter {
                it in diasSeleccionados
            }


        // ==========================================
        // SI SON CONSECUTIVOS
        // ==========================================

        if (diasOrdenados.size >= 2) {

            val posiciones =
                diasOrdenados.map {
                    ordenDias.indexOf(it)
                }

            var consecutivos = true

            for (i in 1 until posiciones.size) {

                if (
                    posiciones[i] !=
                    posiciones[i - 1] + 1
                ) {

                    consecutivos = false
                    break
                }
            }

            if (consecutivos) {

                val primero =
                    nombresDias[
                        diasOrdenados.first()
                    ]

                val ultimo =
                    nombresDias[
                        diasOrdenados.last()
                    ]

                return "$primero a $ultimo"
            }
        }


        // ==========================================
        // 1 SOLO DÍA
        // ==========================================

        if (diasOrdenados.size == 1) {

            return nombresDias[
                diasOrdenados.first()
            ] ?: dias
        }


        // ==========================================
        // DÍAS NO CONSECUTIVOS
        // ==========================================

        val nombres =
            diasOrdenados.map {
                nombresDias[it] ?: it
            }

        return when {

            nombres.size == 2 ->
                "${nombres[0]} y ${nombres[1]}"

            else ->
                nombres.dropLast(1)
                    .joinToString(", ") +
                        " y " +
                        nombres.last()
        }
    }
    private fun construirHorario(
        proveedor: Proveedor
    ): String {

        val apertura =
            proveedor.Pro_Hora_Apertura
                ?.take(5)

        val cierre =
            proveedor.Pro_Hora_Cierre
                ?.take(5)

        val dias =
            proveedor.Pro_Dias
                ?.trim()


        // ==========================================
        // DÍAS ABREVIADOS
        // ==========================================

        val diasMostrar =
            if (!dias.isNullOrEmpty()) {

                abreviarDias(dias)

            } else {

                ""
            }


        // ==========================================
        // SIN HORARIO
        // ==========================================

        if (
            apertura.isNullOrEmpty() ||
            cierre.isNullOrEmpty()
        ) {

            return if (
                diasMostrar.isNotEmpty()
            ) {

                diasMostrar

            } else {

                "Horario no disponible"
            }
        }


        // ==========================================
        // SIN DÍAS
        // ==========================================

        if (
            diasMostrar.isEmpty()
        ) {

            return "$apertura - $cierre hrs"
        }


        // ==========================================
        // RESULTADO
        // ==========================================

        return "$diasMostrar\n$apertura - $cierre hrs"
    }
    // ==========================================
// ABRIR RED SOCIAL
// ==========================================

    private fun abrirEnlace(
        url: String
    ) {

        try {

            var enlace = url.trim()

            // Si la URL no tiene http/https
            if (
                !enlace.startsWith("http://") &&
                !enlace.startsWith("https://")
            ) {

                enlace =
                    "https://$enlace"
            }

            val intent =
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(enlace)
                )

            startActivity(intent)

        } catch (e: Exception) {

            Toast.makeText(
                requireContext(),
                "No se pudo abrir el enlace.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    // ==========================================
// ABRIR WHATSAPP
// ==========================================

    private fun abrirWhatsApp(
        telefono: String
    ) {

        try {

            // Eliminamos espacios, guiones,
            // paréntesis, etc.
            val numero =
                telefono
                    .replace(
                        Regex("[^0-9+]"),
                        ""
                    )

            val uri =
                Uri.parse(
                    "https://wa.me/52$numero"
                )

            val intent =
                Intent(
                    Intent.ACTION_VIEW,
                    uri
                )

            startActivity(intent)

        } catch (e: Exception) {

            Toast.makeText(
                requireContext(),
                "No se pudo abrir WhatsApp.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    private fun calcularDistanciaProveedor(
        proveedor: Proveedor,
        latitudUsuario: Double?,
        longitudUsuario: Double?
    ) {

        // ==========================================
        // UBICACIÓN DEL USUARIO
        // ==========================================

        val latUsuario =
            latitudUsuario

        val lonUsuario =
            longitudUsuario


        // ==========================================
        // UBICACIÓN DEL PROVEEDOR
        // ==========================================

        val latProveedor =
            proveedor.Pro_Latitud
                ?.toDoubleOrNull()

        val lonProveedor =
            proveedor.Pro_Longitud
                ?.toDoubleOrNull()


        // ==========================================
        // VALIDAR UBICACIONES
        // ==========================================

        if (
            latUsuario != null &&
            lonUsuario != null &&
            latProveedor != null &&
            lonProveedor != null
        ) {

            // ==========================================
            // UBICACIÓN DEL USUARIO
            // ==========================================

            val ubicacionUsuario =
                Location("usuario").apply {

                    latitude =
                        latUsuario

                    longitude =
                        lonUsuario
                }


            // ==========================================
            // UBICACIÓN DEL PROVEEDOR
            // ==========================================

            val ubicacionProveedor =
                Location("proveedor").apply {

                    latitude =
                        latProveedor

                    longitude =
                        lonProveedor
                }


            // ==========================================
            // CALCULAR DISTANCIA
            // ==========================================

            val distancia =
                ubicacionUsuario.distanceTo(
                    ubicacionProveedor
                )


            // ==========================================
            // MOSTRAR DISTANCIA
            // ==========================================

            binding.txtDistanciaProveedor.text =
                if (distancia < 1000) {

                    "A ${distancia.toInt()} m de ti"

                } else {

                    String.format(
                        "A %.1f km de ti",
                        distancia / 1000
                    )
                }

        } else {

            // ==========================================
            // DISTANCIA NO DISPONIBLE
            // ==========================================

            binding.txtDistanciaProveedor.text =
                "Distancia no disponible"
        }
    }
    private fun mostrarUbicacionesEnMapa(
        map: GoogleMap
    ) {

        val proveedorActual =
            proveedor


        if (proveedorActual == null) {

            Toast.makeText(
                requireContext(),
                "No se encontró la información del proveedor.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        // ==========================================
        // COORDENADAS DEL PROVEEDOR
        // ==========================================

        val latProveedor =
            proveedorActual.Pro_Latitud
                ?.toDoubleOrNull()

        val lonProveedor =
            proveedorActual.Pro_Longitud
                ?.toDoubleOrNull()


        if (
            latProveedor == null ||
            lonProveedor == null
        ) {

            Toast.makeText(
                requireContext(),
                "El proveedor no tiene ubicación registrada.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        // ==========================================
        // UBICACIÓN DEL PROVEEDOR
        // ==========================================

        val posicionProveedor =
            LatLng(
                latProveedor,
                lonProveedor
            )


        // ==========================================
        // CARGAR LOGO DEL PROVEEDOR
        // ==========================================

        Glide.with(this)
            .asBitmap()
            .load(proveedorActual.Pro_Foto_Perfil)
            .placeholder(
                R.drawable.logo_blanco_negro
            )
            .error(
                R.drawable.logo_blanco_negro
            )
            .into(
                object : CustomTarget<Bitmap>() {

                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?
                    ) {

                        val iconoProveedor =
                            crearIconoProveedor(
                                resource
                            )

                        map.addMarker(
                            MarkerOptions()
                                .position(
                                    posicionProveedor
                                )
                                .title(
                                    proveedorActual.Pro_nombre
                                        ?: "Proveedor"
                                )
                                .snippet(
                                    proveedorActual.Pro_Direccion
                                        ?: ""
                                )
                                .icon(
                                    BitmapDescriptorFactory
                                        .fromBitmap(
                                            iconoProveedor
                                        )
                                )
                        )
                    }

                    override fun onLoadCleared(
                        placeholder: Drawable?
                    ) {
                    }
                }
            )


        // ==========================================
        // UBICACIÓN DEL USUARIO
        // ==========================================

        val latUsuario =
            latitudUsuario

        val lonUsuario =
            longitudUsuario


        if (
            latUsuario != null &&
            lonUsuario != null
        ) {

            val posicionUsuario =
                LatLng(
                    latUsuario,
                    lonUsuario
                )


            // ==========================================
            // MARCADOR DEL USUARIO
            // ==========================================

            map.addMarker(

                MarkerOptions()
                    .position(
                        posicionUsuario
                    )
                    .title(
                        "Tu ubicación"
                    )
                    .icon(
                        BitmapDescriptorFactory
                            .defaultMarker(
                                BitmapDescriptorFactory
                                    .HUE_AZURE
                            )
                    )
            )


            // ==========================================
            // AJUSTAR CÁMARA
            // ==========================================

            val limites =
                com.google.android.gms.maps.model.LatLngBounds
                    .Builder()
                    .include(
                        posicionUsuario
                    )
                    .include(
                        posicionProveedor
                    )
                    .build()


            map.animateCamera(
                CameraUpdateFactory.newLatLngBounds(
                    limites,
                    100
                )
            )

        } else {

            // ==========================================
            // SOLO PROVEEDOR
            // ==========================================

            map.animateCamera(

                CameraUpdateFactory.newLatLngZoom(
                    posicionProveedor,
                    15f
                )
            )
        }
    }
    private fun mostrarGaleriaProveedor() {

        // ==========================================
        // BOTÓN MAPA - INACTIVO
        // ==========================================

        binding.btnMapaProveedor.apply {

            setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.azulgourmeet
                )
            )

            backgroundTintList =
                ContextCompat.getColorStateList(
                    requireContext(),
                    R.color.white
                )

            strokeWidth = 1

            strokeColor =
                ContextCompat.getColorStateList(
                    requireContext(),
                    R.color.azulgourmeet
                )
        }


        // ==========================================
        // BOTÓN GALERÍA - ACTIVO
        // ==========================================

        binding.btnGaleriaProveedor.apply {

            setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.white
                )
            )

            backgroundTintList =
                ContextCompat.getColorStateList(
                    requireContext(),
                    R.color.azulgourmeet
                )

            strokeWidth = 0
        }


        // ==========================================
        // BOTÓN PRODUCTOS - INACTIVO
        // ==========================================

        binding.btnProductosProveedor.apply {

            setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.azulgourmeet
                )
            )

            backgroundTintList =
                ContextCompat.getColorStateList(
                    requireContext(),
                    R.color.white
                )

            strokeWidth = 1

            strokeColor =
                ContextCompat.getColorStateList(
                    requireContext(),
                    R.color.azulgourmeet
                )
        }


        // ==========================================
        // TÍTULO
        // ==========================================

        binding.txtQueOfrece.text =
            "Galería del proveedor"


        // ==========================================
        // MOSTRAR GALERÍA
        // ==========================================

        binding.rvGaleriaProveedor.visibility =
            View.VISIBLE


        // ==========================================
        // OCULTAR PRODUCTOS
        // ==========================================

        binding.rvProductosProveedor.visibility =
            View.GONE


        // ==========================================
        // OCULTAR MAPA
        // ==========================================

        val mapFragment =
            childFragmentManager.findFragmentById(
                R.id.mapProveedor
            ) as? SupportMapFragment

        mapFragment?.view?.visibility =
            View.GONE


        // ==========================================
        // IMÁGENES TEMPORALES
        // ==========================================

        val imagenesGaleria =
            listOf(
                null,
                null
            )


        // ==========================================
        // CREAR ADAPTER
        // ==========================================

        galeriaProveedorAdapter =
            GaleriaProveedorAdapter(
                imagenesGaleria
            )


        // ==========================================
        // CONFIGURAR RECYCLER
        // ==========================================

        binding.rvGaleriaProveedor.apply {

            layoutManager =
                LinearLayoutManager(
                    requireContext(),
                    LinearLayoutManager.HORIZONTAL,
                    false
                )

            adapter =
                galeriaProveedorAdapter

            setHasFixedSize(true)

            isNestedScrollingEnabled =
                false

            overScrollMode =
                View.OVER_SCROLL_NEVER
        }
    }
    private fun mostrarMapaProveedor() {

        // ==========================================
        // BOTÓN MAPA
        // ==========================================

        binding.btnMapaProveedor.apply {

            setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.white
                )
            )

            backgroundTintList =
                ContextCompat.getColorStateList(
                    requireContext(),
                    R.color.azulgourmeet
                )

            strokeWidth = 0
        }
        binding.btnGaleriaProveedor.apply {
            setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.azulgourmeet
                )
            )

            backgroundTintList =
                ContextCompat.getColorStateList(
                    requireContext(),
                    R.color.white
                )


            strokeWidth = 1

            strokeColor =
                ContextCompat.getColorStateList(
                    requireContext(),
                    R.color.azulgourmeet
                )
        }
        binding.btnProductosProveedor.apply {
            setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.azulgourmeet
                )
            )

            backgroundTintList =
                ContextCompat.getColorStateList(
                    requireContext(),
                    R.color.white
                )


            strokeWidth = 1

            strokeColor =
                ContextCompat.getColorStateList(
                    requireContext(),
                    R.color.azulgourmeet
                )

        }


        // ==========================================
        // TÍTULO
        // ==========================================

        binding.txtQueOfrece.text =
            "Mira la ubicación del proveedor"


        // ==========================================
        // OCULTAR PRODUCTOS
        // ==========================================

        binding.rvProductosProveedor.visibility =
            View.GONE


        // ==========================================
        // OCULTAR GALERÍA
        // ==========================================

        binding.rvGaleriaProveedor.visibility =
            View.GONE


        // ==========================================
        // OBTENER MAPA
        // ==========================================

        val mapFragment =
            childFragmentManager.findFragmentById(
                R.id.mapProveedor
            ) as? SupportMapFragment


        // ==========================================
        // MOSTRAR MAPA
        // ==========================================

        mapFragment?.view?.visibility =
            View.VISIBLE


        // ==========================================
        // PREPARAR MAPA
        // ==========================================

        mapFragment?.getMapAsync(this)
    }
    private fun crearIconoProveedor(
        bitmap: Bitmap
    ): Bitmap {

        val tamaño = 68

        val resultado =
            Bitmap.createBitmap(
                tamaño,
                tamaño,
                Bitmap.Config.ARGB_8888
            )

        val canvas =
            Canvas(resultado)

        // ==========================================
        // CONFIGURACIÓN
        // ==========================================

        val centro =
            tamaño / 2f

        val radioExterior =
            34f

        val grosorBorde =
            5f

        val radioFoto =
            radioExterior - grosorBorde


        // ==========================================
        // BORDE AZUL
        // ==========================================

        val pinturaBorde =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            )

        pinturaBorde.color =
            Color.rgb(
                23,
                122,
                255
            )

        pinturaBorde.style =
            Paint.Style.FILL

        canvas.drawCircle(
            centro,
            centro,
            radioExterior,
            pinturaBorde
        )


        // ==========================================
        // TAMAÑO DE LA FOTO
        // ==========================================

        val tamañoFoto =
            (radioFoto * 2).toInt()


        val foto =
            Bitmap.createScaledBitmap(
                bitmap,
                tamañoFoto,
                tamañoFoto,
                true
            )


        // ==========================================
        // RECORTE CIRCULAR
        // ==========================================

        val shader =
            BitmapShader(
                foto,
                Shader.TileMode.CLAMP,
                Shader.TileMode.CLAMP
            )


        val pinturaFoto =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            )

        pinturaFoto.shader =
            shader


        // ==========================================
        // DIBUJAR FOTO
        // ==========================================

        canvas.drawCircle(
            centro,
            centro,
            radioFoto,
            pinturaFoto
        )


        return resultado
    }
    private fun crearSeccionesProductosProveedor(
        proveedor: Proveedor,
        recetasProveedor: List<RecetaconFiltro>
    ): MutableList<SeccionProductosProveedor> {

        val secciones =
            mutableListOf<SeccionProductosProveedor>()

        // ==========================================
        // INGREDIENTES
        // ==========================================

        for (categoria in categoriasIngredientes) {

            val ingredientesCategoria =
                proveedor.INGREDIENTES.filter { ingrediente ->

                    ingrediente.CATEGORIA
                        ?.trim()
                        ?.equals(
                            categoria.trim(),
                            ignoreCase = true
                        ) == true
                }

            if (ingredientesCategoria.isNotEmpty()) {

                secciones.add(
                    SeccionProductosProveedor(
                        titulo = categoria,
                        ingredientes = ingredientesCategoria,
                        recetas = emptyList(),
                        esRecetas = false,
                        nombreColeccion = categoria

                    )
                )
            }
        }

        // ==========================================
        // RECETAS DEL PROVEEDOR
        // ==========================================

        if (recetasProveedor.isNotEmpty()) {

            secciones.add(
                SeccionProductosProveedor(
                    titulo = "Recetas del proveedor",
                    ingredientes = emptyList(),
                    recetas = recetasProveedor,
                    esRecetas = true,
                    nombreColeccion = proveedor.Pro_Des_Giro?.trim()
                )
            )
        }

        return secciones
    }
    private fun cargarRecetasProveedor(
        idProveedor: Int
    ) {

        lifecycleScope.launch {

            try {

                val respuesta =
                    ApiClient.apiService
                        .listarRecetasProveedor(
                            idProveedor
                        )

                if (respuesta.success) {

                    val recetas =
                        respuesta.recetas
                            ?: emptyList()

                    val proveedorActual =
                        proveedor
                            ?: return@launch


                    // ==========================================
                    // CREAR SECCIONES COMPLETAS
                    // ==========================================

                    val secciones =
                        crearSeccionesProductosProveedor(
                            proveedorActual,
                            recetas
                        )


                    // ==========================================
                    // CREAR / ACTUALIZAR ADAPTER
                    // ==========================================

                    productosProveedorAdapter =
                        ProductosProveedorAdapter(

                            secciones = secciones,

                            // ==========================================
                            // GUARDAR PROVEEDOR EN COLECCIÓN
                            // ==========================================

                            onGuardarProveedor = { seccion, posicion ->

                                guardarProveedorEnColeccion(
                                    seccion,
                                    posicion
                                )
                            },

                            // ==========================================
                            // CLICK EN RECETA
                            // ==========================================

                            onRecetaClick = { recetaId ->

                                (activity as? Menu_principal_free)?.abrirDetalleReceta(
                                    recetaId
                                )
                            }
                        )


                    // ==========================================
                    // ACTUALIZAR RECYCLER
                    // ==========================================

                    binding.rvProductosProveedor.adapter =
                        productosProveedorAdapter


                } else {

                    Toast.makeText(
                        requireContext(),
                        "No se pudieron cargar las recetas.",
                        Toast.LENGTH_SHORT
                    ).show()
                }


            } catch (e: Exception) {

                e.printStackTrace()

                Toast.makeText(
                    requireContext(),
                    "Error al cargar las recetas.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    private fun guardarProveedorEnColeccion(
        seccion: SeccionProductosProveedor,
        posicion: Int
    ) {

        // ==========================================
        // VALIDAR USUARIO
        // ==========================================

        val clienteId =
            SesionUsuario.obtenerId(
                requireContext()
            )

        if (clienteId <= 0) {

            Toast.makeText(
                requireContext(),
                "Debes iniciar sesión.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        // ==========================================
        // OBTENER PROVEEDOR
        // ==========================================

        val proveedorActual =
            proveedor

        if (proveedorActual == null) {

            Toast.makeText(
                requireContext(),
                "No se encontró el proveedor.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        // ==========================================
        // ID DEL PROVEEDOR
        // ==========================================

        val proveedorId =
            proveedorActual.Id_Proveedor
                .toIntOrNull()

        if (proveedorId == null || proveedorId <= 0) {

            Toast.makeText(
                requireContext(),
                "ID del proveedor no válido.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        // ==========================================
        // DETERMINAR COLECCIÓN
        // ==========================================

        val nombreColeccion =
            if (seccion.esRecetas) {

                // ======================================
                // RECETA
                // ======================================

                proveedorActual.Pro_Des_Giro
                    ?.trim()
                    .orEmpty()

            } else {

                // ======================================
                // INGREDIENTE
                // ======================================

                seccion.titulo
                    .trim()
            }


        // ==========================================
        // VALIDAR COLECCIÓN
        // ==========================================

        if (nombreColeccion.isEmpty()) {

            Toast.makeText(
                requireContext(),
                "No se pudo determinar la colección.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        // ==========================================
        // CREAR DATOS
        // ==========================================

        val datos =
            CrearColeccionProveedor(

                CLI_ID = clienteId,

                PRO_ID = proveedorId,

                NOMBRE_COLECCION = nombreColeccion
            )


        // ==========================================
        // ENVIAR A LA API
        // ==========================================

        lifecycleScope.launch {

            try {

                val respuesta =
                    ApiClient.apiService
                        .crearProveedorColeccion(
                            datos
                        )


                // ======================================
                // RESPUESTA EXITOSA
                // ======================================

                if (respuesta.success) {

                    // ==================================
                    // PROVEEDOR GUARDADO
                    // ==================================

                    if (respuesta.guardado) {

                        Toast.makeText(
                            requireContext(),
                            "Proveedor guardado en $nombreColeccion.",
                            Toast.LENGTH_SHORT
                        ).show()

                    }

                    // ==================================
                    // PROVEEDOR ELIMINADO
                    // ==================================

                    else {

                        Toast.makeText(
                            requireContext(),
                            "Proveedor eliminado de $nombreColeccion.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }


                    // ==================================
                    // ACTUALIZAR ICONO
                    // ==================================

                    productosProveedorAdapter
                        .actualizarEstadoGuardado(
                            posicion,
                            respuesta.guardado
                        )


                } else {

                    // ==================================
                    // ERROR DE API
                    // ==================================

                    Toast.makeText(
                        requireContext(),
                        respuesta.mensaje
                            ?: "No se pudo realizar la operación.",
                        Toast.LENGTH_SHORT
                    ).show()
                }


            } catch (e: Exception) {

                e.printStackTrace()

                Toast.makeText(
                    requireContext(),
                    "Error al guardar el proveedor.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    private fun mostrarProductosProveedor() {

        // ==========================================
        // BOTÓN MAPA - INACTIVO
        // ==========================================

        binding.btnMapaProveedor.apply {

            setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.azulgourmeet
                )
            )

            backgroundTintList =
                ContextCompat.getColorStateList(
                    requireContext(),
                    R.color.white
                )

            strokeWidth = 1

            strokeColor =
                ContextCompat.getColorStateList(
                    requireContext(),
                    R.color.azulgourmeet
                )
        }


        // ==========================================
        // BOTÓN GALERÍA - INACTIVO
        // ==========================================

        binding.btnGaleriaProveedor.apply {

            setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.azulgourmeet
                )
            )

            backgroundTintList =
                ContextCompat.getColorStateList(
                    requireContext(),
                    R.color.white
                )

            strokeWidth = 1

            strokeColor =
                ContextCompat.getColorStateList(
                    requireContext(),
                    R.color.azulgourmeet
                )
        }


        // ==========================================
        // BOTÓN PRODUCTOS - ACTIVO
        // ==========================================

        binding.btnProductosProveedor.apply {

            setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.white
                )
            )

            backgroundTintList =
                ContextCompat.getColorStateList(
                    requireContext(),
                    R.color.azulgourmeet
                )

            strokeWidth = 0
        }


        // ==========================================
        // TÍTULO
        // ==========================================

        binding.txtQueOfrece.text =
            "¿Qué te ofrece “${proveedor?.Pro_nombre ?: "este proveedor"}”?"


        // ==========================================
        // MOSTRAR PRODUCTOS
        // ==========================================

        binding.rvProductosProveedor.visibility =
            View.VISIBLE

        binding.rvGaleriaProveedor.visibility =
            View.GONE


        // ==========================================
        // OCULTAR MAPA
        // ==========================================

        val mapFragment =
            childFragmentManager.findFragmentById(
                R.id.mapProveedor
            ) as? SupportMapFragment

        mapFragment?.view?.visibility =
            View.GONE


        // ==========================================
        // OBTENER PROVEEDOR
        // ==========================================

        val proveedorActual =
            proveedor
                ?: return


        // ==========================================
        // CREAR SECCIONES INICIALES
        // ==========================================

        val seccionesIniciales =
            crearSeccionesProductosProveedor(
                proveedorActual,
                emptyList()
            )


        // ==========================================
        // CREAR ADAPTER
        // ==========================================

        productosProveedorAdapter =
            ProductosProveedorAdapter(

                secciones = seccionesIniciales,

                // ==========================================
                // GUARDAR PROVEEDOR EN COLECCIÓN
                // ==========================================

                onGuardarProveedor = { seccion, posicion ->

                    guardarProveedorEnColeccion(
                        seccion,
                        posicion
                    )
                },

                // ==========================================
                // CLICK EN RECETA
                // ==========================================

                onRecetaClick = { recetaId ->

                    (activity as? Menu_principal_free)?.abrirDetalleReceta(
                        recetaId
                    )
                }
            )


        // ==========================================
        // CONFIGURAR RECYCLER
        // ==========================================

        binding.rvProductosProveedor.apply {

            layoutManager =
                LinearLayoutManager(
                    requireContext(),
                    LinearLayoutManager.VERTICAL,
                    false
                )

            adapter =
                productosProveedorAdapter

            setHasFixedSize(false)

            isNestedScrollingEnabled =
                true

            overScrollMode =
                View.OVER_SCROLL_NEVER
        }
        // ==========================================
        // CONSULTAR RECETAS
        // ==========================================
        cargarRecetasProveedor(
            proveedorActual.Id_Proveedor
                .toIntOrNull()
                ?: 0
        )
    }
    private fun cargarColeccionesProveedor(
        idProveedor: Int
    ) {

        // ==========================================
        // OBTENER USUARIO
        // ==========================================

        val clienteId =
            SesionUsuario.obtenerId(
                requireContext()
            )

        if (clienteId <= 0) {
            return
        }


        // ==========================================
        // VALIDAR PROVEEDOR
        // ==========================================

        if (idProveedor <= 0) {
            return
        }


        // ==========================================
        // CONSULTAR API
        // ==========================================

        lifecycleScope.launch {

            try {

                val datos =
                    ConsultarColeccionesProveedor(
                        CLI_ID = clienteId
                    )


                val respuesta =
                    ApiClient.apiService
                        .listarColeccionesProveedores(
                            datos
                        )


                // ======================================
                // RESPUESTA CORRECTA
                // ======================================

                if (respuesta.success) {

                    // ==================================
                    // OBTENER COLECCIONES
                    // ==================================

                    val colecciones =
                        respuesta.colecciones
                            ?: emptyList()


                    // ==================================
                    // GUARDAR NOMBRES DE COLECCIONES
                    // ==================================

                    coleccionesProveedorGuardado =
                        colecciones
                            .mapNotNull {
                                it.COP_NOMBRE
                                    ?.trim()
                                    ?.takeIf { nombre -> nombre.isNotEmpty() }
                            }
                            .toSet()


                    // ==================================
                    // ACTUALIZAR ICONOS
                    // ==================================

                    if (::productosProveedorAdapter.isInitialized) {

                        productosProveedorAdapter
                            .actualizarColeccionesGuardadas(
                                coleccionesProveedorGuardado
                            )
                    }

                } else {

                    coleccionesProveedorGuardado =
                        emptySet()
                }


            } catch (e: Exception) {

                e.printStackTrace()

                coleccionesProveedorGuardado =
                    emptySet()
            }
        }
    }
    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }

    override fun onMapReady(
        map: GoogleMap
    ) {

        googleMap =
            map


        // ==========================================
        // CONFIGURACIÓN
        // ==========================================

        map.uiSettings.isZoomControlsEnabled =
            true

        map.uiSettings.isMapToolbarEnabled =
            true

        map.uiSettings.isCompassEnabled =
            true


        // ==========================================
        // MOSTRAR UBICACIONES
        // ==========================================

        mostrarUbicacionesEnMapa(
            map
        )
    }
}