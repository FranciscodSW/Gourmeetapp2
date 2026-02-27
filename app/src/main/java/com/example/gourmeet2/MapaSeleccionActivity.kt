package com.example.gourmeet2

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import java.util.Locale

class MapaSeleccionActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var marcador: Marker
    private val LOCATION_PERMISSION_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mapa)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment

        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        solicitarUbicacion()

        mMap.uiSettings.isZoomControlsEnabled = true
    }

    private fun solicitarUbicacion() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_CODE
            )
            return
        }

        activarUbicacion()
    }

    @SuppressLint("MissingPermission")
    private fun activarUbicacion() {

        mMap.isMyLocationEnabled = true

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {

                val ubicacionActual = LatLng(location.latitude, location.longitude)

                mMap.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(ubicacionActual, 16f)
                )

                // 🔥 Marcador arrastrable
                marcador = mMap.addMarker(
                    MarkerOptions()
                        .position(ubicacionActual)
                        .title("Arrastra para seleccionar")
                        .draggable(true)
                )!!

                // 👇 Cuando termine de arrastrar
                mMap.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {

                    override fun onMarkerDragStart(marker: Marker) {}

                    override fun onMarkerDrag(marker: Marker) {}

                    override fun onMarkerDragEnd(marker: Marker) {
                        mostrarConfirmacion(marker.position)
                    }
                })
            }
        }
    }

    private fun mostrarConfirmacion(latLng: LatLng) {

        val geocoder = Geocoder(this, Locale.getDefault())
        var direccionTexto = "Obteniendo dirección..."

        try {
            val direcciones = geocoder.getFromLocation(
                latLng.latitude,
                latLng.longitude,
                1
            )

            if (!direcciones.isNullOrEmpty()) {
                val direccion = direcciones[0]
                direccionTexto = direccion.getAddressLine(0)
            }

        } catch (e: Exception) {
            direccionTexto = "No se pudo obtener la dirección"
        }

        AlertDialog.Builder(this)
            .setTitle("Confirmar ubicación")
            .setMessage("Dirección:\n\n$direccionTexto\n\n¿Deseas seleccionar esta ubicación?")
            .setPositiveButton("Sí") { _, _ ->

                val intent = Intent()
                intent.putExtra("lat", latLng.latitude)
                intent.putExtra("lng", latLng.longitude)
                intent.putExtra("direccion", direccionTexto)

                setResult(RESULT_OK, intent)
                finish()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}