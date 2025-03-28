package com.example.scannerkotlin.adapter

import android.annotation.SuppressLint
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.scannerkotlin.R
import com.example.scannerkotlin.activities.ProductsDocumentComingActivity
import com.example.scannerkotlin.model.Product

class ProductComingAdapter(
    private val productList: MutableList<Product>,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<ProductComingAdapter.ProductViewHolder>() {

    private var focusedPosition: Int = -1
    private val measures = listOf("Штука", "Килограмм", "Литр", "Метр", "Грамм")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_coming_product, parent, false)
        return ProductViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ProductViewHolder, position: Int, payloads: List<Any>) {
        if (payloads.contains("barcode_update")) {
            holder.tvBarcode.setText(productList[position].barcode)
            return
        }
        onBindViewHolder(holder, position)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ProductViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val product = productList[position]

        holder.tvProductName.text = product.name
        holder.etQuantity.setText(product.quantity.toString())
        holder.tvBarcode.setText(product.barcode ?: "")

        val currentMeasureIndex = measures.indexOf(product.measureName)
        if (currentMeasureIndex != -1) {
            holder.spinnerMeasure.setSelection(currentMeasureIndex)
        }

        holder.etQuantity.removeTextChangedListener(holder.quantityWatcher)
        holder.quantityWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                product.quantity = s?.toString()?.toIntOrNull() ?: 0
                (holder.itemView.context as? ProductsDocumentComingActivity)?.updateSaveButtonState()
            }

            override fun afterTextChanged(s: Editable?) {}
        }
        holder.etQuantity.addTextChangedListener(holder.quantityWatcher)

        holder.spinnerMeasure.onItemSelectedListener = null
        holder.spinnerMeasure.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                product.measureName = measures[pos]
                (holder.itemView.context as? ProductsDocumentComingActivity)?.updateSaveButtonState()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        holder.btnDelete.setOnClickListener {
            onDelete(position)
        }

        holder.tvBarcode.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                focusedPosition = holder.adapterPosition
            }
        }

        holder.tvBarcode.removeTextChangedListener(holder.barcodeWatcher)
        holder.tvBarcode.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (focusedPosition == holder.adapterPosition) {
                    productList[focusedPosition].barcode = s.toString()
                    (holder.itemView.context as? ProductsDocumentComingActivity)?.updateSaveButtonState()
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun getItemCount() = productList.size

    fun updateFocusedProductBarcode(newBarcode: String): Int? {
        return if (focusedPosition != -1) {
            val product = productList[focusedPosition]
            product.barcode = newBarcode
            notifyItemChanged(focusedPosition, "barcode_update")
            product.id
        } else {
            null
        }
    }

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        val etQuantity: EditText = itemView.findViewById(R.id.etQuantity)
        val spinnerMeasure: Spinner = itemView.findViewById(R.id.spinnerMeasure)
        val tvBarcode: EditText = itemView.findViewById(R.id.tvBarcode)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)

        var quantityWatcher: TextWatcher? = null
        var barcodeWatcher: TextWatcher? = null

        init {
            val measureAdapter = ArrayAdapter(
                itemView.context,
                android.R.layout.simple_spinner_dropdown_item,
                measures
            )
            spinnerMeasure.adapter = measureAdapter
        }
    }
}
