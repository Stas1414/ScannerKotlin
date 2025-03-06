package com.example.scannerkotlin.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.scannerkotlin.model.Product

class ProductAdapter(context: Context, products: MutableList<Product>) : ArrayAdapter<Product>(context, 0, products) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val product = getItem(position)
        val view = convertView ?: LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_2, parent, false)

        val text1 = view.findViewById<TextView>(android.R.id.text1)
        val text2 = view.findViewById<TextView>(android.R.id.text2)

        product?.let {
            text1.text = it.data
            text2.text = it.symbology
        }

        return view
    }

}