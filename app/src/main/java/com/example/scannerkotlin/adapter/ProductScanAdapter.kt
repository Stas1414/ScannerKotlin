package com.example.scannerkotlin.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.scannerkotlin.R
import com.example.scannerkotlin.model.Product

class ProductScanAdapter(private val products: MutableList<Product>) :
    RecyclerView.Adapter<ProductScanAdapter.ProductViewHolder>() {

    class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvName: TextView = view.findViewById(R.id.tvScanName)
        private val tvMeasure: TextView = view.findViewById(R.id.tvScanMeasure)
        private val tvQuantity: TextView = view.findViewById(R.id.tvScanQuantity)

        @SuppressLint("SetTextI18n")
        fun bind(product: Product) {
            tvName.text = product.name ?: "Нет названия"
            tvMeasure.text = "Ед. изм.: ${product.measureName ?: "Не указано"}"
            tvQuantity.text = "Кол-во: ${product.quantity ?: 0}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_scan_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(products[position])
    }



    override fun getItemCount(): Int = products.size
}