package com.example.gourmeet2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gourmeet2.data.models.Alacena

class AlacenaMenuAdapter(
    private val lista: List<Alacena>,
    private val onSeleccionar: (Alacena) -> Unit,
    private val onEditar: (Alacena) -> Unit,
    private val onEliminar: (Alacena) -> Unit
) : RecyclerView.Adapter<AlacenaMenuAdapter.AlacenaViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AlacenaViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.item_alacena_menu,
                parent,
                false
            )

        return AlacenaViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: AlacenaViewHolder,
        position: Int
    ) {

        val alacena = lista[position]

        holder.txtNombre.text =
            alacena.ALC_NOMBRE


        // ======================================
        // ICONO
        // ======================================

        when (alacena.ALC_ICONO.uppercase()) {

            "CASA" -> {

                holder.imgIcono.setImageResource(
                    R.drawable.ic_casa1
                )
            }

            "OFICINA" -> {

                holder.imgIcono.setImageResource(
                    R.drawable.ic_casa2
                )
            }

            "REFRIGERADOR" -> {

                holder.imgIcono.setImageResource(
                    R.drawable.ic_oficinas
                )
            }

            else -> {

                holder.imgIcono.setImageResource(
                    R.drawable.ic_casa1
                )
            }
        }


        // ======================================
        // SELECCIONAR ALACENA
        // ======================================

        holder.itemView.setOnClickListener {

            onSeleccionar(alacena)
        }


        // ======================================
        // EDITAR
        // ======================================

        holder.btnEditar.setOnClickListener {

            onEditar(alacena)
        }


        // ======================================
        // ELIMINAR
        // ======================================

        holder.btnEliminar.setOnClickListener {

            onEliminar(alacena)
        }
    }

    override fun getItemCount(): Int {

        return lista.size
    }


    class AlacenaViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        val imgIcono: ImageView =
            itemView.findViewById(
                R.id.imgIconoAlacena
            )

        val txtNombre: TextView =
            itemView.findViewById(
                R.id.txtNombreAlacena
            )

        val btnEditar: ImageButton =
            itemView.findViewById(
                R.id.btnEditarAlacena
            )

        val btnEliminar: ImageButton =
            itemView.findViewById(
                R.id.btnEliminarAlacena
            )
    }
}