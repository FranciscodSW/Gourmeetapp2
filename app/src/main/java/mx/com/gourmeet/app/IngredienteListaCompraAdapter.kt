
package mx.com.gourmeet.app

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import mx.com.gourmeet.app.data.models.*
import mx.com.gourmeet.app.databinding.ItemIngredienteListaCompraBinding
import java.util.Locale

class IngredienteListaCompraAdapter(
    private val ingredientes: MutableList<IngredienteListaCompra>,
    private val onEstadoCambiado: (IngredienteListaCompra, Int) -> Unit
) : RecyclerView.Adapter<IngredienteListaCompraAdapter.IngredienteViewHolder>() {

    inner class IngredienteViewHolder(
        val binding: ItemIngredienteListaCompraBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): IngredienteViewHolder {

        val binding = ItemIngredienteListaCompraBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return IngredienteViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: IngredienteViewHolder,
        position: Int
    ) {

        val ingrediente = ingredientes[position]

        with(holder.binding) {

            txtNombreIngrediente.text = ingrediente.descripcion
            txtCantidadCirculo.text = obtenerCantidad(ingrediente)

            val comprado = ingrediente.comprado == 1

            txtCantidadCirculo.isSelected = comprado

            txtCantidadCirculo.setTextColor(
                ContextCompat.getColor(
                    holder.itemView.context,
                    if (comprado) android.R.color.white
                    else R.color.azulgourmeet
                )
            )

            if (comprado) {
                txtNombreIngrediente.paintFlags =
                    txtNombreIngrediente.paintFlags or
                            Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                txtNombreIngrediente.paintFlags =
                    txtNombreIngrediente.paintFlags and
                            Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            txtCantidadCirculo.setOnClickListener {

                val nuevoEstado =
                    if (ingrediente.comprado == 1) 0 else 1

                onEstadoCambiado(
                    ingrediente,
                    nuevoEstado
                )
            }
        }
    }

    private fun obtenerCantidad(
        ingrediente: IngredienteListaCompra
    ): String {

        val unidad = ingrediente.unidad?.trim().orEmpty()

        if (unidad.equals("Al gusto", ignoreCase = true)) {
            return "Al gusto"
        }

        if (unidad.equals("Por revisar", ignoreCase = true)) {
            return "Por revisar"
        }

        val cantidad = ingrediente.cantidad
            ?: return unidad.ifEmpty { "Por revisar" }

        val cantidadTexto = String.format(
            Locale.getDefault(),
            "%.3f",
            cantidad
        ).trimEnd('0').trimEnd(',', '.')

        return listOf(cantidadTexto, unidad)
            .filter { it.isNotBlank() }
            .joinToString(" ")
    }

    override fun getItemCount(): Int = ingredientes.size

    fun actualizarLista(nuevaLista: List<IngredienteListaCompra>) {
        ingredientes.clear()
        ingredientes.addAll(nuevaLista)
        notifyDataSetChanged()
    }
}

