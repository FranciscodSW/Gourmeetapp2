package mx.com.gourmeet.app


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import mx.com.gourmeet.app.data.models.IntegranteHogarBusqueda
import  mx.com.gourmeet.app.databinding.ItemUsuarioBusquedaIntegranteHogarBinding


class IntegrantesHogarBusquedaAdapter(
    private val onSeleccionar: (IntegranteHogarBusqueda) -> Unit
) : RecyclerView.Adapter<IntegrantesHogarBusquedaAdapter.IntegranteViewHolder>() {

    private val integrantes = mutableListOf<IntegranteHogarBusqueda>()

    inner class IntegranteViewHolder(
        val binding: ItemUsuarioBusquedaIntegranteHogarBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): IntegranteViewHolder {

        val binding = ItemUsuarioBusquedaIntegranteHogarBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return IntegranteViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: IntegranteViewHolder,
        position: Int
    ) {
        val integrante = integrantes[position]
        val binding = holder.binding

        binding.txtNombreUsuario.text = integrante.HOG_USU_NOMBRE
        binding.txtCorreoUsuario.text =
            integrante.HOG_USU_CORREO.orEmpty().ifBlank {
                "Sin correo registrado"
            }

        // El XML ya tiene una imagen de usuario predeterminada.
        binding.imgFotoUsuario.setImageResource(
            R.drawable.ic_user
        )

        binding.root.setOnClickListener {
            onSeleccionar(integrante)
        }

        binding.seleccionarUsuario.setOnClickListener {
            onSeleccionar(integrante)
        }
    }

    override fun getItemCount(): Int = integrantes.size

    fun actualizarLista(nuevaLista: List<IntegranteHogarBusqueda>) {
        integrantes.clear()
        integrantes.addAll(nuevaLista)
        notifyDataSetChanged()
    }

    fun limpiarLista() {
        integrantes.clear()
        notifyDataSetChanged()
    }
}