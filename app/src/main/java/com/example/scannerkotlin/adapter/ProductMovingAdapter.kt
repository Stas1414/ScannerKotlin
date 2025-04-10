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
import androidx.lifecycle.LifecycleOwner 
import androidx.lifecycle.lifecycleScope 
import androidx.recyclerview.widget.RecyclerView
import com.example.scannerkotlin.R


import com.example.scannerkotlin.model.DocumentElement
import com.example.scannerkotlin.model.Store
import com.example.scannerkotlin.service.CatalogDocumentMovingService
import kotlinx.coroutines.* 

class ProductMovingAdapter(
    private val context: Context,
    private var documentList: MutableList<DocumentElement>, 
    private var storeList: List<Store>,                 
    private val onDeleteClick: (position: Int) -> Unit
) : RecyclerView.Adapter<ProductMovingAdapter.DocumentViewHolder>() {

    
    private val service: CatalogDocumentMovingService by lazy { CatalogDocumentMovingService() }
    
    private var onDataChangedListener: (() -> Unit)? = null

    fun setOnDataChangedListener(listener: () -> Unit) {
        this.onDataChangedListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_moving_product, parent, false)
        return DocumentViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: DocumentViewHolder, @SuppressLint("RecyclerView") position: Int) { 
        val document = documentList[position]

        holder.bind(document, position)
    }

    override fun getItemCount(): Int = documentList.size

    
    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newItems: List<DocumentElement>) {
        documentList.clear()
        documentList.addAll(newItems)
        notifyDataSetChanged() 
        onDataChangedListener?.invoke() 
    }

    
    @SuppressLint("NotifyDataSetChanged")
    fun updateStores(newStores: List<Store>) {
        storeList = newStores
        
        notifyDataSetChanged()
    }

    
    override fun onViewRecycled(holder: DocumentViewHolder) {
        super.onViewRecycled(holder)
        holder.cancelAmountJob() 
        Log.d("Adapter", "ViewHolder recycled, job cancelled for position ${holder.adapterPosition}")
    }

    
    inner class DocumentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvProductName: TextView = view.findViewById(R.id.tvProductName)
        private val spinnerFrom: Spinner = view.findViewById(R.id.spinnerItem1)
        private val spinnerTo: Spinner = view.findViewById(R.id.spinnerItem2)
        val etQuantity: EditText = view.findViewById(R.id.etQuantity)
        private val availableQuantity: TextView = view.findViewById(R.id.tvAvailable)
        private val btnDelete: ImageView = view.findViewById(R.id.btnDelete)

        
        private var currentAmountJob: Job? = null
        private var quantityTextWatcher: TextWatcher? = null 

        @SuppressLint("SetTextI18n")
        fun bind(document: DocumentElement, position: Int) {
            
            cancelAmountJob()

            tvProductName.text = document.name ?: "Без названия"

            
            val storeTitles = storeList.map { it.title  }
            val storeAdapter = ArrayAdapter(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                storeTitles
            )
            spinnerFrom.adapter = storeAdapter
            spinnerTo.adapter = storeAdapter

            
            val initialFromPosition = storeList.indexOfFirst { it.id == document.storeFrom }.takeIf { it != -1 } ?: 0
            val initialToPosition = storeList.indexOfFirst { it.id == document.storeTo }.takeIf { it != -1 } ?: 0
            spinnerFrom.setSelection(initialFromPosition, false) 
            spinnerTo.setSelection(initialToPosition, false)

            
            spinnerFrom.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                    if (pos < 0 || pos >= storeList.size) return 
                    val selectedStoreId = storeList[pos].id
                    
                    if (document.storeFrom != selectedStoreId) {
                        document.storeFrom = selectedStoreId
                        Log.d("Adapter", "StoreFrom selected: $selectedStoreId for ${document.name}")
                        
                        loadAvailableAmount(document, this@DocumentViewHolder) 
                        onDataChangedListener?.invoke() 
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

            spinnerTo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                    if (pos < 0 || pos >= storeList.size) return 
                    val selectedStoreId = storeList[pos].id
                    if (document.storeTo != selectedStoreId) {
                        document.storeTo = selectedStoreId
                        Log.d("Adapter", "StoreTo selected: $selectedStoreId for ${document.name}")
                        onDataChangedListener?.invoke() 
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

            
            
            if (document.mainAmount != null) {
                availableQuantity.text = "Доступно: ${document.mainAmount}"
            } else {
                availableQuantity.text = "Доступно: Загрузка..."
                
                if (document.storeFrom != null) { 
                    loadAvailableAmount(document, this)
                } else {
                    availableQuantity.text = "Доступно: Выберите склад"
                }
            }

            
            etQuantity.removeTextChangedListener(quantityTextWatcher) 
            etQuantity.setText(document.amount?.takeIf { it != 0 }?.toString() ?: "") 

            quantityTextWatcher = object : TextWatcher {
                private var lastValidValue = document.amount?.toString() ?: ""
                private var isSelfUpdate = false 

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    if (!isSelfUpdate) {
                        lastValidValue = s?.toString() ?: ""
                    }
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    
                    onDataChangedListener?.invoke()
                }

                override fun afterTextChanged(s: Editable?) {
                    if (isSelfUpdate) return 

                    val input = s?.toString() ?: ""
                    val currentPosition = adapterPosition
                    if (currentPosition == RecyclerView.NO_POSITION) return 

                    val currentDocument = documentList[currentPosition] 

                    if (input.isEmpty()) {
                        currentDocument.amount = null
                        lastValidValue = ""
                        return
                    }

                    try {
                        val value = input.toDouble()
                        val availableAmount = currentDocument.mainAmount ?: 0.0 

                        
                        if (value < 0) { 
                            isSelfUpdate = true
                            etQuantity.setText("0")
                            etQuantity.setSelection(etQuantity.text.length)
                            currentDocument.amount = 0
                            lastValidValue = "0"
                        } else if (value > availableAmount && availableAmount >= 0) { 
                            isSelfUpdate = true
                            val correctedValueStr = availableAmount.toInt().toString() 
                            etQuantity.setText(correctedValueStr)
                            etQuantity.setSelection(etQuantity.text.length)
                            currentDocument.amount = availableAmount.toInt()
                            lastValidValue = correctedValueStr
                        } else {
                            
                            currentDocument.amount = value.toInt()
                            lastValidValue = input
                        }
                    } catch (e: NumberFormatException) {
                        Log.w("AdapterInput", "Invalid number format: $input")
                        
                        isSelfUpdate = true
                        etQuantity.setText(lastValidValue)
                        etQuantity.setSelection(lastValidValue.length)
                        
                        currentDocument.amount = lastValidValue.toIntOrNull()
                    } finally {
                        isSelfUpdate = false
                    }
                }
            }
            etQuantity.addTextChangedListener(quantityTextWatcher) 

            
            btnDelete.setOnClickListener {
                val currentPosition = adapterPosition
                if (currentPosition != RecyclerView.NO_POSITION) {
                    onDeleteClick(currentPosition) 
                }
            }
        }

        
        @SuppressLint("SetTextI18n")
        private fun loadAvailableAmount(document: DocumentElement, holder: DocumentViewHolder) {
            val storeId = document.storeFrom ?: return 
            val productId = document.elementId ?: return 
            val lifecycleOwner = context as? LifecycleOwner ?: return 

            
            cancelAmountJob()
            Log.d("Adapter", "Launching job to load amount for product ${document.elementId} from store $storeId")
            holder.availableQuantity.text = "Доступно: Загрузка..." 

            currentAmountJob = lifecycleOwner.lifecycleScope.launch {
                var amount: Double? = null
                var error: Throwable? = null
                try {
                    
                    amount = service.getStoreAmount(storeId, productId)
                    Log.d("Adapter", "Amount received: $amount for product $productId from store $storeId")
                } catch (e: Exception) {
                    Log.e("Adapter", "Error loading store amount for product $productId from store $storeId", e)
                    error = e
                }

                
                withContext(Dispatchers.Main) {
                    if (isActive) { 
                        val currentPosition = adapterPosition
                        
                        if (currentPosition != RecyclerView.NO_POSITION && currentPosition < documentList.size) {
                            val currentDocument = documentList[currentPosition]
                            
                            if (currentDocument.elementId == document.elementId) {
                                if (error == null) {
                                    val availableAmount = (amount ?: 0.0).coerceAtLeast(0.0)
                                    currentDocument.mainAmount = availableAmount 
                                    holder.availableQuantity.text = "Доступно: $availableAmount"

                                    
                                    val currentInputAmount = currentDocument.amount
                                    if (currentInputAmount != null && currentInputAmount > availableAmount) {
                                        Log.d("Adapter", "Correcting input amount $currentInputAmount to available $availableAmount")
                                        currentDocument.amount = availableAmount.toInt()
                                        holder.etQuantity.setText(availableAmount.toInt().toString()) 
                                    }
                                } else {
                                    currentDocument.mainAmount = 0.0 
                                    holder.availableQuantity.text = "Доступно: Ошибка"
                                    
                                    currentDocument.amount = 0
                                    holder.etQuantity.setText("0")
                                }
                                onDataChangedListener?.invoke() 
                            } else {
                                Log.w("Adapter", "ViewHolder reused for different elementId before amount loaded.")
                            }
                        } else {
                            Log.w("Adapter", "ViewHolder position invalid ($currentPosition) after amount loaded.")
                        }
                    } else {
                        Log.d("Adapter", "Amount loading job was cancelled before UI update.")
                    }
                }
            }
        }

        
        fun cancelAmountJob() {
            currentAmountJob?.cancel()
            currentAmountJob = null
        }
    }
}