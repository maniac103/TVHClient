package org.tvheadend.tvhclient.ui.features.dvr.timer_recordings

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.application
import org.tvheadend.data.entity.Channel
import org.tvheadend.data.entity.ServerProfile
import org.tvheadend.data.entity.TimerRecording
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.service.ConnectionService
import org.tvheadend.tvhclient.ui.base.BaseViewModel
import org.tvheadend.tvhclient.util.extensions.channelDataSource
import org.tvheadend.tvhclient.util.extensions.prefs
import org.tvheadend.tvhclient.util.extensions.serverProfileDataSource
import org.tvheadend.tvhclient.util.extensions.serverStatusDataSource
import org.tvheadend.tvhclient.util.extensions.timerRecordingDataSource
import timber.log.Timber
import java.util.*

class TimerRecordingViewModel(application: Application) : BaseViewModel(application) {

    var selectedListPosition = 0
    val currentIdLiveData = MutableLiveData("")
    var recording = TimerRecording()
    var recordingLiveData = MediatorLiveData<TimerRecording>()
    val recordings: LiveData<List<TimerRecording>> = application.timerRecordingDataSource.getLiveDataItems()
    var recordingProfileNameId = 0

    private val defaultChannelSortOrder = application.applicationContext.resources.getString(R.string.pref_default_channel_sort_order)

    /**
     * Returns an intent with the recording data
     */
    fun getIntentData(context: Context, recording: TimerRecording): Intent {
        val intent = Intent(context, ConnectionService::class.java)
        intent.putExtra("directory", recording.directory)
        intent.putExtra("title", recording.title)
        intent.putExtra("name", recording.name)

        if (isTimeEnabled) {
            intent.putExtra("start", recording.start)
            intent.putExtra("stop", recording.stop)
        } else {
            intent.putExtra("start", (0).toLong())
            intent.putExtra("stop", (0).toLong())
        }
        intent.putExtra("daysOfWeek", recording.daysOfWeek)
        intent.putExtra("priority", recording.priority)
        intent.putExtra("enabled", if (recording.isEnabled) 1 else 0)

        if (recording.channelId > 0) {
            intent.putExtra("channelId", recording.channelId)
        }
        return intent
    }

    var isTimeEnabled: Boolean = false
        set(value) {
            field = value
            if (!value) {
                startTimeInMillis = Calendar.getInstance().timeInMillis
                stopTimeInMillis = Calendar.getInstance().timeInMillis
            }
        }

    init {
        recordingLiveData.addSource(currentIdLiveData) { value ->
            if (value.isNotEmpty()) {
                recordingLiveData.value = application.timerRecordingDataSource.getItemById(value)
            }
        }
    }

    fun loadRecordingByIdSync(id: String) {
        recording = application.timerRecordingDataSource.getItemById(id) ?: TimerRecording()
        isTimeEnabled = recording.start > 0 && recording.stop > 0
    }

    var startTimeInMillis: Long = 0
        get() {
            return recording.startTimeInMillis
        }
        set(milliSeconds) {
            field = milliSeconds
            recording.start = getMinutesFromTime(milliSeconds)
        }

    var stopTimeInMillis: Long = 0
        get() {
            return recording.stopTimeInMillis
        }
        set(milliSeconds) {
            field = milliSeconds
            recording.stop = getMinutesFromTime(milliSeconds)
        }

    /**
     * The start and stop time handling is done in milliseconds within the app, but the
     * server requires and provides minutes instead. In case the start and stop times of
     * a recording need to be updated the milliseconds will be converted to minutes.
     */
    private fun getMinutesFromTime(milliSeconds : Long) : Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = milliSeconds
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val minutes = (hour * 60 + minute).toLong()
        Timber.d("Time in millis is $milliSeconds, start minutes are $minutes")
        return minutes
    }

    fun getChannelList(): List<Channel> {
        val channelSortOrder = Integer.valueOf(application.prefs.getString("channel_sort_order", defaultChannelSortOrder) ?: defaultChannelSortOrder)
        return application.channelDataSource.getChannels(channelSortOrder)
    }

    fun getRecordingProfileNames(): Array<String> {
        return application.serverProfileDataSource.recordingProfileNames
    }

    fun getRecordingProfile(): ServerProfile? {
        return application.serverProfileDataSource.getItemById(application.serverStatusDataSource.activeItem.timerRecordingServerProfileId)
    }
}
