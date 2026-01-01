package org.tvheadend.tvhclient.ui.features.channels

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.tvheadend.data.entity.ChannelWithProgram
import org.tvheadend.data.entity.Recording
import org.tvheadend.data.entity.RecordingWithChannel
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.ChannelListAdapterBinding
import org.tvheadend.tvhclient.ui.common.ListItemContainerView
import org.tvheadend.tvhclient.ui.common.interfaces.RecyclerViewClickInterface
import org.tvheadend.tvhclient.util.extensions.applyChannelIcon
import org.tvheadend.tvhclient.util.extensions.applyRecordingStateIcon
import org.tvheadend.tvhclient.util.extensions.determineContentTypeColor
import org.tvheadend.tvhclient.util.extensions.formatStartStopTime
import org.tvheadend.tvhclient.util.extensions.interpretColoredText
import org.tvheadend.tvhclient.util.extensions.isEqualTo

class ChannelRecyclerViewAdapter internal constructor(
    private val isDualPane: Boolean,
    private val clickCallback: RecyclerViewClickInterface<ChannelWithProgram>
) : RecyclerView.Adapter<ChannelRecyclerViewAdapter.ChannelViewHolder>(), Filterable {
    private val recordingList = ArrayList<RecordingWithChannel>()
    private val channelList = ArrayList<ItemModel>()
    private var channelListFiltered: MutableList<ItemModel> = ArrayList()
    private var selectedPosition = 0

    var showChannelName: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }
    var showChannelNumber: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }
    var showProgramSubtitle: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }
    var showProgressBar: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }
    var showGenreColor: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }
    var showNextProgramTitle: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val itemBinding = ChannelListAdapterBinding.inflate(layoutInflater, parent, false)
        return ChannelViewHolder(itemBinding, isDualPane)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        if (channelListFiltered.size > position) {
            val item = channelListFiltered[position]
            holder.bind(
                item,
                position,
                channelListFiltered.size,
                selectedPosition == position,
                showChannelName,
                showChannelNumber,
                showProgramSubtitle,
                showNextProgramTitle,
                showGenreColor,
                showProgressBar,
                clickCallback
            )
        }
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int, payloads: List<Any>) {
        onBindViewHolder(holder, position)
    }

    internal fun addItems(items: MutableList<ChannelWithProgram>) {
        val newItems = items.map { ItemModel(it) }.toMutableList()
        updateRecordingState(newItems, recordingList)

        val oldItems = ArrayList(channelListFiltered)
        val diffResult = DiffUtil.calculateDiff(DiffCallback(oldItems, newItems))

        channelList.clear()
        channelListFiltered.clear()
        channelList.addAll(newItems)
        channelListFiltered.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)

        if (selectedPosition > channelListFiltered.size) {
            selectedPosition = 0
        }
    }

    override fun getItemCount(): Int {
        return channelListFiltered.size
    }

    override fun getItemViewType(position: Int): Int {
        return R.layout.channel_list_adapter
    }

    fun setPosition(pos: Int) {
        notifyItemChanged(selectedPosition)
        selectedPosition = pos
        notifyItemChanged(pos)
    }

    fun getItem(position: Int) = if (channelListFiltered.size > position && position >= 0) {
        channelListFiltered[position]
    } else {
        null
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence): FilterResults {
                val charString = charSequence.toString()
                val origList = channelList
                val filteredList = if (charString.isNotEmpty()) {
                    origList.filter { model -> model.channel.name.equals(charString, ignoreCase = true) }
                } else {
                    origList
                }

                val filterResults = FilterResults()
                filterResults.values = filteredList
                return filterResults
            }

            override fun publishResults(charSequence: CharSequence, filterResults: FilterResults) {
                channelListFiltered.clear()
                @Suppress("UNCHECKED_CAST")
                channelListFiltered.addAll(filterResults.values as ArrayList<ItemModel>)
                notifyDataSetChanged()
            }
        }
    }

    /**
     * Whenever a recording changes in the database the list of available recordings are
     * saved in this recycler view. The previous list is cleared to avoid showing outdated
     * recording states. Each recording is checked if it belongs to the
     * currently shown program. If yes then its state is updated.
     *
     * @param list List of recordings
     */
    internal fun addRecordings(list: List<RecordingWithChannel>) {
        recordingList.clear()
        recordingList.addAll(list)
        updateRecordingState(channelListFiltered, recordingList)
    }

    private fun updateRecordingState(items: MutableList<ItemModel>, recordings: List<RecordingWithChannel>) {
        items.forEachIndexed { index, model ->
            var recordingExists = false

            for (recording in recordings) {
                if (model.channel.programId > 0 && model.channel.programId == recording.eventId) {
                    val oldRecording = model.recording
                    model.recording = recording.base

                    // Do a full update only when a new recording was added or the recording
                    // state has changed which results in a different recording state icon
                    // Otherwise do not update the UI
                    if (oldRecording == null
                            || !oldRecording.error.isEqualTo(recording.error)
                            || !oldRecording.state.isEqualTo(recording.state)) {
                        notifyItemChanged(index)
                    }
                    recordingExists = true
                    break
                }
            }
            if (!recordingExists && model.recording != null) {
                model.recording = null
                notifyItemChanged(index)
            }
        }
    }

    data class ItemModel(val channel: ChannelWithProgram, var recording: Recording? = null)

    private class DiffCallback(private val oldList: List<ItemModel>, private val newList: List<ItemModel>) : DiffUtil.Callback() {
        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return newList[newItemPosition].channel.id == oldList[oldItemPosition].channel.id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return newList[newItemPosition] == oldList[oldItemPosition]
        }
    }

    class ChannelViewHolder(
        private val binding: ChannelListAdapterBinding,
        private val isDualPane: Boolean
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            item: ItemModel,
            position: Int,
            totalCount: Int,
            isSelected: Boolean,
            showChannelName: Boolean,
            showChannelNumber: Boolean,
            showProgramSubtitle: Boolean,
            showNextProgramTitle: Boolean,
            showGenreColor: Boolean,
            showProgressBar: Boolean,
            clickCallback: RecyclerViewClickInterface<ChannelWithProgram>
        ) {
            binding.root.apply {
                setOnClickListener { clickCallback.onClick(this, position, item.channel) }
                setOnLongClickListener { clickCallback.onLongClick(this, position, item.channel) }
                assignRole(position, totalCount, isDualPane && isSelected)
            }
            binding.icon.apply {
                setOnClickListener { clickCallback.onClick(this, position, item.channel) }
                applyChannelIcon(item.channel, binding.iconText)
            }
            binding.iconText.setOnClickListener { v -> clickCallback.onClick(v, position, item.channel) }
            binding.channelNumber.apply {
                isVisible = showChannelNumber
                text = if (item.channel.numberMinor == 0) item.channel.number.toString() else item.channel.displayNumber
            }
            binding.channelName.apply {
                isVisible = showChannelName
                text = item.channel.name
            }
            binding.title.apply {
                isVisible = item.channel.programId > 0
                text = context.interpretColoredText(item.channel.programTitle)
            }
            binding.subtitle.apply {
                text = context.interpretColoredText(item.channel.programSubtitle)
                isVisible = text.isNotEmpty() &&
                        item.channel.programId > 0 &&
                        showProgramSubtitle &&
                        item.channel.programSubtitle != item.channel.programTitle
            }
            binding.startStopTime.apply {
                isVisible = item.channel.programId > 0
                text = context.formatStartStopTime(item.channel.programStart, item.channel.programStop)
            }
            binding.duration.apply {
                isVisible = item.channel.programId > 0
                text = context.getString(R.string.minutes, item.channel.duration)
            }
            binding.nextTitle.apply {
                text = item.channel.nextProgramTitle
                isVisible = item.channel.nextProgramId > 0 &&
                        showNextProgramTitle &&
                        item.channel.nextProgramTitle != null
            }
            binding.noPrograms.isVisible = item.channel.programId == 0
            binding.state.applyRecordingStateIcon(item.recording)
            binding.progressbar.apply {
                isVisible = item.channel.programId > 0 && item.channel.progress > 0 && showProgressBar
                progress = item.channel.progress
            }
            binding.genre.apply {
                val color = item.channel.determineContentTypeColor(context)
                isVisible = showGenreColor && color != null
                color?.let { setBackgroundColor(it) }
            }
        }
    }
}
