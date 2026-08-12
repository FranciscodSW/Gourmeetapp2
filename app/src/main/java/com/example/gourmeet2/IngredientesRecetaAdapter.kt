package com.example.gourmeet2
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.gourmeet2.data.models.IngredienteReceta
import com.example.gourmeet2.databinding.ItemIngredienteMiniBinding

class IngredientesRecetaAdapter(

    private val ingredientes: List<IngredienteReceta>

) : RecyclerView.Adapter<IngredientesRecetaAdapter.ViewHolder>() {

    inner class ViewHolder(
        val binding: ItemIngredienteMiniBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val binding =
            ItemIngredienteMiniBinding.inflate(
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

        val ingrediente =
            ingredientes[position]

        with(holder.binding) {

            txtIngrediente.text =
                ingrediente.nombre

            if (!ingrediente.foto.isNullOrEmpty()) {

                Glide.with(imgIngrediente.context)
                    .load(ingrediente.foto)
                    .centerCrop()
                    .into(imgIngrediente)

            } else {

                imgIngrediente.setImageResource(
                    R.drawable.ic_logo_circular
                )
            }
        }
    }

    override fun getItemCount(): Int =
        ingredientes.size
}