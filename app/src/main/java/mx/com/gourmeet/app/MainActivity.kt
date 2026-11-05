package mx.com.gourmeet.app
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import androidx.lifecycle.lifecycleScope
import com.google.firebase.FirebaseApp
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mx.com.gourmeet.app.utils.SesionUsuario

class MainActivity : AppCompatActivity() {

    private var dialogSinInternet: Dialog? = null
    private var navegando = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        setContentView(R.layout.activity_main)

        val logo = findViewById<ImageView>(R.id.logo)

        logo.alpha = 0f

        if (tieneInternet()) {
            animarLogoYDecidirDestino(logo)
        } else {
            mostrarDialogSinInternet()
        }
    }

    /**
     * Comprueba si Android tiene una conexión
     * validada para acceder a Internet.
     */
    private fun tieneInternet(): Boolean {

        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE)
                    as ConnectivityManager

        val network = connectivityManager.activeNetwork
            ?: return false

        val capabilities =
            connectivityManager.getNetworkCapabilities(network)
                ?: return false

        return capabilities.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_INTERNET
        ) && capabilities.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_VALIDATED
        )
    }

    /**
     * Muestra el diálogo cuando no hay Internet.
     */
    private fun mostrarDialogSinInternet() {

        if (isFinishing || isDestroyed) return

        if (dialogSinInternet?.isShowing == true) return

        val dialog = Dialog(this)

        dialog.setContentView(R.layout.dialog_sin_internet)

        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)

        dialog.window?.setBackgroundDrawable(
            ColorDrawable(Color.TRANSPARENT)
        )

        dialog.show()

        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        dialogSinInternet = dialog

        val btnReintentar =
            dialog.findViewById<MaterialButton>(
                R.id.btnReintentarInternet
            )

        btnReintentar.setOnClickListener {

            btnReintentar.isEnabled = false

            lifecycleScope.launch {

                delay(500)

                if (isFinishing || isDestroyed) {
                    return@launch
                }

                if (tieneInternet()) {

                    dialog.dismiss()
                    dialogSinInternet = null

                    val logo = findViewById<ImageView>(
                        R.id.logo
                    )

                    animarLogoYDecidirDestino(logo)

                } else {

                    btnReintentar.isEnabled = true
                }
            }
        }
    }

    /**
     * Anima el logo y decide a qué pantalla navegar.
     */
    private fun animarLogoYDecidirDestino(
        logo: ImageView
    ) {

        if (navegando || isFinishing || isDestroyed) {
            return
        }

        logo.animate().cancel()

        logo.alpha = 0f

        logo.animate()
            .alpha(1f)
            .setDuration(1000)
            .withEndAction {

                decidirDestino()

            }
            .start()
    }

    /**
     * Si existe una sesión guardada, abre la transición.
     * Si no existe, abre el Login.
     */
    private fun decidirDestino() {

        if (navegando || isFinishing || isDestroyed) {
            return
        }

        val sesionIniciada =
            SesionUsuario.haIniciadoSesion(this)

        Log.d(
            "SESION",
            "¿Sesión iniciada?: $sesionIniciada"
        )

        if (sesionIniciada) {

            Log.d(
                "NAVEGACION",
                "Sesión encontrada. Abriendo transición."
            )

            navegarA(
                Trancicion_de_carga_para_menu_free::class.java
            )

        } else {

            Log.d(
                "NAVEGACION",
                "No hay sesión. Abriendo Login."
            )

            navegarA(
                Login::class.java
            )
        }
    }

    /**
     * Navega una sola vez a la pantalla indicada.
     */
    private fun navegarA(destino: Class<*>) {

        if (navegando || isFinishing || isDestroyed) {
            return
        }

        navegando = true

        val intent = Intent(this, destino)

        startActivity(intent)

        overridePendingTransition(
            android.R.anim.fade_in,
            android.R.anim.fade_out
        )

        finish()
    }

    override fun onDestroy() {

        dialogSinInternet?.dismiss()
        dialogSinInternet = null

        super.onDestroy()
    }
}