package org.tvheadend.tvhclient.ui.features.channels

import android.app.Application
import androidx.lifecycle.switchMap
import org.tvheadend.tvhclient.util.extensions.channelDataSource
import org.tvheadend.tvhclient.util.extensions.prefs
import org.tvheadend.tvhclient.util.livedata.CombinedTupleLiveData

class ChannelViewModel(private val application: Application) : BaseChannelViewModel(application) {

    var selectedListPosition = 0
    var selectedTimeOffset = 0

    val channels = CombinedTupleLiveData(
        selectedTime,
        application.prefs.channelSortOrderLiveData(),
        selectedChannelTagIds
    ) { time, sortOrder, tagIds -> Triple(time, sortOrder, tagIds) }
        .switchMap { (time, sortOrder, tagIds) -> application.channelDataSource.getAllChannelsByTime(time, sortOrder.ordinal, tagIds) }

    var showGenreColor = application.prefs.genreColorsForChannelsLiveData()
    var showNextProgramTitle = application.prefs.showNextProgramTitleLiveData()
    var showProgressBar = application.prefs.showProgramProgressLiveData()
    var showProgramSubtitle = application.prefs.showProgramSubtitleLiveData()
    val showChannelName = application.prefs.showChannelNameLiveData()
    val showChannelNumber = application.prefs.showChannelNumbersLiveData()
}
