package org.tvheadend.tvhclient.ui.features.dvr.timer_recordings

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import org.tvheadend.data.ServerCapabilities
import org.tvheadend.data.entity.TimerRecordingWithChannel
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.TimerRecordingListAdapterBinding
import org.tvheadend.tvhclient.ui.common.interfaces.RecyclerViewClickInterface
import org.tvheadend.tvhclient.ui.features.dvr.minutesToTimeMillis
import org.tvheadend.tvhclient.util.extensions.applyIcon
import org.tvheadend.tvhclient.util.extensions.applyText
import org.tvheadend.tvhclient.util.extensions.determineDaysOfWeekText
import org.tvheadend.tvhclient.util.extensions.formatStartStopTime
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

class TimerRecordingRecyclerViewAdapter internal constructor(
    private val isDualPane: Boolean,
    private val clickCallback: RecyclerViewClickInterface<TimerRecordingWithChannel>,
    htspVersion: Int
) : RecyclerView.Adapter<TimerRecordingRecyclerViewAdapter.TimerRecordingViewHolder>(), Filterable {
    private val caps = ServerCapabilities(htspVersion)
    private val recordingList = ArrayList<TimerRecordingWithChannel>()
    private var recordingListFiltered: MutableList<TimerRecordingWithChannel> = ArrayList()
    private var selectedPosition = 0

    val items: List<TimerRecordingWithChannel>
        get() = recordingListFiltered

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimerRecordingViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val itemBinding = TimerRecordingListAdapterBinding.inflate(layoutInflater, parent, false)
        return TimerRecordingViewHolder(itemBinding, isDualPane)
    }

    override fun onBindViewHolder(holder: TimerRecordingViewHolder, position: Int) {
        if (recordingListFiltered.size > position) {
            val recording = recordingListFiltered[position]
            holder.bind(recording, position, recordingListFiltered.size, selectedPosition == position, caps, clickCallback)
        }
    }

    override fun onBindViewHolder(holder: TimerRecordingViewHolder, position: Int, payloads: List<Any>) {
        onBindViewHolder(holder, position)
    }

    internal fun addItems(newItems: List<TimerRecordingWithChannel>) {
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
        return R.layout.timer_recording_list_adapter
    }

    fun setPosition(pos: Int) {
        notifyItemChanged(selectedPosition)
        selectedPosition = pos
        notifyItemChanged(pos)
    }

    fun getItem(position: Int): TimerRecordingWithChannel? {
        return if (recordingListFiltered.size > position && position >= 0) {
            recordingListFiltered[position]
        } else {
            null
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence): FilterResults {
                val charString = charSequence.toString()
                val filteredList: MutableList<TimerRecordingWithChannel> = ArrayList()
                if (charString.isNotEmpty()) {
                    for (recording in CopyOnWriteArrayList(recordingList)) {
                        val title = recording.title ?: ""
                        val name = recording.name ?: ""
                        when {
                            title.lowercase().contains(charString.lowercase()) -> filteredList.add(recording)
                            name.lowercase().contains(charString.lowercase()) -> filteredList.add(recording)
                        }
                    }
                } else {
                    filteredList.addAll(recordingList)
                }

                val filterResults = FilterResults()
                filterResults.values = filteredList
                return filterResults
            }

            override fun publishResults(charSequence: CharSequence, filterResults: FilterResults) {
                recordingListFiltered.clear()
                @Suppress("UNCHECKED_CAST")
                recordingListFiltered.addAll(filterResults.values as ArrayList<TimerRecordingWithChannel>)
                notifyDataSetChanged()
            }
        }
    }

    class TimerRecordingViewHolder(private val binding: TimerRecordingListAdapterBinding, private val isDualPane: Boolean) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            recording: TimerRecordingWithChannel,
            position: Int,
            totalCount: Int,
            isSelected: Boolean,
            caps: ServerCapabilities,
            clickCallback: RecyclerViewClickInterface<TimerRecordingWithChannel>
        ) {
            binding.root.apply {
                setOnClickListener { clickCallback.onClick(it, position, recording) }
                setOnLongClickListener { clickCallback.onLongClick(it, position, recording) }
                assignRole(position, totalCount, isDualPane && isSelected)
            }
            binding.title.text = recording.title ?: recording.name
            binding.name.apply {
                text = recording.name
                isVisible = text.isNotEmpty() && recording.title != recording.name
            }
            binding.channel.applyText { recording.channelName ?: getString(R.string.all_channels) }
            binding.duration.applyText { getString(R.string.minutes, recording.duration) }
            binding.daysOfWeek.applyText { determineDaysOfWeekText(recording.daysOfWeek) }
            binding.startStop.applyText {
                formatStartStopTime(
                    minutesToTimeMillis(recording.start),
                    minutesToTimeMillis(recording.stop)
                )
            }
            binding.disabled.isVisible = caps.recordingEnabledSupported && !recording.isEnabled
            binding.icon.applyIcon(recording.channelIcon, recording.channelName, binding.iconText)
        }
    }
}
