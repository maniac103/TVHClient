package org.tvheadend.tvhclient.util.livedata

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer

class CombinedPairLiveData<T1, T2, O>(source1: LiveData<T1>, source2: LiveData<T2>, private val combine: (data1: T1, data2: T2) -> O) : MediatorLiveData<O>() {
    private var data1: ValueHolder<T1>? = null
    private var data2: ValueHolder<T2>? = null

    init {
        super.addSource(source1) {
            data1 = ValueHolder(it)
            combineIfPossible()
        }
        super.addSource(source2) {
            data2 = ValueHolder(it)
            combineIfPossible()
        }
    }

    private fun combineIfPossible() {
        val d1 = data1 ?: return
        val d2 = data2 ?: return
        value = combine(d1.value, d2.value)
    }

    override fun <T : Any?> addSource(source: LiveData<T>, onChanged: Observer<in T>) {
        throw UnsupportedOperationException()
    }

    override fun <T : Any?> removeSource(toRemote: LiveData<T>) {
        throw UnsupportedOperationException()
    }

    private data class ValueHolder<T>(val value: T)
}
