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
import com.example.scannerkotlin.activities.ProductsDocumentActivity
import com.example.scannerkotlin.model.Product

class ProductAdapter(
    private val productList: MutableList<Product>,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    private var focusedPosition: Int = -1

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        val etQuantity: EditText = itemView.findViewById(R.id.etQuantity)
        val spinnerMeasure: Spinner = itemView.findViewById(R.id.spinnerMeasure)
        val tvBarcode: EditText = itemView.findViewById(R.id.tvBarcode)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int, payloads: List<Any>) {
        if (payloads.isNotEmpty() && payloads[0] == "barcode_update") {
            holder.tvBarcode.setText(productList[position].barcode)
            return
        }

        onBindViewHolder(holder, position)
    }


    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ProductViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val product = productList[position]


        holder.tvProductName.text = product.name


        holder.etQuantity.setText("")

        holder.tvBarcode.setText(product.barcode ?: "")


        holder.tvBarcode.showSoftInputOnFocus = false

        val measures = listOf("Штука", "Килограмм", "Литр", "Метр", "Грамм")
        val measureAdapter = ArrayAdapter(
            holder.itemView.context,
            android.R.layout.simple_spinner_dropdown_item,
            measures
        )
        holder.spinnerMeasure.adapter = measureAdapter

        val currentMeasureIndex = measures.indexOf(product.measureName)
        if (currentMeasureIndex != -1) {
            holder.spinnerMeasure.setSelection(currentMeasureIndex)
        }


        holder.etQuantity.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val input = s?.toString() ?: ""
                productList[position].quantity = input.toIntOrNull() ?: 0
                (holder.itemView.context as? ProductsDocumentActivity)?.updateSaveButtonState()
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        holder.spinnerMeasure.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                productList[position].measureName = measures[pos]
                (holder.itemView.context as? ProductsDocumentActivity)?.updateSaveButtonState()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        holder.btnDelete.setOnClickListener {
            onDelete(position)
        }

        holder.tvBarcode.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                focusedPosition = position
            }
        }

        holder.tvBarcode.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                productList[position].barcode = s.toString()
                (holder.itemView.context as? ProductsDocumentActivity)?.updateSaveButtonState()
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
}