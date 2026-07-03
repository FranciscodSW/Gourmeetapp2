package com.example.gourmeet2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.gourmeet2.data.models.ComentarioReceta

class ComentarioAdapter(

    private val lista: MutableList<ComentarioReceta>

) : RecyclerView.Adapter<ComentarioAdapter.ViewHolder>() {

    inner class ViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        val imgUser =
            itemView.findViewById<ImageView>(R.id.imgUser)

        val tvNombre =
            itemView.findViewById<TextView>(R.id.tvNombreUsuario)

        val tvFecha =
            itemView.findViewById<TextView>(R.id.tvFechaRespuesta)

        val tvComentario =
            itemView.findViewById<TextView>(R.id.tvComentario)

        val tvLikes =
            itemView.findViewById<TextView>(R.id.btnLikeRespuesta)

        val tvDislikes =
            itemView.findViewById<TextView>(R.id.btnDislike)


    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val view = LayoutInflater
            .from(parent.context)
            .inflate(
                R.layout.item_comentario,
                parent,
                false
            )

        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        val comentario = lista[position]

        holder.tvNombre.text =
            comentario.usuario

        holder.tvFecha.text =
            comentario.fecha

        holder.tvComentario.text =
            comentario.comentario

        holder.tvLikes.text =
            comentario.likes.toString()

        holder.tvDislikes.text =
            comentario.dislikes.toString()
        if (comentario.foto != null) {
            Glide.with(holder.itemView.context)
                .load(comentario.foto)
                .placeholder(R.drawable.ic_user)
                .into(holder.imgUser)
        } else {
            holder.imgUser.setImageResource(R.drawable.ic_user)
        }
    }
    override fun getItemCount() =
        lista.size
}