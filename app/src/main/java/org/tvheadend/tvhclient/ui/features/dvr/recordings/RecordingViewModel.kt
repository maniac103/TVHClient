package org.tvheadend.tvhclient.ui.features.dvr.recordings

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import org.tvheadend.data.entity.Channel
import org.tvheadend.data.entity.Recording
import org.tvheadend.data.entity.ServerProfile
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.service.ConnectionService
import org.tvheadend.tvhclient.ui.base.BaseViewModel
import org.tvheadend.tvhclient.util.extensions.channelDataSource
import org.tvheadend.tvhclient.util.extensions.prefs
import org.tvheadend.tvhclient.util.extensions.recordingDataSource
import org.tvheadend.tvhclient.util.extensions.serverProfileDataSource
import org.tvheadend.tvhclient.util.extensions.serverStatusDataSource
import timber.log.Timber

class RecordingViewModel(private val application: Application) : BaseViewModel(application), SharedPreferences.OnSharedPreferenceChangeListener {

    var selectedListPosition = 0
    val currentIdLiveData = MutableLiveData(0)
    val completedRecordings: LiveData<List<Recording>>
    val scheduledRecordings: LiveData<List<Recording>>
    val failedRecordings: LiveData<List<Recording>>
    val removedRecordings: LiveData<List<Recording>>

    var showGenreColor = MutableLiveData<Boolean>()
    var recordingLiveData = MediatorLiveData<Recording>()
    var recording = Recording()
    var recordingProfileNameId = 0

    private val completedRecordingSortOrder = MutableLiveData<Int>()
    private var hideDuplicateScheduledRecordings: MutableLiveData<Boolean> = MutableLiveData()

    private val defaultChannelSortOrder = application.applicationContext.resources.getString(R.string.pref_default_channel_sort_order)
    private val defaultShowGenreColor = application.resources.getBoolean(R.bool.pref_default_genre_colors_for_recordings_enabled)
    private val defaultCompletedRecordingSortOrder = application.applicationContext.resources.getString(R.string.pref_default_completed_recording_sort_order)
    private val defaultHideDuplicateScheduledRecordings = application.resources.getBoolean(R.bool.pref_default_hide_duplicate_scheduled_recordings_enabled)

    fun getIntentData(context: Context, recording: Recording): Intent {
        val intent = Intent(context, ConnectionService::class.java)
        intent.putExtra("title", recording.title)
        intent.putExtra("subtitle", recording.subtitle)
        intent.putExtra("summary", recording.summary)
        intent.putExtra("description", recording.description)
        intent.putExtra("stop", recording.stop / 1000)
        intent.putExtra("stopExtra", recording.stopExtra)

        if (!recording.isRecording) {
            intent.putExtra("channelId", recording.channelId)
            intent.putExtra("start", recording.start / 1000)
            intent.putExtra("startExtra", recording.startExtra)
            intent.putExtra("priority", recording.priority)
            intent.putExtra("enabled", if (recording.isEnabled) 1 else 0)
        }
        return intent
    }

    init {
        onSharedPreferenceChanged(application.prefs, "hide_duplicate_scheduled_recordings_enabled")
        onSharedPreferenceChanged(application.prefs, "completed_recording_sort_order")
        onSharedPreferenceChanged(application.prefs, "genre_colors_for_recordings_enabled")

        recordingLiveData.addSource(currentIdLiveData) { value ->
            if (value > 0) {
                recordingLiveData.value = application.recordingDataSource.getItemById(value)
            }
        }

        scheduledRecordings = ScheduledRecordingLiveData(hideDuplicateScheduledRecordings).switchMap { value ->
            Timber.d("Loading scheduled recordings because the duplicate setting has changed")
            if (value == null) {
                Timber.d("Skipping loading of scheduled recordings because the duplicate setting is not set")
                return@switchMap null
            }
            return@switchMap application.recordingDataSource.getScheduledRecordings(value)
        }
        completedRecordings = CompletedRecordingLiveData(completedRecordingSortOrder).switchMap { value ->
            if (value == null) {
                Timber.d("Not loading of completed recordings because no recording sort order is set")
                return@switchMap null
            }
            return@switchMap application.recordingDataSource.getCompletedRecordings(value)
        }
        failedRecordings = application.recordingDataSource.getFailedRecordings()
        removedRecordings = application.recordingDataSource.getRemovedRecordings()

        Timber.d("Registering shared preference change listener")
        application.prefs.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onCleared() {
        Timber.d("Unregistering shared preference change listener")
        application.prefs.unregisterOnSharedPreferenceChangeListener(this)
        super.onCleared()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        Timber.d("Shared preference $key has changed")
        if (sharedPreferences == null) return
        when (key) {
            "genre_colors_for_recordings_enabled" -> showGenreColor.value = sharedPreferences.getBoolean(key,defaultShowGenreColor)
            "completed_recording_sort_order" -> completedRecordingSortOrder.value = Integer.valueOf(sharedPreferences.getString("completed_recording_sort_order", defaultCompletedRecordingSortOrder) ?: defaultCompletedRecordingSortOrder)
            "hide_duplicate_scheduled_recordings_enabled" -> hideDuplicateScheduledRecordings.value = sharedPreferences.getBoolean(key, defaultHideDuplicateScheduledRecordings)
        }
    }

    fun loadRecordingByIdSync(id: Int) {
        recording = application.recordingDataSource.getItemById(id) ?: Recording()
    }

    fun getChannelList(): List<Channel> {
        val channelSortOrder = Integer.valueOf(application.prefs.getString("channel_sort_order", defaultChannelSortOrder)
                ?: defaultChannelSortOrder)
        return application.channelDataSource.getChannels(channelSortOrder)
    }

    fun getRecordingProfileNames(): Array<String> {
        return application.serverProfileDataSource.recordingProfileNames
    }

    fun getRecordingProfile(): ServerProfile? {
        return application.serverProfileDataSource.getItemById(
            application.serverStatusDataSource.activeItem.recordingServerProfileId
        )
    }

    internal class ScheduledRecordingLiveData(hideDuplicates: LiveData<Boolean>) : MediatorLiveData<Boolean>() {
        init {
            addSource(hideDuplicates) { hide ->
                value = hide
            }
        }
    }

    internal class CompletedRecordingLiveData(selectedChannelSortOrder: LiveData<Int>) : MediatorLiveData<Int?>() {
        init {
            addSource(selectedChannelSortOrder) { order ->
                value = order
            }
        }
    }
}
