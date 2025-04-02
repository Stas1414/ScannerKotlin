package com.example.scannerkotlin.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.scannerkotlin.R
import com.example.scannerkotlin.model.DocumentElement
import com.example.scannerkotlin.model.Store

class ProductMovingAdapter(
    private val context: Context,
    private val documentList: MutableList<DocumentElement>,
    private val storeList: List<Store>,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<ProductMovingAdapter.DocumentViewHolder>() {

    private val storeTitles = storeList.map { it.title }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_moving_product, parent, false)
        return DocumentViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
        val document = documentList[position]

        // Устанавливаем название товара
        holder.tvProductName.text = document.name ?: "Без названия"

        // Инициализация спиннеров
        val storeAdapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_dropdown_item,
            storeList.map { it.title }
        )
        holder.spinnerFrom.adapter = storeAdapter
        holder.spinnerTo.adapter = storeAdapter

        // Устанавливаем выбранные позиции в спиннерах
        document.storeFrom?.let { fromId ->
            storeList.indexOfFirst { it.id == fromId }.takeIf { it != -1 }?.let {
                holder.spinnerFrom.setSelection(it)
            }
        }

        document.storeTo?.let { toId ->
            storeList.indexOfFirst { it.id == toId }.takeIf { it != -1 }?.let {
                holder.spinnerTo.setSelection(it)
            }
        }

        // Обработчики выбора в спиннерах
        holder.spinnerFrom.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                document.storeFrom = storeList[pos].id
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        holder.spinnerTo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                document.storeTo = storeList[pos].id
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Устанавливаем доступное количество
        val availableAmount = document.amount ?: 0.0
        holder.availableQuantity.text = "Доступно: $availableAmount"

        // Удаляем предыдущий TextWatcher
        holder.etQuantity.removeTextChangedListener(holder.quantityTextWatcher)

        // Устанавливаем начальное значение
        holder.etQuantity.setText(document.amount?.takeIf { it != 0.0 }?.toString() ?: "")

        // Создаем новый TextWatcher
        holder.quantityTextWatcher = object : TextWatcher {
            private var lastValidValue = holder.etQuantity.text.toString()

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                lastValidValue = s?.toString() ?: ""
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val input = s?.toString() ?: ""

                // Если поле пустое
                if (input.isEmpty()) {
                    document.amount = null
                    return
                }

                // Проверяем корректность ввода
                if (input.matches(Regex("^\\d*\\.?\\d*$"))) {
                    val value = input.toDoubleOrNull() ?: 0.0

                    // Проверяем, не превышает ли ввод доступное количество
                    if (value > availableAmount) {
                        holder.etQuantity.setText(availableAmount.toString())
                        holder.etQuantity.setSelection(holder.etQuantity.text.length)
                        document.amount = availableAmount
                    } else {
                        document.amount = value
                    }
                } else {

                    holder.etQuantity.setText(lastValidValue)
                    holder.etQuantity.setSelection(lastValidValue.length)
                }
            }
        }


        holder.etQuantity.addTextChangedListener(holder.quantityTextWatcher)


        holder.btnDelete.setOnClickListener {
            onDeleteClick(position)
        }
    }
    override fun getItemCount(): Int = documentList.size

    fun removeItem(position: Int) {
        documentList.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, documentList.size)
    }

    inner class DocumentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvProductName: TextView = view.findViewById(R.id.tvProductName)
        val spinnerFrom: Spinner = view.findViewById(R.id.spinnerItem1)
        val spinnerTo: Spinner = view.findViewById(R.id.spinnerItem2)
        val etQuantity: EditText = view.findViewById(R.id.etQuantity)
        val availableQuantity: TextView = view.findViewById(R.id.tvAvailable)
        val btnDelete: ImageView = view.findViewById(R.id.btnDelete)


        var quantityTextWatcher: TextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        }

        init {
            val adapter = ArrayAdapter(
                itemView.context,
                android.R.layout.simple_spinner_dropdown_item,
                storeTitles
            )
            spinnerTo.adapter = adapter
            spinnerFrom.adapter = adapter
        }
    }
}
