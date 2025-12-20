package org.tvheadend.data.source

import androidx.lifecycle.LiveData

interface DataSourceInterface<IT, PT> {

    fun getLiveDataItemCount(): LiveData<Int>

    fun getLiveDataItems(): LiveData<List<PT>>

    fun getLiveDataItemById(id: Any): LiveData<PT>

    fun getItems(): List<PT>

    fun getItemById(id: Any): PT?

    fun addItem(item: IT)

    fun updateItem(item: IT)

    fun removeItem(item: IT)
}
