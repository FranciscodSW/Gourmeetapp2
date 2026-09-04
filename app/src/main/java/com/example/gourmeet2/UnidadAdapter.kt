package com.example.gourmeet2.adapters

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.gourmeet2.R

class UnidadAdapter(
    context: Context,
    private val unidades: List<String>,
    private val titulos: Set<String>
) : ArrayAdapter<String>(
    context,
    android.R.layout.simple_dropdown_item_1line,
    unidades
) {

    override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup
    ): View {

        val view = convertView
            ?: LayoutInflater.from(context).inflate(
                android.R.layout.simple_dropdown_item_1line,
                parent,
                false
            )

        val textView = view.findViewById<TextView>(
            android.R.id.text1
        )

        val elemento = unidades[position]

        textView.text = elemento

        if (elemento in titulos) {

            // =========================
            // TÍTULO DE CATEGORÍA
            // =========================

            textView.setTypeface(
                null,
                Typeface.BOLD
            )

            textView.textSize = 14f
            textView.setTextColor(
                context.getColor(R.color.azulgourmeet)
            )

            textView.setPadding(
                16,
                16,
                16,
                8
            )

            view.isEnabled = false

        } else {

            // =========================
            // UNIDAD NORMAL
            // =========================

            textView.setTypeface(
                null,
                Typeface.NORMAL
            )

            textView.textSize = 14f

            textView.setTextColor(
                context.getColor(android.R.color.black)
            )

            textView.setPadding(
                24,
                12,
                16,
                12
            )

            view.isEnabled = true
        }

        return view
    }

    override fun isEnabled(position: Int): Boolean {

        return unidades[position] !in titulos
    }
}