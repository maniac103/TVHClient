package org.tvheadend.tvhclient.ui.common

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import org.tvheadend.api.ConnectionStateResult
import org.tvheadend.tvhclient.service.SyncStateReceiver
import org.tvheadend.tvhclient.service.SyncStateResult
import org.tvheadend.tvhclient.ui.common.interfaces.NetworkStatusInterface
import org.tvheadend.tvhclient.util.extensions.connectionDataSource
import org.tvheadend.tvhclient.util.extensions.serverStatusDataSource
import org.tvheadend.tvhclient.util.livedata.Event

class GlobalStatusViewModel(application: Application) : AndroidViewModel(application), NetworkStatusInterface, SyncStateReceiver.Listener {
    private val networkStatusLiveDataInternal = MutableLiveData(Event(NetworkStatus.NETWORK_UNKNOWN))
    private val connectionToServerAvailableLiveDataInternal = MutableLiveData(false)

    val connectionLiveData = application.connectionDataSource.liveDataActiveItem
    val networkStatusLiveData: LiveData<Event<NetworkStatus>> = networkStatusLiveDataInternal
    val connectionToServerAvailableLiveData: LiveData<Boolean> = connectionToServerAvailableLiveDataInternal
    val htspVersionLiveData = application.serverStatusDataSource.liveDataActiveItem.map { it?.htspVersion ?: 0 }

    override fun setNetworkStatus(status: NetworkStatus) {
        networkStatusLiveDataInternal.value = Event(status)
        if (status == NetworkStatus.NETWORK_IS_DOWN) {
            connectionToServerAvailableLiveDataInternal.value = false
        }
    }

    override fun getNetworkStatus(): NetworkStatus? = networkStatusLiveData.value?.peekContent()

    override fun onSyncStateChanged(result: SyncStateResult) {
        if (result is SyncStateResult.Connecting) {
            when (result.reason) {
                is ConnectionStateResult.Closed -> {
                    connectionToServerAvailableLiveDataInternal.value = false
                }
                is ConnectionStateResult.Connected -> {
                    connectionToServerAvailableLiveDataInternal.value = true
                }
                is ConnectionStateResult.Failed -> {
                    connectionToServerAvailableLiveDataInternal.value = false
                }
                else -> {}
            }
        }
    }
}