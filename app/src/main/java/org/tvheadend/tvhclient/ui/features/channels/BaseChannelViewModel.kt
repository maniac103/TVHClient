package org.tvheadend.tvhclient.ui.features.channels

import android.app.Application
import android.content.Context
import androidx.lifecycle.MutableLiveData
import org.tvheadend.data.entity.Program
import org.tvheadend.data.entity.RecordingWithChannel
import org.tvheadend.data.entity.ServerProfile
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.base.BaseViewModel
import org.tvheadend.tvhclient.util.extensions.channelDataSource
import org.tvheadend.tvhclient.util.extensions.channelTagDataSource
import org.tvheadend.tvhclient.util.extensions.prefs
import org.tvheadend.tvhclient.util.extensions.programDataSource
import org.tvheadend.tvhclient.util.extensions.recordingDataSource
import org.tvheadend.tvhclient.util.extensions.serverProfileDataSource
import org.tvheadend.tvhclient.util.extensions.serverStatusDataSource
import org.tvheadend.tvhclient.util.livedata.CombinedPairLiveData
import timber.log.Timber
import java.util.*

open class BaseChannelViewModel(private val application: Application) : BaseViewModel(application) {
    val channelTags = CombinedPairLiveData(
        application.channelTagDataSource.getLiveDataItems(),
        application.prefs.showAllChannelTagsLiveData()
    ) { tags, showAllTags -> tags.filter { showAllTags || it.channelCount > 0 } }

    val recordings = application.recordingDataSource.getLiveDataItems()
    val selectedChannelTagIds = application.channelTagDataSource.liveDataSelectedItemIds
    val channelCount = application.channelDataSource.getLiveDataItemCount()
    val selectedTime = MutableLiveData(Date().time)

    fun setSelectedTime(time: Long) {
        if (selectedTime.value != time) {
            selectedTime.value = time
        }
    }

    fun setSelectedChannelTagIds(ids: Set<Int>) {
        val tagValues = channelTags.value ?: HashSet<Int>()
        if (!tagValues.toTypedArray().contentEquals(ids.toTypedArray())) {
            Timber.d("Updating database with newly selected channel tag ids")
            application.channelTagDataSource.updateSelectedChannelTags(ids)
        }
    }

    fun getSelectedChannelTagName(context: Context): String {
        if (selectedChannelTagIds.value == null || channelTags.value == null) {
            Timber.d("No channel tags or selected tag id values exist")
            return context.getString(R.string.unknown)
        }

        Timber.d("Returning name of the selected channel tag")
        val selectedTagIds = selectedChannelTagIds.value ?: ArrayList()
        if (selectedTagIds.size == 1) {
            channelTags.value?.forEach {
                if (selectedTagIds.contains(it.tagId)) {
                    return it.tagName ?: context.getString(R.string.unknown)
                }
            }
            return context.getString(R.string.unknown)
        } else return if (selectedTagIds.isEmpty()) {
            context.getString(R.string.all_channels)
        } else {
            context.getString(R.string.multiple_channel_tags)
        }
    }

    fun getRecordingById(id: Int): RecordingWithChannel? {
        return application.recordingDataSource.getItemByEventId(id)
    }

    fun getRecordingProfile(): ServerProfile? {
        return application.serverProfileDataSource.getItemById(
            application.serverStatusDataSource.activeItem.recordingServerProfileId
        )
    }

    fun getRecordingProfileNames(): Array<String> {
        return application.serverProfileDataSource.recordingProfileNames
    }

    fun getProgramById(id: Int): Program? {
        return application.programDataSource.getItemById(id)
    }
}
