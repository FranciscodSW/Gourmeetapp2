package com.example.gourmeet2

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import java.util.Locale

class MapaSeleccionActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    private lateinit var marcador: Marker
    private val LOCATION_PERMISSION_CODE = 101
    private lateinit var autoCompleteTextView: AutoCompleteTextView
    private lateinit var suggestionsAdapter: ArrayAdapter<String>
    private val suggestionsList = mutableListOf<String>()
    private val placesList = mutableListOf<android.location.Address>()
    private val handler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mapa)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment

        mapFragment.getMapAsync(this)
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getString(R.string.google_maps_key))
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        solicitarUbicacion()
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = true
        mMap.uiSettings.isCompassEnabled = true
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

        // Configurar el buscador con autocompletado
        configurarBuscadorConListaPersonalizada()

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val ubicacionActual = LatLng(location.latitude, location.longitude)

                mMap.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(ubicacionActual, 16f)
                )

                // Marcador arrastrable
                marcador = mMap.addMarker(
                    MarkerOptions()
                        .position(ubicacionActual)
                        .title("Arrastra para seleccionar")
                        .draggable(true)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                )!!

                // Listener para arrastre
                mMap.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
                    override fun onMarkerDragStart(marker: Marker) {}
                    override fun onMarkerDrag(marker: Marker) {}
                    override fun onMarkerDragEnd(marker: Marker) {
                        mostrarConfirmacion(marker.position)
                    }
                })

                // Listener para clic en el mapa
                mMap.setOnMapClickListener { latLng ->
                    marcador.position = latLng
                    mostrarConfirmacion(latLng)
                }
            }
        }
    }

    private fun configurarBuscadorConListaPersonalizada() {

        autoCompleteTextView = findViewById(R.id.autoCompleteDireccion)

        suggestionsAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            suggestionsList
        )

        autoCompleteTextView.setAdapter(suggestionsAdapter)
        autoCompleteTextView.threshold = 2

        autoCompleteTextView.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {

                val query = s.toString()

                searchRunnable?.let { handler.removeCallbacks(it) }

                if (query.length >= 2) {

                    searchRunnable = Runnable {
                        buscarSugerenciasEnTiempoReal(query)
                    }

                    handler.postDelayed(searchRunnable!!, 500)

                } else {

                    suggestionsList.clear()
                    suggestionsAdapter.notifyDataSetChanged()

                }
            }
        })

        autoCompleteTextView.setOnItemClickListener { parent, _, position, _ ->

            val seleccion = parent.getItemAtPosition(position) as String

            val lugar = placesList.find {
                it.getAddressLine(0) == seleccion
            }

            lugar?.let {

                val latLng = LatLng(it.latitude, it.longitude)

                mMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(latLng, 18f)
                )

                marcador.position = latLng

                mostrarConfirmacion(latLng)

                autoCompleteTextView.setText("")
            }
        }
    }

    private fun buscarSugerenciasEnTiempoReal(query: String) {

        Thread {

            val geocoder = Geocoder(this, Locale.getDefault())

            try {

                val direcciones = geocoder.getFromLocationName(query, 10)

                runOnUiThread {

                    suggestionsList.clear()
                    placesList.clear()

                    if (!direcciones.isNullOrEmpty()) {

                        direcciones.forEach {

                            val direccion = it.getAddressLine(0)

                            suggestionsList.add(direccion)
                            placesList.add(it)

                        }

                    } else {

                        suggestionsList.add("No se encontraron resultados")

                    }

                    suggestionsAdapter.notifyDataSetChanged()
                    autoCompleteTextView.showDropDown()

                }

            } catch (e: Exception) {

                e.printStackTrace()

            }

        }.start()
    }

    private fun mostrarConfirmacion(latLng: LatLng) {
        val geocoder = Geocoder(this, Locale.getDefault())
        var direccionTexto = "Obteniendo dirección..."
        var direccionCompleta = ""

        try {
            val direcciones = geocoder.getFromLocation(
                latLng.latitude,
                latLng.longitude,
                1
            )
            if (!direcciones.isNullOrEmpty()) {
                val direccion = direcciones[0]
                // Construir una dirección más legible
                direccionCompleta = buildString {
                    if (!direccion.thoroughfare.isNullOrEmpty()) append("${direccion.thoroughfare}, ")
                    if (!direccion.subThoroughfare.isNullOrEmpty()) append("${direccion.subThoroughfare} - ")
                    if (!direccion.locality.isNullOrEmpty()) append("${direccion.locality}, ")
                    if (!direccion.adminArea.isNullOrEmpty()) append("${direccion.adminArea}, ")
                    append(direccion.countryName)
                }
                direccionTexto = direccion.getAddressLine(0)
            }
        } catch (e: Exception) {
            direccionTexto = "No se pudo obtener la dirección"
            direccionCompleta = "${latLng.latitude}, ${latLng.longitude}"
        }

        val direccionFinal = if (direccionTexto != "No se pudo obtener la dirección")
            direccionTexto else direccionCompleta

        AlertDialog.Builder(this)
            .setTitle("Confirmar ubicación")
            .setMessage("Dirección:\n\n$direccionFinal\n\n¿Deseas seleccionar esta ubicación?")
            .setPositiveButton("Sí") { _, _ ->
                val intent = Intent()
                intent.putExtra("lat", latLng.latitude)
                intent.putExtra("lng", latLng.longitude)
                intent.putExtra("direccion", direccionFinal)

                setResult(RESULT_OK, intent)
                finish()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            activarUbicacion()
        }
    }
}