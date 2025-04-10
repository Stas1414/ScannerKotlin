package com.example.scannerkotlin.adapter

import android.annotation.SuppressLint
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.scannerkotlin.R
import com.example.scannerkotlin.model.Product

class ProductComingAdapter(
    private val productList: MutableList<Product>,
    private val onDelete: (position: Int) -> Unit // Лямбда для удаления
) : RecyclerView.Adapter<ProductComingAdapter.ProductViewHolder>() {

    private var focusedPosition: Int = RecyclerView.NO_POSITION // Инициализируем безопасным значением
    private val measures = listOf("Штука", "Килограмм", "Литр", "Метр", "Грамм") // Список мер

    // Listener для уведомления Activity об изменениях данных, влияющих на кнопку "Сохранить"
    private var onDataChangedListener: (() -> Unit)? = null

    fun setOnDataChangedListener(listener: () -> Unit) {
        this.onDataChangedListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_coming_product, parent, false)
        return ProductViewHolder(view)
    }

    // Обработка частичного обновления для штрихкода
    override fun onBindViewHolder(holder: ProductViewHolder, position: Int, payloads: List<Any>) {
        if (payloads.contains(PAYLOAD_BARCODE_UPDATE)) {
            // Проверяем, что позиция все еще валидна
            if (position >= 0 && position < productList.size) {
                val product = productList[position]
                holder.tvBarcode.setText(product.barcode ?: "") // Обновляем только штрихкод
                Log.d("Adapter", "Payload update for barcode at position $position")
            } else {
                Log.w("Adapter", "Payload update received for invalid position $position")
            }
        } else {
            // Полная привязка, если нет payload
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    // Полная привязка данных
    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        if (position >= 0 && position < productList.size) {
            val product = productList[position]
            holder.bind(product) // Передаем логику привязки в ViewHolder
        } else {
            Log.e("Adapter", "onBindViewHolder called with invalid position: $position, list size: ${productList.size}")
        }
    }

    override fun getItemCount() = productList.size

    // Метод для обновления штрихкода у элемента, который сейчас в фокусе
    fun updateFocusedProductBarcode(newBarcode: String): Boolean { // Возвращаем Boolean успеха
        val currentFocusedPosition = focusedPosition
        // Добавляем проверку на валидность позиции в списке
        if (currentFocusedPosition != RecyclerView.NO_POSITION && currentFocusedPosition < productList.size) {
            try {
                val product = productList[currentFocusedPosition]
                product.barcode = newBarcode
                // Уведомляем об изменении конкретного элемента с payload
                notifyItemChanged(currentFocusedPosition, PAYLOAD_BARCODE_UPDATE)
                Log.d("Adapter", "Barcode updated for product ${product.id} at position $currentFocusedPosition")
                onDataChangedListener?.invoke() // Уведомляем об изменении данных
                return true
            } catch (e: IndexOutOfBoundsException) {
                // Обработка случая, если список изменился между проверкой и доступом
                Log.e("Adapter", "IndexOutOfBoundsException in updateFocusedProductBarcode for position $currentFocusedPosition", e)
                focusedPosition = RecyclerView.NO_POSITION // Сбрасываем фокус
                return false
            }
        } else {
            Log.w("Adapter", "Cannot update barcode, focusedPosition is invalid or out of bounds: $currentFocusedPosition")
            return false
        }
    }

    // --- ViewHolder ---
    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        val etQuantity: EditText = itemView.findViewById(R.id.etQuantity)
        val spinnerMeasure: Spinner = itemView.findViewById(R.id.spinnerMeasure)
        val tvBarcode: EditText = itemView.findViewById(R.id.tvBarcode)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)

        var quantityWatcher: TextWatcher? = null
        var barcodeWatcher: TextWatcher? = null

        init {
            // Настраиваем адаптер для спиннера мер
            val measureAdapter = ArrayAdapter(
                itemView.context,
                android.R.layout.simple_spinner_dropdown_item,
                measures
            )
            spinnerMeasure.adapter = measureAdapter

            // Настройка поля штрихкода для скрытия клавиатуры (программно)
            tvBarcode.isFocusable = true
            tvBarcode.isFocusableInTouchMode = true
            tvBarcode.isCursorVisible = false
            tvBarcode.showSoftInputOnFocus = false
        }

        @SuppressLint("SetTextI18n")
        fun bind(product: Product) {
            // --- Привязка данных ---
            tvProductName.text = product.name ?: "Без названия"
            etQuantity.setText(product.quantity?.takeIf { it != 0 }?.toString() ?: "")
            tvBarcode.setText(product.barcode ?: "")

            // --- Настройка Спиннера мер ---
            spinnerMeasure.onItemSelectedListener = null // Сброс
            val currentMeasureIndex = measures.indexOf(product.measureName).takeIf { it != -1 } ?: 0
            spinnerMeasure.setSelection(currentMeasureIndex, false) // Установка

            spinnerMeasure.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                    val selectedMeasure = measures.getOrNull(pos) ?: return // Безопасное получение
                    val currentPosition = adapterPosition
                    if (currentPosition != RecyclerView.NO_POSITION) {
                        val currentProduct = productList.getOrNull(currentPosition) ?: return
                        if (currentProduct.measureName != selectedMeasure) {
                            currentProduct.measureName = selectedMeasure
                            // Обновляем measureId, если нужно
                            // currentProduct.measureId = getMeasureIdFromName(selectedMeasure)
                            Log.d("ViewHolder", "Measure changed to '$selectedMeasure' at pos $currentPosition")
                            onDataChangedListener?.invoke()
                        }
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            // --- Настройка Поля количества ---
            etQuantity.removeTextChangedListener(quantityWatcher) // Удаление
            quantityWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val currentPosition = adapterPosition
                    if (currentPosition != RecyclerView.NO_POSITION) {
                        val currentProduct = productList.getOrNull(currentPosition) ?: return
                        val newQuantity = s?.toString()?.toIntOrNull() ?: 0
                        // Обновляем модель, только если значение действительно изменилось
                        if (currentProduct.quantity != newQuantity) {
                            currentProduct.quantity = newQuantity
                            onDataChangedListener?.invoke()
                        }
                    }
                }
                override fun afterTextChanged(s: Editable?) {}
            }
            etQuantity.addTextChangedListener(quantityWatcher) // Добавление

            // --- Настройка Поля штрихкода ---
            tvBarcode.removeTextChangedListener(barcodeWatcher) // Удаление
            barcodeWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val currentPosition = adapterPosition
                    if (currentPosition != RecyclerView.NO_POSITION && tvBarcode.hasFocus() && focusedPosition == currentPosition) {
                        val currentProduct = productList.getOrNull(currentPosition) ?: return
                        val newBarcode = s?.toString() ?: ""
                        // Обновляем модель, только если значение изменилось
                        if (currentProduct.barcode != newBarcode) {
                            currentProduct.barcode = newBarcode
                            onDataChangedListener?.invoke()
                        }
                    }
                }
                override fun afterTextChanged(s: Editable?) {}
            }
            tvBarcode.addTextChangedListener(barcodeWatcher) // Добавление

            // Фокус listener для штрихкода
            tvBarcode.setOnFocusChangeListener { _, hasFocus ->
                val currentPosition = adapterPosition
                if (currentPosition != RecyclerView.NO_POSITION) {
                    if (hasFocus) {
                        focusedPosition = currentPosition
                        Log.d("ViewHolder", "Barcode EditText focused at position $currentPosition")
                    } else {
                        if (focusedPosition == currentPosition) {
                            focusedPosition = RecyclerView.NO_POSITION
                            Log.d("ViewHolder", "Barcode EditText lost focus at position $currentPosition")
                        }
                    }
                }
            }

            // Кнопка удаления
            btnDelete.setOnClickListener {
                val currentPosition = adapterPosition
                if (currentPosition != RecyclerView.NO_POSITION) {
                    onDelete(currentPosition)
                }
            }
        } // --- Конец bind ---

    } // --- Конец ViewHolder ---

    // Константа для payload
    companion object {
        const val PAYLOAD_BARCODE_UPDATE = "barcode_update"
    }
}