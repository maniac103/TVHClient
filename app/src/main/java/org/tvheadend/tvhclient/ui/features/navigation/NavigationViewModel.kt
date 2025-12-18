package org.tvheadend.tvhclient.ui.features.navigation

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.tvheadend.tvhclient.ui.base.BaseViewModel
import org.tvheadend.tvhclient.ui.features.navigation.NavigationDrawer.Companion.MENU_SETTINGS
import org.tvheadend.tvhclient.util.extensions.connectionDataSource
import org.tvheadend.tvhclient.util.extensions.prefs
import org.tvheadend.tvhclient.util.livedata.Event
import timber.log.Timber

class NavigationViewModel(private val application: Application) : BaseViewModel(application) {

    val connections = application.connectionDataSource.getItems()
    val connectionLiveData = application.connectionDataSource.liveDataActiveItem
    private val navigationMenuId = MutableLiveData<Event<Long>>()
    var currentNavigationMenuId: Long

    init {
        Timber.d("Initializing")
        currentNavigationMenuId = application.prefs.startScreenMenuId
        navigationMenuId.value = Event(currentNavigationMenuId)
    }

    fun getNavigationMenuId(): LiveData<Event<Long>> = navigationMenuId

    fun setNavigationMenuId(id: Long) {
        Timber.d("Received new navigation id $id, previous navigation id is ${navigationMenuId.value?.peekContent()}")
        if (currentNavigationMenuId != id || id == MENU_SETTINGS) {
            Timber.d("Setting navigation id to $id")
            currentNavigationMenuId = id
            navigationMenuId.value = Event(id)
        }
    }

    fun setSelectedConnectionAsActive(id: Int): Boolean {
        val currentlyActiveConnection = application.connectionDataSource.activeItem
        val newActiveConnection = application.connectionDataSource.getItemById(id)

        Timber.d("Switching connection from ${currentlyActiveConnection.name} with id ${currentlyActiveConnection.id} to ${newActiveConnection?.name} with id ${newActiveConnection?.id}")

        if (newActiveConnection != null && newActiveConnection.id != currentlyActiveConnection.id) {
            application.connectionDataSource.switchActiveConnection(currentlyActiveConnection.id, newActiveConnection.id)
            Timber.d("Switched active connection from ${currentlyActiveConnection.name} to ${newActiveConnection.name} (db version is ${application.connectionDataSource.activeItem.name})")
            return true
        }
        return false
    }

    fun setSelectedMenuItemId(id: Long) {
        Timber.d("Back button was pressed, setting current navigation id ${navigationMenuId.value} to id $id")
        currentNavigationMenuId = id
    }
}