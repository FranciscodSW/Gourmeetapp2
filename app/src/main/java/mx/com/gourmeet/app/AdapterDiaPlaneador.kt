package mx.com.gourmeet.app

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import mx.com.gourmeet.app.data.api.ApiClient
import mx.com.gourmeet.app.data.models.ActualizarHoraEspacioRequest
import mx.com.gourmeet.app.data.models.DiaPlaneador
import mx.com.gourmeet.app.data.models.EspacioComida
import mx.com.gourmeet.app.databinding.ItemDiaDeLaSemanaPlaneadorBinding


class AdapterDiaPlaneador(
    private val dias: MutableList<DiaPlaneador>,
    private val onSeleccionarDia: (DiaPlaneador, Boolean) -> Unit,
    private val onAgregarComida: (DiaPlaneador, Int) -> Unit,
    private val onAgregarColacion: (DiaPlaneador, Int) -> Unit,
    private val onAsignarHora: (EspacioComida) -> Unit = {},
    private val onEliminarElemento: (EspacioComida, ElementoComida) -> Unit,
    private val onAsignarCocinero: (DiaPlaneador) -> Unit = {}
) : RecyclerView.Adapter<AdapterDiaPlaneador.DiaViewHolder>() {


    inner class DiaViewHolder(
        val binding: ItemDiaDeLaSemanaPlaneadorBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DiaViewHolder {

        val binding = ItemDiaDeLaSemanaPlaneadorBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return DiaViewHolder(binding)
    }


    override fun onBindViewHolder(
        holder: DiaViewHolder,
        position: Int
    ) {
        val dia = dias[position]
        val binding = holder.binding
        val context = binding.root.context

        // Nombre del día.
        binding.diaDeLaSemana.text = dia.nombreDia
// Mostrar el nombre del cocinero asignado o el texto predeterminado.
        binding.btnAsignarCocinero.text = dia.nombreCocinero
            ?.takeIf { it.isNotBlank() }
            ?.let { "$it" }
            ?: "Asignar cocinero"

// Asignar o consultar el cocinero del día seleccionado.
        binding.btnAsignarCocinero.setOnClickListener {
            val posicionActual = holder.bindingAdapterPosition

            if (posicionActual != RecyclerView.NO_POSITION) {
                val diaActual = dias[posicionActual]

                if (diaActual.seleccionado) {
                    onAsignarCocinero(diaActual)
                } else {
                    Toast.makeText(
                        context,
                        "Selecciona primero el día.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        // Mantener siempre el icono del check blanco.
        binding.seleccionarDia.setImageResource(R.drawable.ic_check_white)

        // Azul = seleccionado | Gris = deseleccionado.
        val colorFondo = if (dia.seleccionado) {
            ContextCompat.getColor(context, R.color.azulgourmeet)
        } else {
            Color.parseColor("#BDBDBD")
        }
        binding.seleccionarDia.backgroundTintList =
            ColorStateList.valueOf(colorFondo)
        // Mostrar u ocultar el contenido del día.
        binding.contenidoDia.visibility =
            if (dia.seleccionado) View.VISIBLE else View.GONE

        // Seleccionar o deseleccionar al tocar cualquier parte del encabezado.
        binding.EncabezadoDia.setOnClickListener {
            val posicionActual = holder.bindingAdapterPosition

            if (posicionActual != RecyclerView.NO_POSITION) {
                val diaActual = dias[posicionActual]
                val estadoAnterior = diaActual.seleccionado

                diaActual.seleccionado = !estadoAnterior
                notifyItemChanged(posicionActual)

                onSeleccionarDia(diaActual, estadoAnterior)
            }
        }
        // Estado visual del contenido
        binding.contenidoDia.visibility =
            if (dia.expandido && dia.seleccionado) {
                View.VISIBLE
            } else {
                View.GONE
            }
        binding.contenidoDia.visibility =
            if (dia.expandido && dia.seleccionado) {
                View.VISIBLE
            } else {
                View.GONE
            }

        binding.btnContraerDia.rotation =
            if (dia.expandido) 0f else 180f

        binding.btnContraerDia.setOnClickListener {
            val posicion = holder.bindingAdapterPosition

            if (posicion != RecyclerView.NO_POSITION) {
                val diaActual = dias[posicion]

                diaActual.expandido = !diaActual.expandido

                notifyItemChanged(posicion)
            }
        }

// Orientación de la flecha
        binding.btnContraerDia.rotation =
            if (dia.expandido) 0f else 180f

// La flecha solo contrae o expande
        binding.btnContraerDia.setOnClickListener {
            val posicion = holder.bindingAdapterPosition

            if (posicion != RecyclerView.NO_POSITION) {
                val diaActual = dias[posicion]

                diaActual.expandido = !diaActual.expandido

                notifyItemChanged(posicion)
            }
        }

        // RecyclerView de desayuno, comida y cena.
        binding.contenedores.layoutManager = LinearLayoutManager(
            context,
            LinearLayoutManager.HORIZONTAL,
            false
        )

        val comidasPrincipales = dia.espacios
            .filter { it.TPC_ID in 1..3 }
            .sortedBy { it.TPC_ID }

        binding.contenedores.adapter = AdapterSeleccionarComida(
            espacios = comidasPrincipales,
            onAgregarComida = { espacio ->
                if (dia.seleccionado) {
                    onAgregarComida(dia, espacio.TPC_ID)
                }
            },
            onAsignarHora = { espacio ->
                if (dia.seleccionado) {
                    onAsignarHora(espacio)
                }
            },
            onEliminarElemento = { espacio, elemento ->
                if (dia.seleccionado) {
                    onEliminarElemento(espacio, elemento)
                }
            }
        )

        // RecyclerView de colaciones en formato horizontal.
        binding.contenedoresColaciones.layoutManager =
            LinearLayoutManager(
                context,
                LinearLayoutManager.HORIZONTAL,
                false
            )

        val colaciones = dia.espacios
            .filter { it.TPC_ID == 4 }
            .sortedBy { it.ECM_NUMERO_COLACION }

        binding.contenedoresColaciones.adapter =
            AdapterSeleccionarComida(
                espacios = colaciones,
                onAgregarComida = { espacio ->
                    if (dia.seleccionado) {
                        onAgregarComida(dia, espacio.TPC_ID)
                    }
                },
                onAsignarHora = { espacio ->
                    if (dia.seleccionado) {
                        onAsignarHora(espacio)
                    }
                },
                onEliminarElemento = { espacio, elemento ->
                    if (dia.seleccionado) {
                        onEliminarElemento(espacio, elemento)
                    }
                }
            )

        // Habilitar o deshabilitar las listas y botones.
        binding.contenedores.isEnabled = dia.seleccionado
        binding.contenedoresColaciones.isEnabled = dia.seleccionado

    }


    override fun getItemCount(): Int = dias.size

    fun actualizarDias(nuevosDias: List<DiaPlaneador>) {
        dias.clear()
        dias.addAll(nuevosDias)
        notifyDataSetChanged()
    }
    // Obtener los días actuales del planeador
    fun obtenerDias(): List<DiaPlaneador> {
        return dias.toList()
    }


}