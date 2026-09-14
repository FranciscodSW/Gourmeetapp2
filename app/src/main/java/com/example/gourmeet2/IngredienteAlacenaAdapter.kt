package com.example.gourmeet2

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.gourmeet2.data.models.IngredienteAlacena
import java.text.SimpleDateFormat
import java.util.Locale

class IngredienteAlacenaAdapter(
    private var lista: List<IngredienteAlacena>,
    private val onIngredienteClick: (IngredienteAlacena) -> Unit
) : RecyclerView.Adapter<IngredienteAlacenaAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        // Imagen del ingrediente
        val imagen: ImageView =
            itemView.findViewById(R.id.EmojiIngrediente)

        // Imagen que representa el estado
        val estado: ImageView =
            itemView.findViewById(R.id.imgEstadoIngrediente)

        // Nombre
        val nombre: TextView =
            itemView.findViewById(R.id.txtNombreIngrediente)

        // Cantidad
        val cantidad: TextView =
            itemView.findViewById(R.id.txtCantidadIngrediente)

        // Fecha de vencimiento
        val fecha: TextView =
            itemView.findViewById(R.id.txtFechadecompra)
        val fechaConsumo: LinearLayout =
            itemView.findViewById(R.id.fechaconsumo)

        val proximoACaducar: LinearLayout =
            itemView.findViewById(R.id.proximo_a_caducar)

        val revisar: LinearLayout =
            itemView.findViewById(R.id.revisar)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.item_ingrediente_alacena,
                parent,
                false

            )


        return ViewHolder(view)

    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        val ingrediente = lista[position]

        Log.d(
            "ALACENA_ADAPTER",
            "Mostrando: ${ingrediente.ING_DESCRIPCION}"
        )

        // ==========================================
        // NOMBRE DEL INGREDIENTE
        // ==========================================

        holder.nombre.text =
            ingrediente.ING_DESCRIPCION.trim()


        // ==========================================
        // CANTIDAD
        // ==========================================

        val cantidad = ingrediente.AI_CANTIDAD

        val cantidadTexto =
            if (cantidad % 1.0 == 0.0) {
                cantidad.toInt().toString()
            } else {
                cantidad.toString()
            }
        holder.cantidad.text =
            "$cantidadTexto ${ingrediente.AI_UNIDAD}"

        // ==========================================
        // FECHA DE VENCIMIENTO
        // ==========================================

        holder.fecha.text =
            formatearFechaDiaMes(
                ingrediente.AI_FECHA_VENCIMIENTO
            )


        // ==========================================
        // ESTADO DEL INGREDIENTE
        // ==========================================

        val estadoIngrediente =
            ingrediente.AI_ESTADO
                ?.trim()
                ?.lowercase()
        // Ocultar todas las vistas inferiores
// antes de mostrar la correspondiente.
// Esto es necesario porque RecyclerView reutiliza las tarjetas.
        holder.fechaConsumo.visibility = View.GONE
        holder.proximoACaducar.visibility = View.GONE
        holder.revisar.visibility = View.GONE

// Mostrar la vista correspondiente al estado
        when (estadoIngrediente) {
            "no perecedero",
            "verde",
            "fresco",
            "maduro",
            "sellado" -> {
                holder.fechaConsumo.visibility = View.VISIBLE
            }

            "pasado" -> {
                holder.proximoACaducar.visibility = View.VISIBLE
            }

            "descompuesto" -> {
                holder.revisar.visibility = View.VISIBLE
            }
        }

        val imagenEstado = when (estadoIngrediente) {
            "no perecedero"->
                R.drawable.ic_no_perecedero

            "verde"->
                R.drawable.ic_verde

            "fresco" ->
                R.drawable.ic_fresco

            "maduro" ->
                R.drawable.ic_maduro

            "pasado" ->
                R.drawable.ic_pasado

            "descompuesto" ->
                R.drawable.ic_descompuesto

            "sellado" ->
                R.drawable.ic_sellado

            else -> {

                Log.w(
                    "ALACENA_ADAPTER",
                    "Estado desconocido: ${ingrediente.AI_ESTADO}"
                )

                R.drawable.ic_fresco
            }
        }

        holder.estado.setImageResource(imagenEstado)


        // ==========================================
        // IMAGEN DEL INGREDIENTE
        // ==========================================

        if (!ingrediente.Foto_Ingrediente.isNullOrEmpty()) {

            Glide.with(holder.itemView.context)
                .load(ingrediente.Foto_Ingrediente)
                .placeholder(R.drawable.ic_ingredientes)
                .error(R.drawable.ic_ingredientes)
                .into(holder.imagen)

        } else {

            holder.imagen.setImageResource(
                R.drawable.ic_ingredientes
            )
        }
        holder.itemView.setOnClickListener {
            onIngredienteClick(ingrediente)
        }
    }

    // ==========================================
    // CANTIDAD DE ELEMENTOS
    // ==========================================

    override fun getItemCount(): Int {
        return lista.size
    }

    // ==========================================
    // ACTUALIZAR LISTA
    // ==========================================
    private fun formatearFechaDiaMes(fecha: String?): String {

        if (fecha.isNullOrBlank()) {
            return "Sin vencimiento"
        }

        return try {

            // Formato que viene de MySQL/API
            val formatoEntrada =
                SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.getDefault()
                )

            // Formato que queremos mostrar
            val formatoSalida =
                SimpleDateFormat(
                    "dd/MM",
                    Locale.getDefault()
                )

            val fechaConvertida =
                formatoEntrada.parse(fecha)

            if (fechaConvertida != null) {
                formatoSalida.format(fechaConvertida)
            } else {
                "Sin vencimiento"
            }

        } catch (e: Exception) {

            Log.e(
                "ALACENA_ADAPTER",
                "Error formateando fecha: $fecha",
                e
            )

            "Sin vencimiento"
        }
    }

    fun actualizarLista(
        nuevaLista: List<IngredienteAlacena>
    ) {

        lista = nuevaLista

        Log.d(
            "ALACENA_ADAPTER",
            "Lista actualizada: ${lista.size}"
        )

        notifyDataSetChanged()
    }
}