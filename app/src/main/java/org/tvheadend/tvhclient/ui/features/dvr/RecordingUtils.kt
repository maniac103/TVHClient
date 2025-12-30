package org.tvheadend.tvhclient.ui.features.dvr

import android.content.Context
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import org.tvheadend.data.entity.Channel
import org.tvheadend.data.entity.ServerProfile
import org.tvheadend.tvhclient.R
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

fun getDateStringFromTimeInMillis(milliSeconds: Long): String {
    val sdf = SimpleDateFormat("dd.MM", Locale.US)
    return sdf.format(milliSeconds)
}

fun getTimeStringFromTimeInMillis(milliSeconds: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.US)
    return sdf.format(milliSeconds)
}

fun showTimePicker(millis: Long): MaterialTimePicker {
    val c = Calendar.getInstance()
    c.timeInMillis = millis
    return MaterialTimePicker.Builder()
        // TODO: clock format
        .setHour(c.get(Calendar.HOUR_OF_DAY))
        .setMinute(c.get(Calendar.MINUTE))
        .build()
}
fun showDatePicker(millis: Long): MaterialDatePicker<Long> {
    return MaterialDatePicker.Builder
        .datePicker()
        .setSelection(millis)
        .build()
}

fun minutesToTimeMillis(minutes: Long): Long {
    if (minutes < 0) {
        return -1L
    }
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    return calendar.timeInMillis + (minutes * 60 * 1000)
}

fun replaceHourAndMinute(millis: Long, hour: Int, minute: Int): Long {
    val c = Calendar.getInstance()
    c.timeInMillis = millis
    c.set(Calendar.HOUR_OF_DAY, hour)
    c.set(Calendar.MINUTE, minute)
    return c.timeInMillis
}

fun handleChannelListSelection(context: Context, channelList: List<Channel>, showAllChannelsListEntry: Boolean, callback: ((Channel) -> Unit)?) {
    // Fill the channel tag adapter with the available channel tags
    val channels = ArrayList(channelList)

    // Add the default channel (all channels)
    // to the list after it has been sorted
    if (showAllChannelsListEntry) {
        val channel = Channel()
        channel.id = 0
        channel.name = context.getString(R.string.all_channels)
        channels.add(0, channel)
    }

    val channelListSelectionAdapter = ChannelListSelectionAdapter(context, channels)
    // Show the dialog that shows all available channel tags. When the
    // user has selected a tag, restart the loader to loadRecordingById the updated channel list
    MaterialAlertDialogBuilder(context)
        .setTitle(R.string.tags)
        .setSingleChoiceItems(channelListSelectionAdapter, -1) { dialog, which ->
            callback?.invoke(channels[which])
            dialog.dismiss()
        }
        .show()
}

fun handlePrioritySelection(context: Context, selectedPriority: Int, callback: ((Int) -> Unit)?) {
    Timber.d("Selected priority is ${if (selectedPriority == 6) 5 else selectedPriority}")
    val priorityNames = context.resources.getStringArray(R.array.dvr_priority_names)
    MaterialAlertDialogBuilder(context)
        .setTitle(R.string.select_priority)
        .setSingleChoiceItems(priorityNames, if (selectedPriority == 6) 5 else selectedPriority) { _, index ->
            Timber.d("New selected priority is ${if (index == 5) 6 else index}")
            callback?.invoke(if (index == 5) 6 else index)
        }
        .show()
}

fun handleRecordingProfileSelection(context: Context, recordingProfilesList: Array<String>, selectedProfile: Int, callback: ((Int) -> Unit)?) {
    MaterialAlertDialogBuilder(context)
        .setTitle(R.string.select_dvr_config)
        .setSingleChoiceItems(recordingProfilesList, selectedProfile) { _, index ->
            callback?.invoke(index)
        }
        .show()
}

fun getSelectedProfileId(profile: ServerProfile?, recordingProfilesList: Array<String>): Int = profile
    ?.let { prof -> recordingProfilesList.indexOfFirst { it == prof.name } }
    ?.takeIf { it >= 0 }
    ?: 0
