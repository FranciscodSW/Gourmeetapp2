package mx.com.gourmeet.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import com.google.android.material.bottomsheet.BottomSheetDialog
import androidx.appcompat.app.AppCompatActivity

class AgregarComidasADia : AppCompatActivity() {

    private lateinit var dialog: BottomSheetDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        abrirVentanaAgregarComidas()
    }

    private fun abrirVentanaAgregarComidas() {
        dialog = BottomSheetDialog(this)

        val vista = LayoutInflater.from(this).inflate(
            R.layout.activity_agregar_comidas_a_dia,
            null
        )

        dialog.setContentView(vista)

        // Botón para cerrar la ventana.
        vista.findViewById<ImageButton>(R.id.btnCerrarIngrediente)
            .setOnClickListener {
                dialog.dismiss()
                finish()
            }

        dialog.setOnDismissListener {
            finish()
        }

        dialog.show()
    }
}
