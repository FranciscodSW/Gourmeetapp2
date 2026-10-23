package mx.com.gourmeet.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import mx.com.gourmeet.app.data.models.EspacioComida
import mx.com.gourmeet.app.data.models.IngredienteEspacio
import mx.com.gourmeet.app.databinding.ItemSeleccionarComidaBinding

class AdapterSeleccionarComida(
    private val espacios: List<EspacioComida>,
    private val onAgregarComida: (EspacioComida) -> Unit,
    private val onAsignarHora: (EspacioComida) -> Unit,
    private val onEliminarElemento: (EspacioComida, ElementoComida) -> Unit
) : RecyclerView.Adapter<AdapterSeleccionarComida.ComidaViewHolder>() {

    inner class ComidaViewHolder(
        val binding: ItemSeleccionarComidaBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ComidaViewHolder {
        val binding = ItemSeleccionarComidaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ComidaViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ComidaViewHolder,
        position: Int
    ) {
        val espacio = espacios[position]
        val binding = holder.binding

        // Título del espacio.
        binding.txtTitulo.text = when (espacio.TPC_ID) {
            1 -> "Desayuno"
            2 -> "Comida"
            3 -> "Cena"
            4 -> "Colación ${espacio.ECM_NUMERO_COLACION}"
            else -> "Comida"
        }

        // Preparar la lista interna.
        binding.listadecomidas.layoutManager =
            LinearLayoutManager(binding.root.context)

        // Conservar el ID de cada receta o ingrediente.
        val contenido: List<ElementoComida> = when {
            espacio.recetas.isNotEmpty() -> {
                espacio.recetas.map { receta ->
                    ElementoComida(
                        nombre = receta.REC_NOMBRE,
                        ecrId = receta.ECR_ID
                    )
                }
            }

            espacio.ingredientes.isNotEmpty() -> {
                espacio.ingredientes.map { ingrediente ->
                    ElementoComida(
                        nombre = construirTextoIngrediente(ingrediente),
                        eciId = ingrediente.ECI_ID
                    )
                }
            }

            else -> emptyList()
        }

        // La X envía el espacio y el elemento seleccionado
        // al callback de la pantalla que administra las API.
        binding.listadecomidas.adapter = AdapterNombreComida(
            elementos = contenido.toMutableList(),
            onEliminar = { elemento ->
                onEliminarElemento(espacio, elemento)
            }
        )

        // En colaciones: máximo una receta o dos ingredientes.
        val esColacion = espacio.TPC_ID == 4

        val puedeAgregar = !esColacion ||
                (
                        espacio.recetas.isEmpty() &&
                                espacio.ingredientes.size < 2
                        )

        binding.btnAgregarComida.visibility =
            if (puedeAgregar) View.VISIBLE else View.GONE

        // Conservar el botón de asignar hora.
        actualizarTextoHora(
            boton = binding.btnAsignarhora,
            espacio = espacio
        )

        binding.btnAgregarComida.setOnClickListener {
            val posicionActual = holder.bindingAdapterPosition

            if (posicionActual != RecyclerView.NO_POSITION) {
                onAgregarComida(espacios[posicionActual])
            }
        }

        binding.btnAsignarhora.setOnClickListener {
            val posicionActual = holder.bindingAdapterPosition

            if (posicionActual != RecyclerView.NO_POSITION) {
                onAsignarHora(espacios[posicionActual])
            }
        }
    }

    private fun construirTextoIngrediente(
        ingrediente: IngredienteEspacio
    ): String {
        val cantidad = ingrediente.ECI_CANTIDAD?.let { valor ->
            if (valor % 1.0 == 0.0) {
                valor.toInt().toString()
            } else {
                valor.toString()
            }
        }

        return buildString {
            append(ingrediente.ING_DESCRIPCION)

            if (cantidad != null) {
                append(" · ")
                append(cantidad)
            }

            if (ingrediente.ECI_UNIDAD.isNotBlank()) {
                append(" ")
                append(ingrediente.ECI_UNIDAD)
            }
        }
    }

    private fun actualizarTextoHora(
        boton: Button,
        espacio: EspacioComida
    ) {
        val hora = espacio.ECM_HORA
            ?.take(5)
            ?.takeIf {
                it.matches(Regex("\\d{2}:\\d{2}"))
            }

        boton.text = if (hora != null) {
            "Hora: $hora"
        } else {
            "Asignar hora"
        }
    }

    override fun getItemCount(): Int = espacios.size
}


