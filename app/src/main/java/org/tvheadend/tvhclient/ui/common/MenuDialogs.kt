package org.tvheadend.tvhclient.ui.common

import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.databinding.ViewDataBinding
import androidx.preference.PreferenceManager
import org.tvheadend.data.entity.ChannelTag
import org.tvheadend.tvhclient.BR
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.GenreColorListAdapterBinding
import org.tvheadend.tvhclient.ui.common.interfaces.ChannelTagIdsSelectedInterface
import org.tvheadend.tvhclient.ui.common.interfaces.ChannelTimeSelectedInterface
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.content.edit
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.tvheadend.tvhclient.databinding.ChanneltagListMultipleChoiceAdapterBinding
import org.tvheadend.tvhclient.databinding.ChanneltagListSingleChoiceAdapterBinding


fun showChannelTagSelectionDialog(context: Context, channelTags: MutableList<ChannelTag>, channelCount: Int, callback: ChannelTagIdsSelectedInterface): Boolean {
    val isMultipleChoice = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("multiple_channel_tags_enabled",
            context.resources.getBoolean(R.bool.pref_default_multiple_channel_tags_enabled))

    // Create a default tag (All channels)
    if (!isMultipleChoice) {
        val tag = ChannelTag()
        tag.tagId = 0
        tag.tagName = context.getString(R.string.all_channels)
        tag.channelCount = channelCount
        var allChannelsSelected = true
        for (channelTag in channelTags) {
            if (channelTag.isSelected) {
                allChannelsSelected = false
                break
            }
        }
        tag.isSelected = allChannelsSelected
        channelTags.add(0, tag)
    }

    val adapter = ChannelTagSelectionAdapter(context, channelTags, isMultipleChoice)

    // Show the dialog that shows all available channel tags. When the
    // user has selected a tag, restart the loader to loadRecordingById the updated channel list
    val dialogBuilder = MaterialAlertDialogBuilder(context)
        .setSingleChoiceItems(adapter, -1, null)
    if (isMultipleChoice) {
        dialogBuilder
            .setTitle(R.string.select_multiple_channel_tags)
            .setPositiveButton(R.string.save) { _, _ -> callback.onChannelTagIdsSelected(adapter.selectedTagIds) }
    } else {
         dialogBuilder
             .setTitle(R.string.filter_channel_list)
             .setOnDismissListener { callback.onChannelTagIdsSelected(adapter.selectedTagIds) }
    }

    val dialog = dialogBuilder.show()
    adapter.setCallback(dialog)

    return true
}

class ChannelTagSelectionAdapter(context: Context, private val channelTagList: List<ChannelTag>, private val isMultiChoice: Boolean) :
    ArrayAdapter<ChannelTag>(context, 0, channelTagList) {

    private val selectedChannelTagIds = mutableSetOf<Int>()
    private lateinit var dialog: DialogInterface

    internal val selectedTagIds: Set<Int>
        get() = selectedChannelTagIds

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val binding = if (convertView == null) {
            val inflater = LayoutInflater.from(context)
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(parent.context)
            val showChannelTagIcons = sharedPreferences.getBoolean("channel_tag_icons_enabled",
                parent.context.resources.getBoolean(R.bool.pref_default_channel_tag_icons_enabled))

            val binding = if (isMultiChoice) {
                ChanneltagListMultipleChoiceAdapterBinding.inflate(inflater, parent, false)
            } else {
                ChanneltagListSingleChoiceAdapterBinding.inflate(inflater, parent, false)
            }
            binding.setVariable(BR.callback, this)
            binding.setVariable(BR.showChannelTagIcons, showChannelTagIcons)
            binding
        } else {
            convertView.tag as ViewDataBinding
        }
        binding.root.tag = binding
        val channelTag = getItem(position)
        binding.setVariable(BR.channelTag, channelTag)
        binding.setVariable(BR.position, position)
        if (channelTag?.isSelected == true) {
            selectedChannelTagIds.add(channelTag.tagId)
        }
        return binding.root
    }

    fun setCallback(dialog: DialogInterface) {
        this.dialog = dialog
    }

    fun onChecked(view: View, position: Int, isChecked: Boolean) {
        val tagId = channelTagList[position].tagId
        if (isChecked) {
            selectedChannelTagIds.add(tagId)
        } else {
            selectedChannelTagIds.remove(tagId)
        }
    }

    fun onSelected(position: Int) {
        val tagId = channelTagList[position].tagId
        selectedChannelTagIds.clear()
        if (position != 0) {
            selectedChannelTagIds.add(tagId)
        }
        dialog.dismiss()
    }
}

