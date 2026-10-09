package mx.com.gourmeet.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import mx.com.gourmeet.app.databinding.ActivityPremiumBinding

class PremiumActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPremiumBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding =
            ActivityPremiumBinding.inflate(
                layoutInflater
            )

        setContentView(binding.root)


        // ==========================================
        // BOTÓN REGRESAR
        // ==========================================

        binding.btnRegresar.setOnClickListener {

            finish()

        }






    }
}