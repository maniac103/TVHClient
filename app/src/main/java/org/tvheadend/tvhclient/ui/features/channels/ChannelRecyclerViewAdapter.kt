package org.tvheadend.tvhclient.ui.features.channels

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.tvheadend.data.entity.ChannelWithProgram
import org.tvheadend.data.entity.Recording
import org.tvheadend.data.entity.RecordingWithChannel
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.ChannelListAdapterBinding
import org.tvheadend.tvhclient.ui.common.interfaces.RecyclerViewClickInterface
import org.tvheadend.tvhclient.util.extensions.isEqualTo

class ChannelRecyclerViewAdapter internal constructor(private val viewModel: ChannelViewModel, private val isDualPane: Boolean, private val clickCallback: RecyclerViewClickInterface, private val lifecycleOwner: LifecycleOwner) : RecyclerView.Adapter<ChannelRecyclerViewAdapter.ChannelViewHolder>(), Filterable {

    private val recordingList = ArrayList<RecordingWithChannel>()
    private val channelList = ArrayList<ItemModel>()
    private var channelListFiltered: MutableList<ItemModel> = ArrayList()
    private var selectedPosition = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val itemBinding = ChannelListAdapterBinding.inflate(layoutInflater, parent, false)
        val viewHolder = ChannelViewHolder(itemBinding, viewModel, isDualPane)
        itemBinding.lifecycleOwner = lifecycleOwner
        return viewHolder
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        if (channelListFiltered.size > position) {
            val item = channelListFiltered[position]
            holder.bind(item, position, selectedPosition == position, clickCallback)
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

    class ChannelViewHolder(private val binding: ChannelListAdapterBinding,
                            private val viewModel: ChannelViewModel,
                            private val isDualPane: Boolean) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ItemModel, position: Int, isSelected: Boolean, clickCallback: RecyclerViewClickInterface) {
            binding.model = item
            binding.position = position
            binding.isSelected = isSelected
            binding.viewModel = viewModel
            binding.isDualPane = isDualPane
            binding.callback = clickCallback
            binding.executePendingBindings()
        }
    }
}
