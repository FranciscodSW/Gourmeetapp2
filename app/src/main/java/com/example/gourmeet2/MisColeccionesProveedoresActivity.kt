package com.example.gourmeet2

import android.R.attr.overScrollMode
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gourmeet2.data.api.ApiClient
import com.example.gourmeet2.data.models.ConsultarColeccionesProveedor
import com.example.gourmeet2.data.models.ColeccionProveedor
import com.example.gourmeet2.data.models.ListarColeccionesProveedores
import com.example.gourmeet2.data.models.Proveedor
import com.example.gourmeet2.databinding.ActivityMisColeccionesProveedoresBinding
import com.example.gourmeet2.utils.SesionUsuario
import kotlinx.coroutines.launch

class MisColeccionesProveedoresActivity :
    AppCompatActivity() {

    private lateinit var binding: ActivityMisColeccionesProveedoresBinding


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        binding =
            ActivityMisColeccionesProveedoresBinding.inflate(
                layoutInflater
            )

        setContentView(binding.root)


        // ==========================================
        // CARGAR COLECCIONES
        // ==========================================

        cargarColeccionesProveedores()
    }


    private fun cargarColeccionesProveedores() {

        // ==========================================
        // OBTENER USUARIO
        // ==========================================

        val clienteId =
            SesionUsuario.obtenerId(this)


        if (clienteId <= 0) {

            Toast.makeText(
                this,
                "Debes iniciar sesión.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        // ==========================================
        // CONSULTAR API
        // ==========================================

        lifecycleScope.launch {

            try {

                // ======================================
                // CREAR DATOS DE CONSULTA
                // ======================================

                val datos =
                    ConsultarColeccionesProveedor(
                        CLI_ID = clienteId
                    )


                // ======================================
                // LLAMAR API
                // ======================================

                val respuesta =
                    ApiClient.apiService
                        .listarColeccionesProveedores(
                            datos
                        )


                // ======================================
                // PROCESAR RESPUESTA
                // ======================================

                if (respuesta.success) {

                    mostrarColecciones(
                        respuesta.colecciones
                            ?: emptyList()
                    )

                } else {

                    Toast.makeText(
                        this@MisColeccionesProveedoresActivity,
                        respuesta.mensaje
                            ?: "No se pudieron cargar las colecciones.",
                        Toast.LENGTH_SHORT
                    ).show()
                }


            } catch (e: Exception) {

                e.printStackTrace()

                Toast.makeText(
                    this@MisColeccionesProveedoresActivity,
                    "Error al consultar las colecciones.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    private fun mostrarColecciones(
        colecciones: List<ColeccionProveedor>
    ) {

        val adapter =
            ColeccionesProveedoresAdapter(
                colecciones
            ) { proveedor ->

                abrirDetalleProveedor(
                    proveedor
                )
            }


        binding.rvMisColeccionesProveedores.apply {

            layoutManager =
                LinearLayoutManager(
                    this@MisColeccionesProveedoresActivity,
                    LinearLayoutManager.VERTICAL,
                    false
                )

            this.adapter =
                adapter

            setHasFixedSize(false)

            isNestedScrollingEnabled =
                true

            overScrollMode =
                RecyclerView.OVER_SCROLL_NEVER
        }
    }
    private fun abrirDetalleProveedor(
        proveedor: Proveedor
    ) {

        val intent =
            Intent(
                this,
                Menu_principal_free::class.java
            )

        intent.putExtra(
            "ABRIR_DETALLE_PROVEEDOR",
            true
        )

        intent.putExtra(
            "PROVEEDOR_ID",
            proveedor.Id_Proveedor.toString()
        )

        startActivity(intent)
    }
}