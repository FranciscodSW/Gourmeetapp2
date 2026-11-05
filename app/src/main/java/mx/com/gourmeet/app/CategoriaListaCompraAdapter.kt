package mx.com.gourmeet.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import mx.com.gourmeet.app.data.models.CategoriaListaCompra
import mx.com.gourmeet.app.data.models.IngredienteListaCompra
import mx.com.gourmeet.app.databinding.ItemTituloDeLaListaDeComprasBinding

class CategoriaListaCompraAdapter(
    private val categorias: MutableList<CategoriaListaCompra>,
    private val onEstadoCambiado: (IngredienteListaCompra, Int) -> Unit
) : RecyclerView.Adapter<CategoriaListaCompraAdapter.CategoriaViewHolder>() {

    inner class CategoriaViewHolder(
        val binding: ItemTituloDeLaListaDeComprasBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CategoriaViewHolder {

        val binding = ItemTituloDeLaListaDeComprasBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return CategoriaViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: CategoriaViewHolder,
        position: Int
    ) {

        val categoria = categorias[position]

        with(holder.binding) {

            nombredelacategoriadecompra.text =
                categoria.nombreCategoria

            listadefaltante.layoutManager =
                LinearLayoutManager(root.context)

            listadefaltante.isNestedScrollingEnabled = false
            listadefaltante.setHasFixedSize(false)

            listadefaltante.adapter =
                IngredienteListaCompraAdapter(
                    categoria.ingredientes.toMutableList(),
                    onEstadoCambiado
                )
        }
    }

    override fun getItemCount(): Int = categorias.size

    fun actualizarLista(nuevaLista: List<CategoriaListaCompra>) {
        categorias.clear()
        categorias.addAll(nuevaLista)
        notifyDataSetChanged()
    }
}
