package com.example.gourmeet2.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.gourmeet2.R
import com.example.gourmeet2.databinding.ItemComentarioBinding
import com.example.gourmeet2.data.models.*

interface OnComentarioClickListener {
    fun onActualizarRespuesta(
        respuesta: RespuestaComentario,
        nuevoTexto: String
    )
    fun onLike(comentario: Comentarios)

    fun onDislike(comentario: Comentarios)

    fun onReportar(comentario: Comentarios)

    fun onResponder(comentario: Comentarios)
    fun onEnviarRespuesta(
        comentario: Comentarios,
        respuesta: String
    )
    fun onEliminarRespuesta(
        respuesta: RespuestaComentario
    )
    fun onEditarComentario(comentario: Comentarios)

    fun onEliminarComentario(comentario: Comentarios)
    fun onActualizarComentario(
        comentario: Comentarios,
        nuevoComentario: String,
        nuevaCalificacion: Float
    )
    fun onLikeRespuesta(
        respuesta: RespuestaComentario
    )

    fun onDislikeRespuesta(
        respuesta: RespuestaComentario
    )

    fun onReportarRespuesta(
        respuesta: RespuestaComentario
    )
    fun onEditarRespuesta(
        respuesta: RespuestaComentario
    )





}

class ComentarioAdapter(
    private val comentarios: MutableList<Comentarios>,
    private val listener: OnComentarioClickListener
) : RecyclerView.Adapter<ComentarioAdapter.ComentarioViewHolder>() {
    private var comentarioAbierto = -1
    private var comentarioEditando = -1
    private var respuestaEditando: RespuestaComentario? = null
    private var modoRespuesta = "CREAR"
    inner class ComentarioViewHolder(
        val binding: ItemComentarioBinding
    ) : RecyclerView.ViewHolder(binding.root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComentarioViewHolder {

        val binding = ItemComentarioBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return ComentarioViewHolder(binding)
    }
    override fun onBindViewHolder(holder: ComentarioViewHolder, position: Int) {

        val comentario = comentarios[position]

        //----------------------------------------------------
        // Información del comentario
        //----------------------------------------------------

        holder.binding.txtNombre.text =
            "${comentario.usuario.nombre} dijo:"

        holder.binding.txtComentario.text = comentario.comentario

        val estaEditando = comentario.id == comentarioEditando

        holder.binding.ratingComentario.rating = comentario.calificacion

        holder.binding.txtComentario.visibility =
            if (estaEditando) View.GONE else View.VISIBLE

        holder.binding.layoutEditarComentario.visibility =
            if (estaEditando) View.VISIBLE else View.GONE

        holder.binding.ratingComentario.setIsIndicator(
            !estaEditando
        )
        holder.binding.layoutAccionesComentario.visibility =
            if (comentario.esMio &&
                comentario.estatus == 1 &&
                !estaEditando)
                View.VISIBLE
            else
                View.GONE

        holder.binding.btnResponder.visibility =
            if (estaEditando) View.GONE else View.VISIBLE

        holder.binding.btnLike.visibility =
            if (estaEditando) View.GONE else View.VISIBLE

        holder.binding.btnDislike.visibility =
            if (estaEditando) View.GONE else View.VISIBLE

        holder.binding.btnReportar.visibility =
            if (estaEditando) View.GONE else View.VISIBLE

        if (estaEditando) {

            holder.binding.edtEditarComentario.setText(
                comentario.comentario
            )

        }
        holder.binding.btnConfirmarComentario.setOnClickListener {

            val nuevoComentario = holder.binding.edtEditarComentario.text
                .toString()
                .trim()

            val nuevaCalificacion =
                holder.binding.ratingComentario.rating

            if (nuevoComentario.isEmpty()) {
                holder.binding.edtEditarComentario.error =
                    "Escribe un comentario"
                return@setOnClickListener
            }

            listener.onActualizarComentario(
                comentario,
                nuevoComentario,
                nuevaCalificacion
            )

        }
        //----------------------------------------------------
        // Foto
        //----------------------------------------------------

        if (!comentario.usuario.foto.isNullOrEmpty()) {

            Glide.with(holder.itemView.context)
                .load(comentario.usuario.foto)
                .placeholder(R.drawable.ic_user)
                .error(R.drawable.ic_user)
                .circleCrop()
                .into(holder.binding.imgUsuario)

        } else {

            holder.binding.imgUsuario.setImageResource(R.drawable.ic_user)

        }
        Log.d(
            "COMENTARIO",
            "id=${comentario.id} esMio=${comentario.esMio} estatus=${comentario.estatus}"
        )
        holder.binding.layoutAccionesComentario.visibility =
            if (comentario.esMio && comentario.estatus == 1)
                View.VISIBLE
            else
                View.GONE
        holder.binding.ratingComentario.visibility =
            if (comentario.estatus == 1)
                View.VISIBLE
            else
                View.GONE

        //----------------------------------------------------
        // Estado de los botones
        //----------------------------------------------------

        actualizarEstadoBotones(holder, comentario)

        //----------------------------------------------------
        // Eventos
        //----------------------------------------------------

        holder.binding.btnLike.setOnClickListener {

            listener.onLike(comentario)

        }

        holder.binding.btnDislike.setOnClickListener {

            listener.onDislike(comentario)

        }

        holder.binding.btnReportar.setOnClickListener {

            listener.onReportar(comentario)

        }

        holder.binding.btnResponder.setOnClickListener {

            mostrarCajaRespuesta(comentario.id)

            listener.onResponder(comentario)

        }
        holder.binding.btnEditarComentario.setOnClickListener {

            listener.onEditarComentario(comentario)

        }

        holder.binding.btnEliminarComentario.setOnClickListener {

            listener.onEliminarComentario(comentario)

        }

        holder.binding.btnEnviarRespuesta.setOnClickListener {

            val texto = holder.binding.edtRespuesta.text.toString().trim()
            if (texto.isNotEmpty()) {

                listener.onEnviarRespuesta(
                    comentario,
                    texto
                )

            }
        }/*
        holder.binding.btnBorrarRespuesta.setOnClickListener {

            respuestaEditando?.let {

                listener.onEliminarRespuesta(it)

            }

        }*/
        //----------------------------------------------------
        // Respuestas
        //----------------------------------------------------

        if (comentario.respuestas.isNotEmpty()) {

            holder.binding.rvRespuestas.visibility = View.VISIBLE

            holder.binding.rvRespuestas.layoutManager =
                LinearLayoutManager(holder.itemView.context)

            holder.binding.rvRespuestas.adapter =
                RespuestaAdapter(
                    comentario.respuestas,
                    listener
                )

        } else {

            holder.binding.rvRespuestas.visibility = View.GONE

        }
        if (comentario.id == comentarioAbierto) {

            holder.binding.layoutContenedorRespuesta.visibility = View.VISIBLE

           holder.binding.txtResponderA.text ="Respondiendo a ${comentario.usuario.nombre}"

            if (modoRespuesta == "EDITAR" && respuestaEditando != null) {

                holder.binding.edtRespuesta.setText(
                    respuestaEditando!!.comentario
                )
               // holder.binding.btnBorrarRespuesta.visibility = View.VISIBLE

            } else {

                holder.binding.edtRespuesta.setText("")
                //holder.binding.btnBorrarRespuesta.visibility = View.GONE

            }

        } else {

            holder.binding.layoutContenedorRespuesta.visibility = View.GONE


        }

    }



    override fun getItemCount(): Int {

        return comentarios.size

    }
    private fun actualizarEstadoBotones(
        holder: ComentarioViewHolder,
        comentario: Comentarios
    ) {

        holder.binding.btnLike.setImageResource(R.drawable.ic_like)
        holder.binding.btnDislike.setImageResource(R.drawable.ic_dislike)
        holder.binding.btnReportar.setImageResource(R.drawable.ic_reportar)

        when (comentario.miReaccion) {
            "LIKE" -> {
                holder.binding.btnLike.setImageResource(
                    R.drawable.ic_like_on
                )
            }
            "DISLIKE" -> {
                holder.binding.btnDislike.setImageResource(
                    R.drawable.ic_dislike_on
                )
            }
            "REPORTAR" -> {
                holder.binding.btnReportar.setImageResource(
                    R.drawable.ic_reportar_on
                )
            }
        }
    }
    fun cerrarCajaRespuesta() {

        val anterior = comentarioAbierto

        comentarioAbierto = -1

        if (anterior != -1) {

            val posicion = comentarios.indexOfFirst {
                it.id == anterior
            }

            if (posicion != -1) {
                notifyItemChanged(posicion)
            }
        }
    }

    fun actualizarLista(lista: List<Comentarios>) {

        comentarios.clear()

        comentarios.addAll(lista)

        notifyDataSetChanged()
    }
    fun actualizarReaccion(

        comentarioId: Int,

        likes: Int,

        dislikes: Int,

        reportes: Int,

        miReaccion: String?

    ) {

        val posicion = comentarios.indexOfFirst {

            it.id == comentarioId

        }

        if (posicion != -1) {

            comentarios[posicion].likes = likes
            comentarios[posicion].dislikes = dislikes
            comentarios[posicion].reportes = reportes
            comentarios[posicion].miReaccion = miReaccion

            notifyItemChanged(posicion)

        }

    }
    fun mostrarCajaRespuesta(comentarioId: Int) {
        modoRespuesta = "CREAR"
        respuestaEditando = null

        val anterior = comentarioAbierto

        comentarioAbierto = comentarioId

        if (anterior != -1) {

            val posicionAnterior = comentarios.indexOfFirst {
                it.id == anterior
            }

            if (posicionAnterior != -1) {
                notifyItemChanged(posicionAnterior)
            }
        }

        val posicionNueva = comentarios.indexOfFirst {
            it.id == comentarioId
        }

        if (posicionNueva != -1) {
            notifyItemChanged(posicionNueva)
        }

    }
    fun editarComentario(comentarioId: Int) {

        val anterior = comentarioEditando

        comentarioEditando = comentarioId

        if (anterior != -1) {

            val posAnterior = comentarios.indexOfFirst {
                it.id == anterior
            }

            if (posAnterior != -1) {
                notifyItemChanged(posAnterior)
            }
        }

        val posNueva = comentarios.indexOfFirst {
            it.id == comentarioId
        }

        if (posNueva != -1) {
            notifyItemChanged(posNueva)
        }

    }
    fun cancelarEdicionComentario() {

        if (comentarioEditando == -1) return

        val posicion = comentarios.indexOfFirst {
            it.id == comentarioEditando
        }

        comentarioEditando = -1

        if (posicion != -1) {
            notifyItemChanged(posicion)
        }

    }
    fun mostrarRespuestaExistente(
        comentarioId: Int,
        respuesta: RespuestaComentario
    ) {

        modoRespuesta = "EDITAR"

        respuestaEditando = respuesta

        val anterior = comentarioAbierto

        comentarioAbierto = comentarioId

        if (anterior != -1) {

            val posicionAnterior = comentarios.indexOfFirst {
                it.id == anterior
            }

            if (posicionAnterior != -1) {
                notifyItemChanged(posicionAnterior)
            }

        }

        val posicionNueva = comentarios.indexOfFirst {
            it.id == comentarioId
        }

        if (posicionNueva != -1) {
            notifyItemChanged(posicionNueva)
        }

    }
    fun actualizarReaccionRespuesta(

        respuestaId: Int,

        likes: Int,

        dislikes: Int,

        reportes: Int,

        miReaccion: String?

    ) {

        comentarios.forEachIndexed { indiceComentario, comentario ->

            val respuesta = comentario.respuestas.firstOrNull {

                it.id == respuestaId

            }

            if (respuesta != null) {

                respuesta.likes = likes

                respuesta.dislikes = dislikes

                respuesta.reportes = reportes

                respuesta.miReaccion = miReaccion

                notifyItemChanged(indiceComentario)

                return

            }

        }

    }
}