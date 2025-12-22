package org.tvheadend.tvhclient.ui.base

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import org.tvheadend.data.entity.Connection
import org.tvheadend.tvhclient.service.ConnectionService
import org.tvheadend.tvhclient.ui.common.NetworkStatus
import org.tvheadend.tvhclient.ui.common.interfaces.NetworkStatusInterface
import org.tvheadend.tvhclient.ui.common.interfaces.SnackbarMessageInterface
import org.tvheadend.tvhclient.ui.features.MainActivity
import org.tvheadend.tvhclient.util.extensions.connectionDataSource
import org.tvheadend.tvhclient.util.extensions.serverStatusDataSource
import org.tvheadend.tvhclient.util.livedata.Event

open class BaseViewModel(application: Application) : AndroidViewModel(application), SnackbarMessageInterface {
    var startupCompleteLiveData = MutableLiveData<Event<Boolean>>()
        private set

    /**
     * Contains an intent with the snackbar message and other information.
     * The value gets set by the {@link SnackbarMessageReceiver}
     */
    var snackbarMessageLiveData = MutableLiveData<Event<Intent>>()
        private set

    var removeFragmentWhenSearchIsDone = false

    var searchQueryLiveData = MutableLiveData("")
    var searchViewHasFocus = false

    val isSearchActive: Boolean
        get() = !searchQueryLiveData.value.isNullOrEmpty()

    init {
        startupCompleteLiveData.value = Event(false)
    }

    fun updateConnectionAndRestartApplication(context: Context?, isSyncRequired: Boolean = true) {
        context?.let {
            if (isSyncRequired) {
                context.connectionDataSource.setSyncRequiredForActiveConnection()
            }
            context.stopService(Intent(context, ConnectionService::class.java))
            val intent = Intent(context, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)
        }
    }

    fun startSearchQuery(query: String) {
        searchQueryLiveData.value = query
    }

    fun clearSearchQuery() {
        searchQueryLiveData.value = ""
    }

    override fun setSnackbarMessage(intent: Intent) {
        snackbarMessageLiveData.value = Event(intent)
    }

    fun setStartupComplete(isComplete: Boolean) {
        startupCompleteLiveData.value = Event(isComplete)
    }
}