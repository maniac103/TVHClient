package org.tvheadend.tvhclient.ui.features.dvr.recordings

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import org.tvheadend.data.entity.Channel
import org.tvheadend.data.entity.Recording
import org.tvheadend.data.entity.RecordingWithChannel
import org.tvheadend.data.entity.ServerProfile
import org.tvheadend.tvhclient.service.ConnectionService
import org.tvheadend.tvhclient.ui.base.BaseViewModel
import org.tvheadend.tvhclient.util.extensions.asStaticLiveData
import org.tvheadend.tvhclient.util.extensions.channelDataSource
import org.tvheadend.tvhclient.util.extensions.prefs
import org.tvheadend.tvhclient.util.extensions.recordingDataSource
import org.tvheadend.tvhclient.util.extensions.serverProfileDataSource
import org.tvheadend.tvhclient.util.extensions.serverStatusDataSource
import timber.log.Timber

class RecordingViewModel(private val application: Application) : BaseViewModel(application) {

    var selectedListPosition = 0
    val currentIdLiveData = MutableLiveData(0)
    val completedRecordings = application.prefs.completedRecordingSortOrderLiveData().switchMap { value ->
        application.recordingDataSource.getCompletedRecordings(value.ordinal)
    }
    val scheduledRecordings = application.prefs.hideDuplicateScheduledRecordingsLiveData().switchMap { value ->
        Timber.d("Loading scheduled recordings because the duplicate setting has changed")
        application.recordingDataSource.getScheduledRecordings(value)
    }
    val failedRecordings = application.recordingDataSource.getFailedRecordings()
    val removedRecordings = application.recordingDataSource.getRemovedRecordings()

    val showGenreColor = application.prefs.genreColorsForRecordingsLiveData()
    val showFileStatus = application.prefs.showRecordingFileStatusLiveData()
    var recordingLiveData = currentIdLiveData
        .switchMap { id ->
            id.takeIf { it > 0 }
                ?.let { application.recordingDataSource.getLiveDataItemById(it) }
                ?: RecordingWithChannel(Recording()).asStaticLiveData()
        }

    var recordingProfileNameId = 0

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

    fun getChannelList(): List<Channel> {
        return application.channelDataSource.getChannels(application.prefs.channelSortOrder.ordinal)
    }

    fun getRecordingProfileNames(): Array<String> {
        return application.serverProfileDataSource.recordingProfileNames
    }

    fun getRecordingProfile(): ServerProfile? {
        return application.serverProfileDataSource.getItemById(
            application.serverStatusDataSource.activeItem.recordingServerProfileId
        )
    }
}
