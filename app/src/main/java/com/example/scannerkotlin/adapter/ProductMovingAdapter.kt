package com.example.scannerkotlin.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.scannerkotlin.R
import com.example.scannerkotlin.model.DocumentElement
import com.example.scannerkotlin.model.Product
import com.example.scannerkotlin.model.Store

class ProductMovingAdapter(
    private val productList: MutableList<DocumentElement>,
    private val storeToList: MutableList<Store>,
    private val storeFromList: MutableList<Store>,
    private val onDelete: (Int) -> Unit,
    private val onQuantityChanged: (Int, String) -> Unit,
    private val onStoreSelected: (Int, Int, Int) -> Unit // position, storeFromPos, storeToPos
) : RecyclerView.Adapter<ProductMovingAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productName: TextView = itemView.findViewById(R.id.tvProductMovingName)
        val quantity: EditText = itemView.findViewById(R.id.movingQuantity)
        val spinnerStoreTo: Spinner = itemView.findViewById(R.id.spinnerStoreTo)
        val spinnerStoreFrom: Spinner = itemView.findViewById(R.id.spinnerStoreFrom)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnMovingDelete)
        val tvAvailableQuantity: TextView = itemView.findViewById(R.id.tvAvailableQuantity)

        private val quantityTextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                onQuantityChanged(adapterPosition, s.toString())
            }
        }

        init {
            quantity.addTextChangedListener(quantityTextWatcher)

            val storeToAdapter = ArrayAdapter(
                itemView.context,
                android.R.layout.simple_spinner_dropdown_item,
                storeToList.map { it.title } // Показываем только названия складов
            )

            val storeFromAdapter = ArrayAdapter(
                itemView.context,
                android.R.layout.simple_spinner_dropdown_item,
                storeFromList.map { it.title }
            )

            spinnerStoreFrom.adapter = storeFromAdapter
            spinnerStoreTo.adapter = storeToAdapter

            spinnerStoreFrom.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    onStoreSelected(adapterPosition, position, spinnerStoreTo.selectedItemPosition)
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }

            spinnerStoreTo.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    onStoreSelected(adapterPosition, spinnerStoreFrom.selectedItemPosition, position)
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_moving_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun getItemCount(): Int = productList.size

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = productList[position]

        holder.productName.text = product.name
        holder.quantity.setText(product.quantity?.toString() ?: "1")
        holder.tvAvailableQuantity.text = product.quantity.toString()

        // Установка выбранных складов, если они есть в продукте
        product..let {
            if (it < holder.spinnerStoreFrom.adapter.count) {
                holder.spinnerStoreFrom.setSelection(it)
            }
        }

        product.selectedStoreToPosition?.let {
            if (it < holder.spinnerStoreTo.adapter.count) {
                holder.spinnerStoreTo.setSelection(it)
            }
        }

        holder.btnDelete.setOnClickListener {
            onDelete(position)
        }
    }

    fun updateStores(newStoreFromList: List<Store>, newStoreToList: List<Store>) {
        storeFromList.clear()
        storeFromList.addAll(newStoreFromList)

        storeToList.clear()
        storeToList.addAll(newStoreToList)

        notifyDataSetChanged()
    }

    fun updateAvailableQuantity(position: Int, quantity: Int) {
        if (position in 0 until productList.size) {
            productList[position].availableQuantity = quantity
            notifyItemChanged(position)
        }
    }
}