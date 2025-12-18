package org.tvheadend.tvhclient.ui.features.settings

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.application
import org.tvheadend.data.entity.Channel
import org.tvheadend.data.entity.Connection
import org.tvheadend.data.entity.ServerProfile
import org.tvheadend.data.entity.ServerStatus
import org.tvheadend.data.source.MiscDataSource
import org.tvheadend.tvhclient.ui.common.interfaces.SnackbarMessageInterface
import org.tvheadend.tvhclient.util.extensions.channelDataSource
import org.tvheadend.tvhclient.util.extensions.connectionDataSource
import org.tvheadend.tvhclient.util.extensions.miscDataSource
import org.tvheadend.tvhclient.util.extensions.prefs
import org.tvheadend.tvhclient.util.extensions.serverProfileDataSource
import org.tvheadend.tvhclient.util.extensions.serverStatusDataSource
import org.tvheadend.tvhclient.util.livedata.Event
import timber.log.Timber

class SettingsViewModel(application: Application) : AndroidViewModel(application), SnackbarMessageInterface {
    private val sharedPreferences = application.prefs

    /**
     * The currently active connection. It is also used to hold the current
     * data when a new connection is added or an existing is edited
     */
    var connectionToEdit: Connection

    /**
     * Contains the id of the connection that shall be edited otherwise -1.
     */
    var connectionIdToBeEdited: Int = -1

    /**
     * The number of available connections as live data
     */
    var connectionCountLiveData: LiveData<Int>

    /**
     * Currently active connection as live data
     */
    var activeConnectionLiveData: LiveData<Connection?>

    /**
     * Contains the list of all available connections as live data
     */
    var connectionListLiveData: LiveData<List<Connection>>

    /**
     *  Contains the currently active server information like the selected playback and recording profile ids or the name and disc space information.
     *  This variable should be updated whenever the @{link currentServerStatusLiveData} variable changes to have the latest values
     */
    var currentServerStatus: ServerStatus

    /**
     *  Live data status of the active server status. The activity need to observe it so that whenever the user changes
     *  the profile or clears the database the {@see currentServerStatus} variable can be updated.
     *  In this way the other setting screens will always have access to the latest values.
     */
    var currentServerStatusLiveData: LiveData<ServerStatus?>

    /**
     * Contains a string with the name of the fragment that shall be shown
     */
    private val navigationMenuIdLiveData = MutableLiveData(Event("default"))

    /**
     * Contains an intent with the snackbar message and other information.
     * The value gets set by the {@link SnackbarMessageReceiver}
     */
    var snackbarMessageLiveData = MutableLiveData<Event<Intent>>()
        private set

    init {
        connectionToEdit = application.connectionDataSource.activeItem
        activeConnectionLiveData = application.connectionDataSource.liveDataActiveItem
        connectionCountLiveData = application.connectionDataSource.getLiveDataItemCount()
        connectionListLiveData = application.connectionDataSource.getLiveDataItems()
        currentServerStatus = application.serverStatusDataSource.activeItem
        currentServerStatusLiveData = application.serverStatusDataSource.liveDataActiveItem
    }

    fun getNavigationMenuId(): LiveData<Event<String>> = navigationMenuIdLiveData

    fun setNavigationMenuId(id: String) {
        Timber.d("Received new navigation id $id")
        navigationMenuIdLiveData.value = Event(id)
    }

    fun getChannelList(): List<Channel> {
        return application.channelDataSource.getChannels(application.prefs.channelSortOrder.ordinal)
    }

    /**
     * Updates the connection with the information that a new sync is required.
     */
    fun setSyncRequiredForActiveConnection() {
        Timber.d("Updating active connection to request a full sync")
        application.connectionDataSource.setSyncRequiredForActiveConnection()
    }

    /**
     * Clear the database contents, when done the callback
     * is triggered which will restart the application
     */
    fun clearDatabase(callback: MiscDataSource.DatabaseClearedCallback) {
        application.miscDataSource.clearDatabase(callback)
    }

    fun updateServerStatus(serverStatus: ServerStatus) {
        application.serverStatusDataSource.updateItem(serverStatus)
    }

    fun getHtspProfile(): ServerProfile? {
        return application.serverProfileDataSource.getItemById(currentServerStatus.htspPlaybackServerProfileId)
    }

    fun getHtspProfiles(): List<ServerProfile> {
        val profiles = application.serverProfileDataSource.htspPlaybackProfiles
        Timber.d("Loaded ${profiles.size} Htsp profiles")
        return profiles
    }

    fun getHttpProfile(): ServerProfile? {
        return application.serverProfileDataSource.getItemById(currentServerStatus.httpPlaybackServerProfileId)
    }

    fun getHttpProfiles(): List<ServerProfile> {
        val profiles = application.serverProfileDataSource.httpPlaybackProfiles
        Timber.d("Loaded ${profiles.size} Http profiles")
        return profiles
    }

    fun getRecordingProfile(): ServerProfile? {
        return application.serverProfileDataSource.getItemById(currentServerStatus.recordingServerProfileId)
    }

    fun getSeriesRecordingProfile(): ServerProfile? {
        return application.serverProfileDataSource.getItemById(currentServerStatus.seriesRecordingServerProfileId)
    }

    fun getTimerRecordingProfile(): ServerProfile? {
        return application.serverProfileDataSource.getItemById(currentServerStatus.timerRecordingServerProfileId)
    }

    fun getRecordingProfiles(): List<ServerProfile> {
        val profiles = application.serverProfileDataSource.recordingProfiles
        Timber.d("Loaded ${profiles.size} recording profiles")
        return profiles
    }

    fun getCastingProfile(): ServerProfile? {
        return application.serverProfileDataSource.getItemById(currentServerStatus.castingServerProfileId)
    }

    fun addConnection() {
        application.connectionDataSource.addItem(connectionToEdit)
    }

    fun loadConnectionById(id: Int) {
        connectionToEdit = application.connectionDataSource.getItemById(id) ?: Connection()
    }

    fun updateConnection(connection: Connection) {
        application.connectionDataSource.updateItem(connection)
    }

    fun updateConnection() {
        application.connectionDataSource.updateItem(connectionToEdit)
    }

    fun removeConnection(connection: Connection) {
        application.connectionDataSource.removeItem(connection)
    }

    override fun setSnackbarMessage(intent: Intent) {
        snackbarMessageLiveData.value = Event(intent)
    }
}
