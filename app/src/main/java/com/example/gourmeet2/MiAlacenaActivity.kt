package com.example.gourmeet2

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.gourmeet2.databinding.ActivityMiAlacenaBinding

class MiAlacenaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMiAlacenaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMiAlacenaBinding.inflate(layoutInflater)

        setContentView(binding.root)

        // ==========================================
        // BOTÓN REGRESAR
        // ==========================================

        binding.btnRegresarAlacena.setOnClickListener {

            finish()

        }
    }
}