package org.tvheadend.tvhclient.ui.features.dvr.recordings

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import org.tvheadend.data.ServerCapabilities
import org.tvheadend.data.entity.Recording
import org.tvheadend.data.entity.RecordingWithChannel
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.RecordingListAdapterBinding
import org.tvheadend.tvhclient.ui.common.interfaces.RecyclerViewClickInterface
import org.tvheadend.tvhclient.util.extensions.applyIcon
import org.tvheadend.tvhclient.util.extensions.applyRecordingStateIcon
import org.tvheadend.tvhclient.util.extensions.applyText
import org.tvheadend.tvhclient.util.extensions.applyTextAndAdjustVisibility
import org.tvheadend.tvhclient.util.extensions.determineContentTypeColor
import org.tvheadend.tvhclient.util.extensions.determineDataErrorText
import org.tvheadend.tvhclient.util.extensions.determineDataSizeText
import org.tvheadend.tvhclient.util.extensions.determineFailedReasonText
import org.tvheadend.tvhclient.util.extensions.determineStreamErrorText
import org.tvheadend.tvhclient.util.extensions.formatDate
import org.tvheadend.tvhclient.util.extensions.formatStartStopTime
import org.tvheadend.tvhclient.util.extensions.interpretColoredText
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

class RecordingRecyclerViewAdapter internal constructor(
    private val isDualPane: Boolean,
    private val clickCallback: RecyclerViewClickInterface<Recording>
) : RecyclerView.Adapter<RecordingRecyclerViewAdapter.RecordingViewHolder>(), Filterable {
    private val recordingList = ArrayList<RecordingWithChannel>()
    private var recordingListFiltered: MutableList<RecordingWithChannel> = ArrayList()
    private var selectedPosition = 0
    var showFileStatus = false
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }
    var showGenreColor = false
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }
    var serverCapabilities: ServerCapabilities? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    val items: List<RecordingWithChannel>
        get() = recordingListFiltered

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordingViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val itemBinding = RecordingListAdapterBinding.inflate(layoutInflater, parent, false)
        return RecordingViewHolder(itemBinding, isDualPane)
    }

    override fun onBindViewHolder(holder: RecordingViewHolder, position: Int) {
        val recording = recordingListFiltered[position]
        holder.bind(
            recording,
            position,
            recordingListFiltered.size,
            selectedPosition == position,
            serverCapabilities,
            showGenreColor,
            showFileStatus,
            clickCallback
        )
    }

    internal fun addItems(newItems: List<RecordingWithChannel>) {
        recordingList.clear()
        recordingListFiltered.clear()
        recordingList.addAll(newItems)
        recordingListFiltered.addAll(newItems)

        notifyDataSetChanged()

        if (selectedPosition > recordingListFiltered.size) {
            selectedPosition = 0
        }
    }

    override fun getItemCount(): Int {
        return recordingListFiltered.size
    }

    override fun getItemViewType(position: Int): Int {
        return R.layout.recording_list_adapter
    }

    fun setPosition(pos: Int) {
        notifyItemChanged(selectedPosition)
        selectedPosition = pos
        notifyItemChanged(pos)
    }

    fun getItem(position: Int): RecordingWithChannel? {
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
                val filteredList: MutableList<RecordingWithChannel> = ArrayList()
                if (charString.isNotEmpty()) {
                    // Iterate over the available channels. Use a copy on write
                    // array in case the channel list changes during filtering.
                    for (recording in CopyOnWriteArrayList(recordingList)) {
                        val title = recording.title ?: ""
                        val subtitle = recording.subtitle ?: ""
                        when {
                            title.lowercase().contains(charString.lowercase()) -> filteredList.add(recording)
                            subtitle.lowercase().contains(charString.lowercase()) -> filteredList.add(recording)
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
                recordingListFiltered.addAll(filterResults.values as ArrayList<RecordingWithChannel>)
                notifyDataSetChanged()
            }
        }
    }

    class RecordingViewHolder(
        private val binding: RecordingListAdapterBinding,
        private val isDualPane: Boolean
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            recording: RecordingWithChannel,
            position: Int,
            totalCount: Int,
            isSelected: Boolean,
            caps: ServerCapabilities?,
            showGenreColor: Boolean,
            showFileStatus: Boolean,
            clickCallback: RecyclerViewClickInterface<Recording>
        ) {
            binding.root.apply {
                setOnClickListener { clickCallback.onClick(this, position, recording.base) }
                setOnLongClickListener { clickCallback.onLongClick(this, position, recording.base) }
                assignRole(position, totalCount, isDualPane && isSelected)
            }
            binding.title.applyTextAndAdjustVisibility { interpretColoredText(recording.title) }
            binding.subtitle.apply {
                text = context.interpretColoredText(recording.subtitle)
                isVisible = text.isNotEmpty() && recording.title != recording.subtitle
            }
            binding.episode.applyTextAndAdjustVisibility(recording.episode)
            binding.summary.apply {
                text = recording.summary
                isVisible = text.isNotEmpty() && recording.subtitle != recording.summary
            }
            binding.description.applyTextAndAdjustVisibility { interpretColoredText(recording.description) }
            binding.channel.applyText { recording.channelName ?: getString(R.string.all_channels) }
            binding.duration.applyText { getString(R.string.minutes, recording.duration) }
            binding.date.applyText { formatDate(recording.start) }
            binding.startStop.applyText { formatStartStopTime(recording.start, recording.stop) }
            binding.icon.applyIcon(recording.channelIcon, recording.channelName, binding.iconText)
            binding.state.applyRecordingStateIcon(recording)
            binding.genre.apply {
                isVisible = showGenreColor
                recording.determineContentTypeColor(context)?.let { setBackgroundColor(it) }
            }
            binding.isSeriesRecording.isVisible = !recording.autorecId.isNullOrEmpty()
            binding.isTimerRecording.isVisible = !recording.timerecId.isNullOrEmpty()
            binding.failedReason.applyTextAndAdjustVisibility { recording.determineFailedReasonText(this) }
            binding.disabled.isVisible = recording.isScheduled && caps?.recordingEnabledSupported == true && !recording.isEnabled
            binding.duplicate.isVisible = recording.isScheduled && caps?.recordingDuplicateSupported == true && recording.duplicate != 0
            binding.dataSize.apply {
                text = recording.determineDataSizeText(context)
                isVisible = text.isNotEmpty() && showFileStatus
            }
            binding.dataErrors.apply {
                text = recording.determineDataErrorText(context)
                isVisible = text.isNotEmpty() && showFileStatus
            }
            binding.streamErrors.apply {
                text = recording.determineStreamErrorText(context)
                isVisible = text.isNotEmpty() && showFileStatus
            }
        }
    }
}
