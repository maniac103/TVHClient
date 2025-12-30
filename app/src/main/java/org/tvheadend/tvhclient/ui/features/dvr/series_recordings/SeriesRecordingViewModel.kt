package org.tvheadend.tvhclient.ui.features.dvr.series_recordings

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.application
import androidx.lifecycle.map
import org.tvheadend.data.entity.Channel
import org.tvheadend.data.entity.SeriesRecording
import org.tvheadend.data.entity.SeriesRecordingWithChannel
import org.tvheadend.data.entity.ServerProfile
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.service.ConnectionService
import org.tvheadend.tvhclient.ui.base.BaseViewModel
import org.tvheadend.tvhclient.util.extensions.channelDataSource
import org.tvheadend.tvhclient.util.extensions.prefs
import org.tvheadend.tvhclient.util.extensions.seriesRecordingDataSource
import org.tvheadend.tvhclient.util.extensions.serverProfileDataSource
import org.tvheadend.tvhclient.util.extensions.serverStatusDataSource
import timber.log.Timber

class SeriesRecordingViewModel(application: Application) : BaseViewModel(application) {

    var selectedListPosition = 0
    val currentIdLiveData = MutableLiveData("")
    val recordingLiveData = currentIdLiveData
        .map { id ->
            val rec = id
                .takeIf { it.isNotEmpty() }
                ?.let { application.seriesRecordingDataSource.getItemById(it) }
                ?: SeriesRecordingWithChannel(SeriesRecording())
            isTimeEnabled = rec.start >= 0 && rec.startWindow >= 0
            rec
        }

    var isTimeEnabled: Boolean = false

    val recordings = application.seriesRecordingDataSource.getLiveDataItems()
    var recordingProfileNameId = 0

    var duplicateDetectionList = application.resources.getStringArray(R.array.duplicate_detection_list)

    /**
     * Returns an intent with the recording data
     */
    fun getIntentData(context: Context, recording: SeriesRecording): Intent {
        val intent = Intent(context, ConnectionService::class.java)
        intent.putExtra("title", recording.title)
        intent.putExtra("name", recording.name)
        intent.putExtra("directory", recording.directory)
        intent.putExtra("minDuration", recording.minDuration * 60)
        intent.putExtra("maxDuration", recording.maxDuration * 60)

        // Assume no start time is specified if 0:00 is selected
        if (isTimeEnabled) {
            Timber.d("Intent Recording start time is ${recording.start}")
            Timber.d("Intent Recording startWindow time is ${recording.startWindow}")
            intent.putExtra("start", recording.start)
            intent.putExtra("startWindow", recording.startWindow)
        } else {
            intent.putExtra("start", -1L)
            intent.putExtra("startWindow", -1L)
        }
        intent.putExtra("startExtra", recording.startExtra)
        intent.putExtra("stopExtra", recording.stopExtra)
        intent.putExtra("dupDetect", recording.dupDetect)
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
        return application.serverProfileDataSource.getItemById(application.serverStatusDataSource.activeItem.seriesRecordingServerProfileId)
    }
}
