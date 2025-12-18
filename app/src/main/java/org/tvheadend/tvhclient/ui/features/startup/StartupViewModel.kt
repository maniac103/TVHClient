package org.tvheadend.tvhclient.ui.features.startup

import android.app.Application
import androidx.lifecycle.*
import org.tvheadend.tvhclient.util.extensions.connectionDataSource
import org.tvheadend.tvhclient.util.livedata.CombinedPairLiveData

open class StartupViewModel(application: Application) : AndroidViewModel(application) {
    val connectionStatus = CombinedPairLiveData(
        application.connectionDataSource.getLiveDataItemCount(),
        application.connectionDataSource.liveDataActiveItem
    ) { count, active ->
        count to (active?.isActive == true)
    }
}