package com.example.gourmeet2.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.gourmeet2.R
import com.example.gourmeet2.data.api.ApiClient
import com.example.gourmeet2.data.models.ReaccionRespuestaRequest
import com.example.gourmeet2.data.models.RespuestaComentario
import com.example.gourmeet2.databinding.ItemRespuestaBinding
import com.google.android.gms.cast.framework.SessionManager

class RespuestaAdapter(

    private val respuestas: List<RespuestaComentario>,
    private val listener: OnComentarioClickListener
) : RecyclerView.Adapter<RespuestaAdapter.RespuestaViewHolder>() {
    private var respuestaEditandoId = -1

    inner class RespuestaViewHolder(
        val binding: ItemRespuestaBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RespuestaViewHolder {

        val binding = ItemRespuestaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return RespuestaViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: RespuestaViewHolder,
        position: Int
    ) {

        val respuesta = respuestas[position]
        val estaEditando = respuesta.id == respuestaEditandoId

        //----------------------------------------
        // Usuario
        //----------------------------------------

        holder.binding.txtNombreRespuesta.text =
            "${respuesta.usuario.nombre} respondió:"

        holder.binding.txtComentarioRespuesta.text =
            respuesta.comentario
        holder.binding.txtComentarioRespuesta.visibility =
            if (estaEditando) View.GONE else View.VISIBLE

        holder.binding.layoutEditarComentarioRespuesta.visibility =
            if (estaEditando) View.VISIBLE else View.GONE

        if (estaEditando) {

            holder.binding.edtEditarRespuesta.setText(
                respuesta.comentario
            )

        }

        //----------------------------------------
        // Imagen
        //----------------------------------------

        if (!respuesta.usuario.foto.isNullOrEmpty()) {

            Glide.with(holder.itemView.context)
                .load(respuesta.usuario.foto)
                .placeholder(R.drawable.ic_user)
                .error(R.drawable.ic_user)
                .circleCrop()
                .into(holder.binding.imgUsuarioRespuesta)

        } else {

            holder.binding.imgUsuarioRespuesta
                .setImageResource(R.drawable.ic_user)

        }

        //----------------------------------------
        // Mostrar acciones solo si es del usuario
        //----------------------------------------

        holder.binding.layoutEditarRespuesta.visibility =
            if (respuesta.esMia)
                View.VISIBLE
            else
                View.GONE

        //----------------------------------------
        // Estado de botones
        //----------------------------------------

        actualizarEstadoBotones(holder, respuesta)

        //----------------------------------------
        // Eventos
        //----------------------------------------

        holder.binding.layoutLikeRespuesta.setOnClickListener {

            listener.onLikeRespuesta(respuesta)

        }

        holder.binding.layoutDislikeRespuesta.setOnClickListener {

            listener.onDislikeRespuesta(respuesta)

        }

        holder.binding.layoutReportarRespuesta.setOnClickListener {

            listener.onReportarRespuesta(respuesta)

        }

        holder.binding.btnEditarRespuesta.setOnClickListener {

            editarRespuesta(respuesta.id)

            listener.onEditarRespuesta(respuesta)

        }

        holder.binding.btnEliminarRespuesta.setOnClickListener {

            listener.onEliminarRespuesta(respuesta)

        }
        holder.binding.btnConfirmarRespuesta.setOnClickListener {

            val texto = holder.binding.edtEditarRespuesta.text
                .toString()
                .trim()

            if (texto.isEmpty()) {

                holder.binding.edtEditarRespuesta.error =
                    "Escribe una respuesta"

                return@setOnClickListener

            }

            listener.onActualizarRespuesta(
                respuesta,
                texto
            )

        }

    }

    override fun getItemCount(): Int {

        return respuestas.size

    }

    private fun actualizarEstadoBotones(
        holder: RespuestaViewHolder,
        respuesta: RespuestaComentario
    ) {

        holder.binding.imgLikeRespuesta.setImageResource(R.drawable.ic_like)
        holder.binding.imgDislikeRespuesta.setImageResource(R.drawable.ic_dislike)
        holder.binding.imgReportarRespuesta.setImageResource(R.drawable.ic_reportar)

        when (respuesta.miReaccion) {

            "LIKE" -> {

                holder.binding.imgLikeRespuesta.setImageResource(
                    R.drawable.ic_like_on
                )

            }

            "DISLIKE" -> {

                holder.binding.imgDislikeRespuesta.setImageResource(
                    R.drawable.ic_dislike_on
                )

            }

            "REPORTAR" -> {

                holder.binding.imgReportarRespuesta.setImageResource(
                    R.drawable.ic_reportar_on
                )

            }

        }

    }
    fun editarRespuesta(
        respuestaId: Int
    ) {

        val anterior = respuestaEditandoId

        respuestaEditandoId = respuestaId

        if (anterior != -1) {

            notifyDataSetChanged()

        }

        notifyDataSetChanged()

    }

}