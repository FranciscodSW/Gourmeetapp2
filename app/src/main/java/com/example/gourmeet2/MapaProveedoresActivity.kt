package com.example.gourmeet2

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.gourmeet2.data.models.ProveedorMapa
import com.example.gourmeet2.databinding.ActivityMapaProveedoresBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import android.graphics.Bitmap
import android.graphics.Bitmap.*
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.Drawable

import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.bumptech.glide.request.transition.Transition


class MapaProveedoresActivity :
    AppCompatActivity(),
    OnMapReadyCallback {

    private lateinit var binding: ActivityMapaProveedoresBinding

    private lateinit var fusedLocationClient:
            FusedLocationProviderClient

    private lateinit var googleMap: GoogleMap

    private val proveedores =
        mutableListOf<ProveedorMapa>()

    companion object {

        private const val REQUEST_LOCATION = 1001

        const val EXTRA_PROVEEDORES =
            "proveedores"
    }

    // =====================================================
    // ON CREATE
    // =====================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        binding =
            ActivityMapaProveedoresBinding.inflate(
                layoutInflater
            )

        setContentView(binding.root)

        // =================================================
        // CLIENTE DE UBICACIÓN
        // =================================================

        fusedLocationClient =
            LocationServices
                .getFusedLocationProviderClient(this)

        // =================================================
        // RECIBIR PROVEEDORES
        // =================================================

        cargarProveedoresRecibidos()

        // =================================================
        // INICIALIZAR MAPA
        // =================================================

        val mapFragment =
            supportFragmentManager
                .findFragmentById(
                    R.id.map
                ) as? SupportMapFragment

        if (mapFragment != null) {

            mapFragment.getMapAsync(this)

        } else {

            Toast.makeText(
                this,
                "No se pudo inicializar el mapa.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // =====================================================
    // RECIBIR PROVEEDORES
    // =====================================================

    private fun cargarProveedoresRecibidos() {

        val lista =
            intent.getSerializableExtra(
                EXTRA_PROVEEDORES
            )

        if (lista is ArrayList<*>) {

            proveedores.clear()

            lista.forEach { elemento ->

                if (elemento is ProveedorMapa) {

                    proveedores.add(elemento)
                }
            }
        }
    }

    // =====================================================
    // MAPA LISTO
    // =====================================================

    override fun onMapReady(
        map: GoogleMap
    ) {

        googleMap = map

        // =================================================
        // CONFIGURACIÓN DEL MAPA
        // =================================================

        googleMap.uiSettings.isZoomControlsEnabled =
            true

        googleMap.uiSettings.isMyLocationButtonEnabled =
            true

        googleMap.uiSettings.isCompassEnabled =
            true

        // =================================================
        // MOSTRAR PROVEEDORES
        // =================================================

        mostrarProveedores()

        // =================================================
        // MOSTRAR UBICACIÓN DEL USUARIO
        // =================================================

        mostrarUbicacionUsuario()
    }

    // =====================================================
    // MOSTRAR UBICACIÓN DEL USUARIO
    // =====================================================

    private fun mostrarUbicacionUsuario() {

        val permisoFino =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )

        val permisoAproximado =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )

        // =================================================
        // SIN PERMISOS
        // =================================================

        if (
            permisoFino != PackageManager.PERMISSION_GRANTED &&
            permisoAproximado != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                REQUEST_LOCATION
            )

            return
        }

        // =================================================
        // ACTIVAR UBICACIÓN EN GOOGLE MAPS
        // =================================================

        try {

            googleMap.isMyLocationEnabled = true

        } catch (e: SecurityException) {

            return
        }

        // =================================================
        // OBTENER ÚLTIMA UBICACIÓN
        // =================================================

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->

                if (location != null) {

                    val posicionUsuario =
                        LatLng(
                            location.latitude,
                            location.longitude
                        )

                    // =====================================
                    // CENTRAR MAPA EN EL USUARIO
                    // =====================================

                    googleMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            posicionUsuario,
                            15f
                        )
                    )

                } else {

                    Toast.makeText(
                        this,
                        "No se pudo obtener tu ubicación actual.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener {

                Toast.makeText(
                    this,
                    "Error al obtener tu ubicación.",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    // =====================================================
    // MOSTRAR PROVEEDORES
    // =====================================================
    private fun mostrarProveedores() {

        for (proveedor in proveedores) {

            val posicion = LatLng(
                proveedor.latitud,
                proveedor.longitud
            )

            if (!proveedor.fotoPerfil.isNullOrEmpty()) {

                Glide.with(this)
                    .asBitmap()
                    .load(proveedor.fotoPerfil)
                    .into(
                        object : CustomTarget<Bitmap>() {

                            override fun onResourceReady(
                                resource: Bitmap,
                                transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?
                            ) {

                                // ==========================================
                                // CREAR ICONO DEL PROVEEDOR
                                // ==========================================

                                val iconoProveedor =
                                    crearIconoProveedor(resource)

                                // ==========================================
                                // AGREGAR MARCADOR
                                // ==========================================

                                googleMap.addMarker(
                                    MarkerOptions()
                                        .position(posicion)
                                        .title(
                                            proveedor.nombre
                                        )
                                        .icon(
                                            BitmapDescriptorFactory.fromBitmap(
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

            } else {

                googleMap.addMarker(
                    MarkerOptions()
                        .position(posicion)
                        .title(
                            proveedor.nombre
                        )
                )
            }
        }
    }

    private fun crearIconoProveedor(
        bitmap: Bitmap
    ): Bitmap {

        val tamaño = 68

        val resultado = Bitmap.createBitmap(
            tamaño,
            tamaño,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(resultado)

        // ==========================================
        // CONFIGURACIÓN
        // ==========================================

        val centro = tamaño / 2f

        // Tamaño del círculo exterior
        val radioExterior = 35f

        // Grosor del borde azul
        val grosorBorde = 5f

        // Radio de la fotografía
        val radioFoto =
            radioExterior - grosorBorde

        // ==========================================
        // PINTURA DEL BORDE AZUL
        // ==========================================

        val pinturaBorde = Paint(
            Paint.ANTI_ALIAS_FLAG
        )

        pinturaBorde.color =
            Color.rgb(23, 122, 255)

        pinturaBorde.style =
            Paint.Style.FILL

        canvas.drawCircle(
            centro,
            centro,
            radioExterior,
            pinturaBorde
        )

        // ==========================================
        // PREPARAR IMAGEN
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
        // RECORTAR FOTO EN CÍRCULO
        // ==========================================

        val shader =
            BitmapShader(
                foto,
                Shader.TileMode.CLAMP,
                Shader.TileMode.CLAMP
            )

        val pinturaFoto =
            Paint(Paint.ANTI_ALIAS_FLAG)

        pinturaFoto.shader = shader

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

    // =====================================================
    // RESULTADO DE PERMISOS
    // =====================================================

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (
            requestCode == REQUEST_LOCATION &&
            grantResults.isNotEmpty() &&
            grantResults.any {
                it == PackageManager.PERMISSION_GRANTED
            }
        ) {

            // =============================================
            // PERMISO CONCEDIDO
            // =============================================

            mostrarUbicacionUsuario()

        } else {

            Toast.makeText(
                this,
                "Necesitamos tu ubicación para mostrarte los proveedores cercanos.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}