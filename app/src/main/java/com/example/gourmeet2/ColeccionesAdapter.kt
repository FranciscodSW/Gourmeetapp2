package com.example.gourmeet2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gourmeet2.R
import com.example.gourmeet2.data.models.ColeccionConRecetas
import com.example.gourmeet2.data.models.Receta
import com.example.gourmeet2.data.models.RecetaColeccion
import com.example.gourmeet2.databinding.ItemColeccionRecetasBinding

class ColeccionesAdapter(

    private val colecciones: MutableList<ColeccionConRecetas>,

    private val listener: OnColeccionListener

) : RecyclerView.Adapter<ColeccionesAdapter.ViewHolder>() {
    interface OnColeccionListener {

        fun onEditar(
            coleccion: ColeccionConRecetas
        )

        fun onEliminar(
            coleccion: ColeccionConRecetas
        )

        fun onRecetaClick(
            recetaId: Int
        )

    }

    inner class ViewHolder(

        val binding: ItemColeccionRecetasBinding

    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val binding = ItemColeccionRecetasBinding.inflate(

            LayoutInflater.from(parent.context),

            parent,

            false

        )

        return ViewHolder(binding)

    }
    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        val coleccion = colecciones[position]

        holder.binding.txtNombreColeccion.text =
            coleccion.COL_NOMBRE

        holder.binding.imgIconoColeccion.setImageResource(
            obtenerIcono(coleccion.COL_PORTADA)
        )

        holder.binding.imgEditar.setOnClickListener {

            listener.onEditar(coleccion)

        }

        holder.binding.imgEliminar.setOnClickListener {

            listener.onEliminar(coleccion)

        }

        //-----------------------------------------
        // Recycler horizontal
        //-----------------------------------------

        val recetaAdapter = RecetasColeccionAdapter(

            coleccion.RECETAS.toMutableList(),

            object : RecetasColeccionAdapter.OnRecetaClickListener {

                override fun onRecetaClick(
                    receta: RecetaColeccion
                ) {

                    listener.onRecetaClick(
                        receta.REC_ID
                    )

                }

            }

        )

        holder.binding.recyclerRecetasColeccion.apply {

            layoutManager = LinearLayoutManager(

                holder.itemView.context,

                RecyclerView.HORIZONTAL,

                false

            )

            adapter = RecetasColeccionAdapter(

                coleccion.RECETAS.toMutableList(),

                object : RecetasColeccionAdapter.OnRecetaClickListener {

                    override fun onRecetaClick(
                        receta: RecetaColeccion
                    ) {

                        listener.onRecetaClick(
                            receta.REC_ID
                        )

                    }

                }

            )

        }

    }
    override fun getItemCount(): Int {

        return colecciones.size

    }
    fun actualizar(

        nuevas: List<ColeccionConRecetas>

    ){

        colecciones.clear()

        colecciones.addAll(nuevas)

        notifyDataSetChanged()

    }
    private fun obtenerIcono(
        portada:String?
    ):Int{

        return when(portada){

            "ic_favoritos" ->
                R.drawable.ic_favoritos

            "ic_desayuno" ->
                R.drawable.ic_desayuno

            "ic_pasta" ->
                R.drawable.ic_pasta

            "ic_carne" ->
                R.drawable.ic_carne

            else ->
                R.drawable.ic_guardar

        }

    }
}