/**
 * Prepares a dialog that shows the available genre colors and the names. In
 * here the data for the adapter is created and the dialog prepared which
 * can be shown later.
 */
fun showGenreColorDialog(context: Context): Boolean {
    val adapter = GenreColorListAdapter(context, context.resources.getStringArray(R.array.pr_content_type0))
    MaterialAlertDialogBuilder(context)
        .setTitle(R.string.genre_color_list)
        .setSingleChoiceItems(adapter, -1) { _, _ -> }
        .show()
    return true
}

class GenreColorListAdapter internal constructor(context: Context, private val contentInfo: Array<String>) :
    ArrayAdapter<String>(context, R.layout.genre_color_list_adapter, R.id.genre, contentInfo) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val binding = if (convertView == null) {
            GenreColorListAdapterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        } else {
            convertView.tag as GenreColorListAdapterBinding
        }
        binding.root.tag = binding
        getItem(position)?.let { contentInfo ->
            binding.contentType = (position + 1) * 16
            binding.contentName = contentInfo
        }
        return binding.root
    }
}

fun showProgramTimeframeSelectionDialog(context: Context, currentSelection: Int, intervalInHours: Int, maxIntervalsToShow: Int, callback: ChannelTimeSelectedInterface?): DialogInterface {

    val startDateFormat = SimpleDateFormat("dd.MM.yyyy - HH:00", Locale.US)
    val endDateFormat = SimpleDateFormat("HH:00", Locale.US)

    val times = ArrayList<String>()
    times.add(context.getString(R.string.current_time))

    // Set the time that shall be shown next in the dialog. This is the
    // current time plus the value of the intervalInHours in milliseconds
    var timeInMillis = System.currentTimeMillis() + 1000 * 60 * 60 * intervalInHours

    // Add the date and time to the list. Remove Increase the time in
    // milliseconds for each iteration by the defined intervalInHours
    for (i in 1 until maxIntervalsToShow) {
        val startTime = startDateFormat.format(timeInMillis)
        timeInMillis += (1000 * 60 * 60 * intervalInHours).toLong()
        val endTime = endDateFormat.format(timeInMillis)
        times.add("$startTime - $endTime")
    }

    return MaterialAlertDialogBuilder(context)
        .setTitle(R.string.select_time)
        .setSingleChoiceItems(times.toTypedArray(), currentSelection) { _, index ->
            callback?.onTimeSelected(index)
        }
        .show()
}

fun showChannelSortOrderSelectionDialog(context: Context): Boolean {
    val channelSortOrder = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(context).getString("channel_sort_order", context.resources.getString(R.string.pref_default_channel_sort_order))!!)
    MaterialAlertDialogBuilder(context)
        .setTitle(R.string.pref_sort_channels)
        .setSingleChoiceItems(R.array.pref_sort_channels_names, channelSortOrder) { _, index ->
            Timber.d("New selected channel sort order changed from $channelSortOrder to $index")
            PreferenceManager.getDefaultSharedPreferences(context).edit {
                putString("channel_sort_order", index.toString())
            }
        }
        .show()
    return false
}

fun showCompletedRecordingSortOrderSelectionDialog(context: Context): Boolean {
    val sortOrder = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(context).getString("completed_recording_sort_order", context.resources.getString(R.string.pref_default_completed_recording_sort_order))!!)
    MaterialAlertDialogBuilder(context)
        .setTitle(R.string.pref_sort_completed_recordings)
        .setSingleChoiceItems(R.array.pref_sort_completed_recordings_names, sortOrder) { _, index ->
            Timber.d("New selected completed recording sort order changed from $sortOrder to $index")
            PreferenceManager.getDefaultSharedPreferences(context).edit {
                putString("completed_recording_sort_order", index.toString())
            }
        }
        .show()
    return false
}