package com.example.gourmeet2
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import android.widget.Toast.makeText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gourmeet2.data.api.ApiClient
import com.example.gourmeet2.data.models.ColeccionConRecetas
import com.example.gourmeet2.data.models.ListarColeccionesRecetasRequest
import com.example.gourmeet2.databinding.ActivityMisColeccionesBinding
import com.example.gourmeet2.utils.SesionUsuario
import kotlinx.coroutines.launch

class MisColeccionesActivity :
    AppCompatActivity() {

    private lateinit var binding:
            ActivityMisColeccionesBinding

    private lateinit var adapter:
            ColeccionesAdapter

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        binding =
            ActivityMisColeccionesBinding.inflate(layoutInflater)

        setContentView(binding.root)

        inicializarRecycler()

        cargarColecciones()

        eventos()

    }


    private fun inicializarRecycler() {

        adapter = ColeccionesAdapter(

            mutableListOf(),

            object : ColeccionesAdapter.OnColeccionListener {

                override fun onEditar(
                    coleccion: ColeccionConRecetas
                ) {

                    makeText(

                        this@MisColeccionesActivity,

                        "Editar ${coleccion.COL_NOMBRE}",

                        Toast.LENGTH_SHORT

                    ).show()

                }

                override fun onEliminar(
                    coleccion: ColeccionConRecetas
                ) {

                    makeText(

                        /* context = */ this@MisColeccionesActivity,

                        /* text = */ "Eliminar ${coleccion.COL_NOMBRE}",

                        /* duration = */ Toast.LENGTH_SHORT

                    ).show()

                }

                override fun onRecetaClick(
                    recetaId: Int
                ) {

                    val intent = Intent(
                        this@MisColeccionesActivity,
                        DetalleRecetaActivity::class.java
                    )

                    intent.putExtra(
                        "REC_ID",
                        recetaId
                    )

                    startActivity(intent)

                }

            }

        )

        binding.recyclerColecciones.apply {

            layoutManager = LinearLayoutManager(

                this@MisColeccionesActivity

            )

            adapter = this@MisColeccionesActivity.adapter

        }


    }

    private fun cargarColecciones() {

        lifecycleScope.launch {

            try {

                val response = ApiClient.apiService
                    .listarColeccionesConRecetas(

                        ListarColeccionesRecetasRequest(

                            SesionUsuario.obtenerId(
                                this@MisColeccionesActivity
                            )

                        )

                    )

                if (response.success) {

                    adapter.actualizar(

                        response.colecciones

                    )

                }

            } catch (e: Exception) {

                Toast.makeText(

                    this@MisColeccionesActivity,

                    "No fue posible cargar las colecciones",

                    Toast.LENGTH_SHORT

                ).show()

            }

        }

    }

    private fun eventos() {

        binding.layoutNuevaColeccion.setOnClickListener {

            Toast.makeText(

                this,

                "Nueva colección",

                Toast.LENGTH_SHORT

            ).show()

        }

    }
}