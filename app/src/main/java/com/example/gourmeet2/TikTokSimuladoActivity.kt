package com.example.gourmeet2

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class TikTokSimuladoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ejemplotictock)

        findViewById<Button>(R.id.btnContinue).setOnClickListener {
            val data = Intent()
            data.putExtra("nombre", "Ejemplo")
            data.putExtra("correo", "ejemplo@gmail.com")

            setResult(Activity.RESULT_OK, data)
            finish()
        }

        findViewById<Button>(R.id.btnCancel).setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }
}