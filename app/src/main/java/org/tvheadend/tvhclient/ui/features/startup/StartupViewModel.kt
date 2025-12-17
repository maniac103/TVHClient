package org.tvheadend.tvhclient.ui.features.startup

import android.app.Application
import androidx.core.util.Pair
import androidx.lifecycle.*
import org.tvheadend.data.entity.Connection
import org.tvheadend.tvhclient.util.extensions.connectionDataSource

open class StartupViewModel(application: Application) : AndroidViewModel(application) {
    private var connectionCount: LiveData<Int>
    private var connectionLiveData: LiveData<Connection?>
    var connectionStatus: LiveData<Pair<Int, Boolean>>

    init {
        connectionCount = application.connectionDataSource.getLiveDataItemCount()
        connectionLiveData = application.connectionDataSource.liveDataActiveItem

        connectionStatus = ConnectionStatusLiveData(connectionCount, connectionLiveData).switchMap { value ->
            val count = value.first ?: 0
            val connection = value.second
            return@switchMap MutableLiveData(Pair(count, connection?.isActive ?: false))
        }
    }

    internal class ConnectionStatusLiveData(connectionCount: LiveData<Int>,
                                            activeConnection: LiveData<Connection?>) : MediatorLiveData<Pair<Int, Connection>>() {
        init {
            addSource(connectionCount) { count ->
                value = Pair.create(count, activeConnection.value)
            }
            addSource(activeConnection) { connection ->
                value = Pair.create(connectionCount.value, connection)
            }
        }
    }
}