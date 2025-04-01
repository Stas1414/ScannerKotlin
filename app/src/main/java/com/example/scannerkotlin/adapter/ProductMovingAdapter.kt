package com.example.scannerkotlin.adapter

import android.annotation.SuppressLint
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.scannerkotlin.R
import com.example.scannerkotlin.model.DocumentElement
import com.example.scannerkotlin.model.Store

class ProductMovingAdapter(
    private val items: MutableList<DocumentElement>,
    private val stores: MutableList<Store>,
    private val onItemDeleted: (position: Int) -> Unit,
    private val onQuantityChanged: (position: Int, quantity: Double) -> Unit,
    private val onStoreSelected: (position: Int, fromStoreId: Int?, toStoreId: Int?) -> Unit
) : RecyclerView.Adapter<ProductMovingAdapter.MovingViewHolder>() {

    inner class MovingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvProductName: TextView = itemView.findViewById(R.id.tvProductMovingName)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnMovingDelete)
        val spinnerFrom: Spinner = itemView.findViewById(R.id.spinnerStoreFrom)
        val spinnerTo: Spinner = itemView.findViewById(R.id.spinnerStoreTo)
        val tvAvailableQuantity: TextView = itemView.findViewById(R.id.tvAvailableQuantity)
        val etQuantity: EditText = itemView.findViewById(R.id.movingQuantity)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_moving_product, parent, false)
        return MovingViewHolder(view)
    }

    override fun onBindViewHolder(holder: MovingViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val item = items[position]

        // Установка названия товара
        holder.tvProductName.text = item.name ?: ""

        // Установка доступного количества (если есть такая информация)
         holder.tvAvailableQuantity.text = item.amount.toString()

        // Установка количества
        holder.etQuantity.setText(item.amount?.toString() ?: "0")

        // Настройка адаптеров для спиннеров
        val storeAdapter = ArrayAdapter(
            holder.itemView.context,
            android.R.layout.simple_spinner_item,
            stores.map { it.title }
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        holder.spinnerFrom.adapter = storeAdapter
        holder.spinnerTo.adapter = storeAdapter

        // Выбор текущих складов в спиннерах
        item.storeFrom?.let { fromId ->
            val fromPos = stores.indexOfFirst { it.id == fromId }
            if (fromPos != -1) holder.spinnerFrom.setSelection(fromPos)
        }

        item.storeTo?.let { toId ->
            val toPos = stores.indexOfFirst { it.id == toId }
            if (toPos != -1) holder.spinnerTo.setSelection(toPos)
        }

        // Обработка изменения количества
        holder.etQuantity.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                try {
                    val quantity = s.toString().toDouble()
                    item.amount = quantity
                    onQuantityChanged(position, quantity)
                } catch (e: NumberFormatException) {
                    item.amount = 0.0
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Обработка выбора складов
        holder.spinnerFrom.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                item.storeFrom = stores[pos].id
                onStoreSelected(position, item.storeFrom, item.storeTo)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        holder.spinnerTo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                item.storeTo = stores[pos].id
                onStoreSelected(position, item.storeFrom, item.storeTo)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Обработка удаления элемента
        holder.btnDelete.setOnClickListener {
            onItemDeleted(position)
        }
    }

    override fun getItemCount(): Int = items.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateStores(newStores: List<Store>) {
        stores.clear()
        stores.addAll(newStores)
        notifyDataSetChanged()
    }
}