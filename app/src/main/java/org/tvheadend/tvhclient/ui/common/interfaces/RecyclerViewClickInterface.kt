package org.tvheadend.tvhclient.ui.common.interfaces

import android.view.View

interface RecyclerViewClickInterface<T> {
    fun onClick(view: View, position: Int, item: T)
    fun onLongClick(view: View, position: Int, item: T): Boolean
}
