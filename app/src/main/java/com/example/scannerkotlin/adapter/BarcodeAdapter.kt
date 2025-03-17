package com.example.scannerkotlin.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.scannerkotlin.R
import com.example.scannerkotlin.model.Barcode

class BarcodeAdapter(private val productList: MutableList<Barcode>) :
    RecyclerView.Adapter<BarcodeAdapter.ProductViewHolder>() {

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dataTextView: TextView = itemView.findViewById(R.id.dataTextView)
        val symbologyTextView: TextView = itemView.findViewById(R.id.symbologyTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.barcode_item, parent, false)
        return ProductViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val currentItem = productList[position]
        holder.dataTextView.text = currentItem.data
        holder.symbologyTextView.text = currentItem.symbology
    }

    override fun getItemCount() = productList.size

}