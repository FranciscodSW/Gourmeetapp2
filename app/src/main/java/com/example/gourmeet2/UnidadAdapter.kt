package com.example.gourmeet2

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.gourmeet2.R

class UnidadAdapter(
    context: Context,
    private val unidades: List<String>,
    private val titulos: Set<String>,
    private val onUnidadSeleccionada: (String) -> Unit
) : ArrayAdapter<String>(
    context,
    0,
    unidades
) {

    private sealed class ElementoMenu {

        data class Titulo(
            val texto: String
        ) : ElementoMenu()

        data class Fila(
            val unidad1: String,
            val unidad2: String?
        ) : ElementoMenu()
    }

    private val elementosMenu =
        mutableListOf<ElementoMenu>()

    init {
        construirMenu()
    }

    private fun construirMenu() {

        elementosMenu.clear()

        var unidadesCategoria =
            mutableListOf<String>()

        fun agregarCategoria() {

            if (unidadesCategoria.isEmpty()) {
                return
            }

            var posicion = 0

            while (posicion < unidadesCategoria.size) {

                val unidad1 =
                    unidadesCategoria[posicion]

                val unidad2 =
                    if (posicion + 1 < unidadesCategoria.size) {
                        unidadesCategoria[posicion + 1]
                    } else {
                        null
                    }

                elementosMenu.add(
                    ElementoMenu.Fila(
                        unidad1 = unidad1,
                        unidad2 = unidad2
                    )
                )

                posicion += 2
            }

            unidadesCategoria =
                mutableListOf()
        }

        for (elemento in unidades) {

            if (elemento in titulos) {

                agregarCategoria()

                elementosMenu.add(
                    ElementoMenu.Titulo(elemento)
                )

            } else {

                unidadesCategoria.add(elemento)
            }
        }

        agregarCategoria()
    }

    override fun getCount(): Int {
        return elementosMenu.size
    }

    override fun getItem(position: Int): String? {

        return when (
            val elemento = elementosMenu[position]
        ) {

            is ElementoMenu.Titulo ->
                elemento.texto

            is ElementoMenu.Fila ->
                elemento.unidad1
        }
    }

    override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup
    ): View {

        return when (
            val elemento = elementosMenu[position]
        ) {

            // =============================================
            // TÍTULO
            // =============================================

            is ElementoMenu.Titulo -> {

                val view =
                    LayoutInflater.from(context).inflate(
                        R.layout.item_unidad_titulo,
                        parent,
                        false
                    )

                val txtTitulo =
                    view.findViewById<TextView>(
                        R.id.txtTituloUnidad
                    )

                txtTitulo.text =
                    elemento.texto

                view
            }

            // =============================================
            // FILA DE UNIDADES
            // =============================================

            is ElementoMenu.Fila -> {

                val view =
                    LayoutInflater.from(context).inflate(
                        R.layout.item_unidades_fila,
                        parent,
                        false
                    )

                val txtUnidad1 =
                    view.findViewById<TextView>(
                        R.id.txtUnidad1
                    )

                val txtUnidad2 =
                    view.findViewById<TextView>(
                        R.id.txtUnidad2
                    )

                // -------------------------
                // UNIDAD 1
                // -------------------------

                txtUnidad1.text =
                    elemento.unidad1

                txtUnidad1.setOnClickListener {

                    seleccionarUnidad(
                        elemento.unidad1
                    )
                }

                // -------------------------
                // UNIDAD 2
                // -------------------------

                if (elemento.unidad2 != null) {

                    txtUnidad2.visibility =
                        View.VISIBLE

                    txtUnidad2.text =
                        elemento.unidad2

                    txtUnidad2.setOnClickListener {

                        seleccionarUnidad(
                            elemento.unidad2
                        )
                    }

                } else {

                    txtUnidad2.visibility =
                        View.INVISIBLE
                }

                view
            }
        }
    }

    // =============================================
    // SELECCIONAR UNIDAD
    // =============================================

    private fun seleccionarUnidad(
        unidad: String
    ) {

        onUnidadSeleccionada(unidad)
    }

    override fun isEnabled(position: Int): Boolean {

        return when (
            elementosMenu[position]
        ) {

            is ElementoMenu.Titulo ->
                false

            is ElementoMenu.Fila ->
                true
        }
    }
}