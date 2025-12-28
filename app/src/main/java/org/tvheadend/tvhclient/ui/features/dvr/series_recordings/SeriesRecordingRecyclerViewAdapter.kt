package org.tvheadend.tvhclient.ui.features.dvr.series_recordings

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import org.tvheadend.data.ServerCapabilities
import org.tvheadend.data.entity.SeriesRecordingWithChannel
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.SeriesRecordingListAdapterBinding
import org.tvheadend.tvhclient.ui.common.interfaces.RecyclerViewClickInterface
import org.tvheadend.tvhclient.util.extensions.applyIcon
import org.tvheadend.tvhclient.util.extensions.applyText
import org.tvheadend.tvhclient.util.extensions.applyTextAndAdjustVisibility
import org.tvheadend.tvhclient.util.extensions.determineDaysOfWeekText
import org.tvheadend.tvhclient.util.extensions.formatStartStopTime

class SeriesRecordingRecyclerViewAdapter internal constructor(
    private val isDualPane: Boolean,
    private val clickCallback: RecyclerViewClickInterface<SeriesRecordingWithChannel>,
    htspVersion: Int
) : RecyclerView.Adapter<SeriesRecordingRecyclerViewAdapter.SeriesRecordingViewHolder>(), Filterable {
    private val caps = ServerCapabilities(htspVersion)
    private val recordingList = ArrayList<SeriesRecordingWithChannel>()
    private var recordingListFiltered: MutableList<SeriesRecordingWithChannel> = ArrayList()
    private var selectedPosition = 0

    val items: List<SeriesRecordingWithChannel>
        get() = recordingListFiltered

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeriesRecordingViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val itemBinding = SeriesRecordingListAdapterBinding.inflate(layoutInflater, parent, false)
        return SeriesRecordingViewHolder(itemBinding, isDualPane)
    }

    override fun onBindViewHolder(holder: SeriesRecordingViewHolder, position: Int) {
        if (recordingListFiltered.size > position) {
            val recording = recordingListFiltered[position]
            holder.bind(recording, position, selectedPosition == position, caps, clickCallback)
        }
    }

    override fun onBindViewHolder(holder: SeriesRecordingViewHolder, position: Int, payloads: List<Any>) {
        onBindViewHolder(holder, position)
    }

    internal fun addItems(newItems: List<SeriesRecordingWithChannel>) {
        recordingList.clear()
        recordingListFiltered.clear()
        recordingList.addAll(newItems)
        recordingListFiltered.addAll(newItems)

        if (selectedPosition > recordingListFiltered.size) {
            selectedPosition = 0
        }
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return recordingListFiltered.size
    }

    override fun getItemViewType(position: Int): Int {
        return R.layout.series_recording_list_adapter
    }

    fun setPosition(pos: Int) {
        notifyItemChanged(selectedPosition)
        selectedPosition = pos
        notifyItemChanged(pos)
    }

    fun getItem(position: Int): SeriesRecordingWithChannel? {
        return if (recordingListFiltered.size > position && position >= 0) {
            recordingListFiltered[position]
        } else {
            null
        }
    }

    override fun getFilter(): Filter = object : Filter() {
        override fun performFiltering(charSequence: CharSequence): FilterResults {
            val charString = charSequence.toString().lowercase()
            val filteredList = if (charString.isNotEmpty()) {
                recordingList.filter { rec ->
                    rec.title?.lowercase()?.contains(charString) == true ||
                            rec.name?.lowercase()?.contains(charString) == true
                }
            } else {
                ArrayList(recordingList)
            }

            val filterResults = FilterResults()
            filterResults.values = filteredList
            return filterResults
        }

        override fun publishResults(charSequence: CharSequence, filterResults: FilterResults) {
            recordingListFiltered.clear()
            @Suppress("UNCHECKED_CAST")
            recordingListFiltered.addAll(filterResults.values as ArrayList<SeriesRecordingWithChannel>)
            notifyDataSetChanged()
        }
    }

    class SeriesRecordingViewHolder(
        private val binding: SeriesRecordingListAdapterBinding,
        private val isDualPane: Boolean
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            recording: SeriesRecordingWithChannel,
            position: Int,
            isSelected: Boolean,
            caps: ServerCapabilities,
            clickCallback: RecyclerViewClickInterface<SeriesRecordingWithChannel>
        ) {
            binding.root.apply {
                setOnClickListener { clickCallback.onClick(it, position, recording) }
                setOnLongClickListener { clickCallback.onLongClick(it, position, recording) }
            }
            binding.title.text = recording.title
            binding.name.applyTextAndAdjustVisibility(recording.name)
            binding.channel.applyText { recording.channelName ?: getString(R.string.all_channels) }
            binding.duration.applyText { getString(R.string.minutes, recording.duration) }
            binding.daysOfWeek.applyText { determineDaysOfWeekText(recording.daysOfWeek) }
            binding.startStop.applyText {
                formatStartStopTime(
                    if (recording.start < 0) recording.start else recording.startTimeInMillis,
                    if (recording.startWindow < 0) recording.startWindow else recording.startWindowTimeInMillis
                )
            }
            binding.icon.applyIcon(recording.channelIcon, recording.channelName, binding.iconText)
            binding.disabled.isVisible = caps.recordingEnabledSupported && !recording.isEnabled
            binding.dualPaneListItemSelection.isVisible = isDualPane && isSelected
        }
    }
}
