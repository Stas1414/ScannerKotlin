package com.example.scannerkotlin.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.scannerkotlin.R
import com.example.scannerkotlin.activities.ProductsDocumentMovingActivity
import com.example.scannerkotlin.model.DocumentElement
import com.example.scannerkotlin.model.Store
import com.example.scannerkotlin.service.CatalogDocumentMovingService

class ProductMovingAdapter(
    private val context: Context,
    private val documentList: MutableList<DocumentElement>,
    private val storeList: List<Store>,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<ProductMovingAdapter.DocumentViewHolder>() {

   private val service:CatalogDocumentMovingService = CatalogDocumentMovingService()



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_moving_product, parent, false)
        return DocumentViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
        val document = documentList[position]


        holder.tvProductName.text = document.name ?: "Без названия"


        val storeAdapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_dropdown_item,
            storeList.map { it.title }
        )
        holder.spinnerFrom.adapter = storeAdapter
        holder.spinnerTo.adapter = storeAdapter


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


        holder.spinnerFrom.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                val selectedStoreId = storeList[pos].id
                document.storeFrom = selectedStoreId


                service.getStoreAmount(selectedStoreId, document.elementId) { amount ->
                    Log.d("Adapter", "Getting amount: $amount")
                    (context as? Activity)?.runOnUiThread {

                        val availableAmount = (amount ?: 0.0).coerceAtLeast(0.0)
                        document.mainAmount = availableAmount
                        holder.availableQuantity.text = "Доступно: $availableAmount"


                        document.amount?.let { currentAmount ->
                            if (currentAmount > availableAmount) {
                                document.amount = availableAmount
                                holder.etQuantity.setText(availableAmount.toString())
                            }
                        }
                    }
                }

                (holder.itemView.context as? ProductsDocumentMovingActivity)?.updateSaveButtonState()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        holder.spinnerTo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                document.storeTo = storeList[pos].id
                (holder.itemView.context as? ProductsDocumentMovingActivity)?.updateSaveButtonState()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }


        val availableAmount = document.mainAmount ?: 0.0
        holder.availableQuantity.text = "Доступно: $availableAmount"

        holder.etQuantity.removeTextChangedListener(holder.quantityTextWatcher)

        holder.etQuantity.setText(document.amount?.takeIf { it != 0.0 }?.toString() ?: "")

        holder.quantityTextWatcher = object : TextWatcher {
            private var lastValidValue = document.amount?.toString() ?: ""
            private var isSelfUpdate = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if (!isSelfUpdate) {
                    lastValidValue = s?.toString() ?: ""
                }
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                (holder.itemView.context as? ProductsDocumentMovingActivity)?.updateSaveButtonState()
            }

            override fun afterTextChanged(s: Editable?) {
                if (isSelfUpdate) return

                val input = s?.toString() ?: ""

                if (input.isEmpty()) {
                    document.amount = null
                    lastValidValue = ""
                    return
                }

                try {
                    val value = input.toDouble()
                    val availableAmount = document.mainAmount ?: 0.0

                    if (value > availableAmount && availableAmount > 0) {
                        isSelfUpdate = true
                        val correctedValue = availableAmount
                        holder.etQuantity.setText(correctedValue.toString())
                        holder.etQuantity.setSelection(holder.etQuantity.text.length)
                        document.amount = correctedValue
                        lastValidValue = correctedValue.toString()
                    } else {
                        document.amount = value
                        lastValidValue = input
                    }
                } catch (e: NumberFormatException) {
                    isSelfUpdate = true
                    holder.etQuantity.setText(lastValidValue)
                    holder.etQuantity.setSelection(lastValidValue.length)
                } finally {
                    isSelfUpdate = false
                }
            }
        }


        holder.etQuantity.addTextChangedListener(holder.quantityTextWatcher)


        holder.btnDelete.setOnClickListener {
            onDeleteClick(position)
        }
    }
    override fun getItemCount(): Int = documentList.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newItems: List<DocumentElement>) {
        documentList.clear()
        documentList.addAll(newItems)
        notifyDataSetChanged()
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

    }
}
