package com.example.gourmeet2

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class DetalleRecetaActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_detalle_receta)

        if (savedInstanceState == null) {

            val recetaId = intent.getIntExtra("REC_ID", 0)

            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.contenedorDetalle,
                    DetalleRecetaFragment.newInstance(recetaId)
                )
                .commit()
        }
    }
}