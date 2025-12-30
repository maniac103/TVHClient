package org.tvheadend.tvhclient.ui.features.programs

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.tvheadend.data.entity.ProgramWithChannel
import org.tvheadend.data.entity.Recording
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.ProgramListAdapterBinding
import org.tvheadend.tvhclient.ui.common.interfaces.RecyclerViewClickInterface
import org.tvheadend.tvhclient.util.extensions.applyIcon
import org.tvheadend.tvhclient.util.extensions.applyText
import org.tvheadend.tvhclient.util.extensions.applyTextAndAdjustVisibility
import org.tvheadend.tvhclient.util.extensions.determineContentTypeColor
import org.tvheadend.tvhclient.util.extensions.determineContentTypeText
import org.tvheadend.tvhclient.util.extensions.determineSeriesInfoText
import org.tvheadend.tvhclient.util.extensions.formatDate
import org.tvheadend.tvhclient.util.extensions.formatStartStopTime
import org.tvheadend.tvhclient.util.extensions.interpretColoredText

class ProgramRecyclerViewAdapter internal constructor(
    private val showChannelIcon: Boolean,
    private val clickCallback: RecyclerViewClickInterface<ItemModel>,
    private val onLastProgramVisibleListener: LastProgramVisibleListener
) : RecyclerView.Adapter<ProgramRecyclerViewAdapter.ProgramViewHolder>(), Filterable {

    private val programList = ArrayList<ItemModel>()
    private var programListFiltered: MutableList<ItemModel> = ArrayList()
    private val recordingList = ArrayList<Recording>()

    var showProgramSubtitle: Boolean = false
        set(value) {
            if (value != field) {
                field = value
                notifyDataSetChanged()
            }
        }
    var showGenreColor: Boolean = false
        set(value) {
            if (value != field) {
                field = value
                notifyDataSetChanged()
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProgramViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val itemBinding = ProgramListAdapterBinding.inflate(layoutInflater, parent, false)
        return ProgramViewHolder(itemBinding, showChannelIcon)
    }

    override fun onBindViewHolder(holder: ProgramViewHolder, position: Int) {
        if (programListFiltered.size > position) {
            val model = programListFiltered[position]
            holder.bind(model, position, showGenreColor, showProgramSubtitle, clickCallback)
            if (position == programList.size - 1) {
                onLastProgramVisibleListener.onLastProgramVisible(position)
            }
        }
    }

    override fun onBindViewHolder(holder: ProgramViewHolder, position: Int, payloads: List<Any>) {
        onBindViewHolder(holder, position)
    }

    internal fun addItems(items: List<ProgramWithChannel>) {
        val newItems = items.map { ItemModel(it) }.toMutableList()
        updateRecordingState(newItems, recordingList)

        val oldItems = ArrayList(programListFiltered)
        val diffResult = DiffUtil.calculateDiff(ProgramListDiffCallback(oldItems, newItems))

        programList.clear()
        programListFiltered.clear()
        programList.addAll(newItems)
        programListFiltered.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemCount(): Int {
        return programListFiltered.size
    }

    override fun getItemViewType(position: Int): Int {
        return R.layout.program_list_adapter
    }

    fun getItem(position: Int): ItemModel? {
        return if (programListFiltered.size > position && position >= 0) {
            programListFiltered[position]
        } else {
            null
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence): FilterResults {
                val charString = charSequence.toString().lowercase()
                val filteredList = if (charString.isNotEmpty()) {
                    programList
                        .filter { it.program.title?.lowercase()?.contains(charString) == true }
                        .toMutableList()
                } else {
                    ArrayList(programList)
                }

                val filterResults = FilterResults()
                filterResults.values = filteredList
                return filterResults
            }

            override fun publishResults(charSequence: CharSequence, filterResults: FilterResults) {
                programListFiltered.clear()
                @Suppress("UNCHECKED_CAST")
                programListFiltered.addAll(filterResults.values as ArrayList<ItemModel>)
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
    internal fun addRecordings(list: List<Recording>) {
        recordingList.clear()
        recordingList.addAll(list)
        updateRecordingState(programListFiltered, recordingList)
    }

    private fun updateRecordingState(items: MutableList<ItemModel>, recordings: List<Recording>) {
        items.forEachIndexed { index, model ->
            val relevantRecording = recordings.firstOrNull { model.program.eventId > 0 && model.program.eventId == it.eventId }
            if (relevantRecording != null) {
                val oldRecording = model.recording
                model.recording = relevantRecording

                // Do a full update only when a new recording was added or the recording
                // state has changed which results in a different recording state icon
                // Otherwise do not update the UI
                if (oldRecording == null || oldRecording.error != relevantRecording.error || oldRecording.state != relevantRecording.state) {
                    notifyItemChanged(index)
                }
            } else if (model.recording != null) {
                model.recording = null
                notifyItemChanged(index)
            }
        }
    }

    data class ItemModel(val program: ProgramWithChannel, var recording: Recording? = null)

    private class ProgramListDiffCallback(private val oldList: List<ItemModel>, private val newList: List<ItemModel>) : DiffUtil.Callback() {
        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return newList[newItemPosition].program.eventId == oldList[oldItemPosition].program.eventId
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return newList[newItemPosition] == oldList[oldItemPosition]
        }
    }

    class ProgramViewHolder(
        private val binding: ProgramListAdapterBinding,
        private val showChannelIcon: Boolean
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            if (!showChannelIcon) {
                val contentStart = (16 * binding.root.context.resources.displayMetrics.density).toInt()
                binding.contentStart.setGuidelineBegin(contentStart)
                binding.icon.isVisible = false
                binding.iconText.isVisible = false
            }
        }

        fun bind(model: ItemModel,
                 position: Int,
                 showGenreColor: Boolean,
                 showProgramSubtitle: Boolean,
                 clickCallback: RecyclerViewClickInterface<ItemModel>) {
            binding.root.apply {
                setOnClickListener { clickCallback.onClick(this, position, model) }
                setOnLongClickListener { clickCallback.onLongClick(this, position, model) }
            }
            if (showChannelIcon) {
                binding.icon.applyIcon(model.program.channelIcon, model.program.channelName, binding.iconText)
            }
            binding.title.applyTextAndAdjustVisibility { interpretColoredText(model.program.title) }
            binding.subtitle.apply {
                text = context.interpretColoredText(model.program.subtitle)
                isVisible = text.isNotEmpty() && showProgramSubtitle && model.program.subtitle != model.program.title
            }
            binding.summary.apply {
                text = model.program.summary
                isVisible = text.isNotEmpty() && (!showProgramSubtitle || model.program.summary != model.program.subtitle)
            }
            binding.genre.apply {
                val color = model.program.determineContentTypeColor(context)
                isVisible = showGenreColor && color != null
                color?.let { setBackgroundColor(it) }
            }
            binding.contentType.applyText { determineContentTypeText(model.program.contentType) }
            binding.date.applyText { formatDate(model.program.start) }
            binding.startStop.applyText { formatStartStopTime(model.program.start, model.program.stop) }
            binding.duration.applyText { getString(R.string.minutes, model.program.duration) }
            binding.progress.apply {
                text = context.getString(R.string.progress, model.program.progress)
                isVisible = model.program.progress > 0
            }
            binding.seriesInfo.applyTextAndAdjustVisibility { model.program.determineSeriesInfoText(this) }
            binding.description.applyTextAndAdjustVisibility { interpretColoredText(model.program.description) }
        }
    }
}
