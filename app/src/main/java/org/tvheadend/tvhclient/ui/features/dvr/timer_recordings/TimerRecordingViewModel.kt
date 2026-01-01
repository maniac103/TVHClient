package org.tvheadend.tvhclient.ui.features.dvr.timer_recordings

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.application
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import org.tvheadend.data.entity.Channel
import org.tvheadend.data.entity.ServerProfile
import org.tvheadend.data.entity.TimerRecording
import org.tvheadend.data.entity.TimerRecordingWithChannel
import org.tvheadend.tvhclient.service.ConnectionService
import org.tvheadend.tvhclient.ui.base.BaseViewModel
import org.tvheadend.tvhclient.util.extensions.asStaticLiveData
import org.tvheadend.tvhclient.util.extensions.channelDataSource
import org.tvheadend.tvhclient.util.extensions.prefs
import org.tvheadend.tvhclient.util.extensions.serverProfileDataSource
import org.tvheadend.tvhclient.util.extensions.serverStatusDataSource
import org.tvheadend.tvhclient.util.extensions.timerRecordingDataSource

class TimerRecordingViewModel(application: Application) : BaseViewModel(application) {

    var selectedListPosition = 0
    val currentIdLiveData = MutableLiveData("")
    val recordingLiveData = currentIdLiveData
        .switchMap { id ->
            id.takeIf { it.isNotEmpty() }
                ?.let { application.timerRecordingDataSource.getLiveDataItemById(it) }
                ?: TimerRecordingWithChannel(TimerRecording()).asStaticLiveData()
        }
        .map { rec ->
            isTimeEnabled = rec != null && rec.start > 0 && rec.stop > 0
            rec
        }
    var isTimeEnabled: Boolean = false

    val recordings = application.timerRecordingDataSource.getLiveDataItems()
    var recordingProfileNameId = 0

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
            intent.putExtra("start", 0L)
            intent.putExtra("stop", 0L)
        }
        intent.putExtra("daysOfWeek", recording.daysOfWeek)
        intent.putExtra("priority", recording.priority)
        intent.putExtra("enabled", if (recording.isEnabled) 1 else 0)

        if (recording.channelId > 0) {
            intent.putExtra("channelId", recording.channelId)
        }
        return intent
    }

    fun getChannelList(): List<Channel> {
        return application.channelDataSource.getChannels(application.prefs.channelSortOrder.ordinal)
    }

    fun getRecordingProfileNames(): Array<String> {
        return application.serverProfileDataSource.recordingProfileNames
    }

    fun getRecordingProfile(): ServerProfile? {
        return application.serverProfileDataSource.getItemById(application.serverStatusDataSource.activeItem.timerRecordingServerProfileId)
    }
}
