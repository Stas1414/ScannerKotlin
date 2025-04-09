package com.example.scannerkotlin.adapter

import android.annotation.SuppressLint
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.scannerkotlin.R
import com.example.scannerkotlin.activities.ProductsDocumentMovingActivity
import com.example.scannerkotlin.model.Document

class DocumentMovingAdapter(private val documentList: MutableList<Document>) :
    RecyclerView.Adapter<DocumentMovingAdapter.DocumentViewHolder>() {

    class DocumentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDocumentNumber: TextView = itemView.findViewById(R.id.tvDocumentNumber)
        val tvArrivalDate: TextView = itemView.findViewById(R.id.tvArrivalDate)
        val btnOpen: Button = itemView.findViewById(R.id.btnOpen)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.document_moving_item, parent, false)
        return DocumentViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
        val document = documentList[position]
        holder.tvDocumentNumber.text = document.title
        holder.tvArrivalDate.text = "Дата прихода ${document.dateCreate}"

        holder.btnOpen.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, ProductsDocumentMovingActivity::class.java)
            intent.putExtra("title", document.title)
            intent.putExtra("idDocument", document.id.toString())
            context.startActivity(intent)
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

