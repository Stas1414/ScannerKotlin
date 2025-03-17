package com.example.scannerkotlin.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.scannerkotlin.R
import com.example.scannerkotlin.listener.OnItemClickListener
import com.example.scannerkotlin.model.Document

class DocumentAdapter(
    private var documentList: MutableList<Document>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<DocumentAdapter.ProductViewHolder>() {

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleView: TextView = itemView.findViewById(R.id.titleDocument)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.document_item, parent, false)
        return ProductViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val currentItem = documentList[position]
        holder.titleView.text = currentItem.title


        holder.itemView.setOnClickListener {
            listener.onItemClick(currentItem.title, currentItem.id.toString())
        }
    }

    override fun getItemCount() = documentList.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newDocuments: List<Document>) {
        documentList.clear()
        documentList.addAll(newDocuments)
        notifyDataSetChanged()
    }
}
