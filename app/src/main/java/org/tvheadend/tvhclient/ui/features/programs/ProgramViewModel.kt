package org.tvheadend.tvhclient.ui.features.programs

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.application
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import org.tvheadend.data.entity.ServerProfile
import org.tvheadend.tvhclient.ui.base.BaseViewModel
import org.tvheadend.tvhclient.util.extensions.filter
import org.tvheadend.tvhclient.util.extensions.prefs
import org.tvheadend.tvhclient.util.extensions.programDataSource
import org.tvheadend.tvhclient.util.extensions.recordingDataSource
import org.tvheadend.tvhclient.util.extensions.serverProfileDataSource
import org.tvheadend.tvhclient.util.extensions.serverStatusDataSource
import org.tvheadend.tvhclient.util.livedata.CombinedPairLiveData
import java.util.Date

class ProgramViewModel(application: Application) : BaseViewModel(application) {

    val eventIdLiveData = MutableLiveData(0)
    val channelIdLiveData = MutableLiveData(0)
    val selectedTimeLiveData = MutableLiveData(Date().time)

    val program = eventIdLiveData
        .filter { it > 0 }
        .map { application.programDataSource.getItemById(it) }

    val programs = CombinedPairLiveData(channelIdLiveData, selectedTimeLiveData) { channelId, selectedTime -> channelId to selectedTime }
        .switchMap { (channelId, selectedTime) ->
            if (channelId == 0) {
                application.programDataSource.getLiveDataItemsFromTime(selectedTime)
            } else {
                application.programDataSource.getLiveDataItemByChannelIdAndTime(channelId, selectedTime)
            }
        }

    val recordings = channelIdLiveData.switchMap { channelId ->
        if (channelId == 0) {
            application.recordingDataSource.getLiveDataItems()
        } else {
            application.recordingDataSource.getLiveDataItemsByChannelId(channelId)
        }
    }

    var selectedTime: Long = System.currentTimeMillis()
    var eventId = 0
    var channelId = 0
    var channelName = ""
    var showProgramChannelIcon = false

    val showGenreColor = application.prefs.genreColorsForProgramsLiveData()
    val showProgramSubtitles = application.prefs.showProgramSubtitleLiveData()
    val showProgramArtwork = application.prefs.showProgramArtworkLiveData()

    fun getRecordingProfile(): ServerProfile? =
        application.serverProfileDataSource.getItemById(
            application.serverStatusDataSource.activeItem.recordingServerProfileId
        )

    fun getRecordingProfileNames(): Array<String> =
        application.serverProfileDataSource.recordingProfileNames
}
