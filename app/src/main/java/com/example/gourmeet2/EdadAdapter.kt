package com.example.gourmeet2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EdadAdapter(
    private val edades: List<Int>,
    private val onEdadClick: (Int) -> Unit
) : RecyclerView.Adapter<EdadAdapter.EdadViewHolder>() {

    class EdadViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtEdad: TextView = itemView.findViewById(R.id.txtEdad)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EdadViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_edad, parent, false)
        return EdadViewHolder(view)
    }

    override fun onBindViewHolder(holder: EdadViewHolder, position: Int) {

        val edad = edades[position]

        holder.txtEdad.text = edad.toString()

        holder.itemView.setOnClickListener {
            onEdadClick(edad)
        }
    }

    override fun getItemCount(): Int {
        return edades.size
    }
}