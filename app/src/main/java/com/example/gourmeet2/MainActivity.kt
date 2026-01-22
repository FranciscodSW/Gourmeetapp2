package com.example.gourmeet2
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.ui.AppBarConfiguration
import com.example.gourmeet2.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configurar pantalla completa (opcional)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        // Establecer el layout
        setContentView(R.layout.activity_main)  // Asegúrate de crear este layout

        // Referencia al logo
        val logo = findViewById<ImageView>(R.id.logo)

        // Opción A: Animación simple con alpha
        logo.alpha = 0f
        logo.animate()
            .alpha(1f)
            .setDuration(1000)
            .withEndAction {
                irALogin()
            }
            .start()
    }
    private fun irALogin() {
        val intent = Intent(this, Login::class.java)
        startActivity(intent)
        // Transición suave
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        // Finalizar esta actividad
        finish()
    }


}