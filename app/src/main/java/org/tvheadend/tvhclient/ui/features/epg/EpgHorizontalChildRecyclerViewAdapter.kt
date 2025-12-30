package org.tvheadend.tvhclient.ui.features.epg

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.tvheadend.data.entity.EpgProgram
import org.tvheadend.data.entity.Recording
import org.tvheadend.data.entity.RecordingWithChannel
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.EpgHorizontalChildRecyclerviewAdapterBinding
import org.tvheadend.tvhclient.ui.common.interfaces.RecyclerViewClickInterface
import org.tvheadend.tvhclient.util.extensions.applyRecordingStateIcon
import org.tvheadend.tvhclient.util.extensions.applyText
import org.tvheadend.tvhclient.util.extensions.applyTextAndAdjustVisibility
import org.tvheadend.tvhclient.util.extensions.determineContentTypeColor
import org.tvheadend.tvhclient.util.extensions.interpretColoredText
import kotlin.math.max
import kotlin.math.min

internal class EpgHorizontalChildRecyclerViewAdapter(
    fragmentId: Int,
    lifecycleOwner: LifecycleOwner,
    private val viewModel: EpgViewModel
) : RecyclerView.Adapter<EpgHorizontalChildRecyclerViewAdapter.EpgProgramListViewHolder>() {
    init {
        viewModel.getFragmentStartAndEndTimes(fragmentId).observe(lifecycleOwner) {
            startTime = it.first
            endTime = it.second
        }
        viewModel.hoursOfEpgDataPerScreen.observe(lifecycleOwner) { hoursPerScreen = it }
        viewModel.showProgramSubtitle.observe(lifecycleOwner) { showProgramSubtitle = it }
        viewModel.showGenreColor.observe(lifecycleOwner) { showGenreColor = it }
    }

    private val programList = ArrayList<ItemModel>()
    private val recordingList = ArrayList<Recording>()

    var startTime = 0L
        set(value) {
            if (value != field) {
                field = value
                notifyDataSetChanged()
            }
        }
    var endTime = 0L
        set(value) {
            if (value != field) {
                field = value
                notifyDataSetChanged()
            }
        }
    var hoursPerScreen = 1
        set(value) {
            if (value != field) {
                field = value
                notifyDataSetChanged()
            }
        }
    var parentWidth = 1
        set(value) {
            if (value != field) {
                field = value
                notifyDataSetChanged()
            }
        }
    var showProgramSubtitle = false
        set(value) {
            if (value != field) {
                field = value
                notifyDataSetChanged()
            }
        }
    var showGenreColor = false
        set(value) {
            if (value != field) {
                field = value
                notifyDataSetChanged()
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpgProgramListViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val itemBinding = EpgHorizontalChildRecyclerviewAdapterBinding.inflate(layoutInflater, parent, false)
        val viewHolder = EpgProgramListViewHolder(itemBinding)
        return viewHolder
    }

    override fun onBindViewHolder(holder: EpgProgramListViewHolder, position: Int) {
        val model = programList[position]
        val program = model.program

        val millisPerHour = 60L * 60L * 1000L
        val start = max(program.start, startTime)
        val stop = min(program.stop, endTime)
        val durationInHours = (stop - start).toFloat() / millisPerHour.toFloat()
        val layoutWidth = (durationInHours * parentWidth / hoursPerScreen).toInt()

        holder.bind(model, position, layoutWidth, showProgramSubtitle, showGenreColor, viewModel)
    }

    override fun onBindViewHolder(holder: EpgProgramListViewHolder, position: Int, payloads: List<Any>) {
        onBindViewHolder(holder, position)
    }

    fun addItems(items: List<EpgProgram>, recordings: List<RecordingWithChannel>) {
        val newItems = items.map { ItemModel(it) }.toMutableList()
        updateRecordingState(newItems, recordingList)

        val oldItems = ArrayList(programList)
        val diffResult = DiffUtil.calculateDiff(EpgProgramListDiffCallback(oldItems, newItems))

        programList.clear()
        programList.addAll(newItems)

        recordingList.clear()
        recordingList.addAll(recordings.map { it.base })
        updateRecordingState(programList, recordingList)

        diffResult.dispatchUpdatesTo(this)
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
                if (
                    oldRecording == null ||
                    oldRecording.error != relevantRecording.error ||
                    oldRecording.state != relevantRecording.state
                ) {
                    notifyItemChanged(index)
                }
            } else if (model.recording != null) {
                model.recording = null
                notifyItemChanged(index)
            }
        }
    }

    override fun getItemCount(): Int {
        return programList.size
    }

    override fun getItemViewType(position: Int): Int {
        return R.layout.epg_horizontal_child_recyclerview_adapter
    }

    data class ItemModel(val program: EpgProgram, var recording: Recording? = null)

    private class EpgProgramListDiffCallback(private val oldList: List<ItemModel>, private val newList: List<ItemModel>) : DiffUtil.Callback() {

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

    internal class EpgProgramListViewHolder(
        private val binding: EpgHorizontalChildRecyclerviewAdapterBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            model: ItemModel,
            position: Int,
            layoutWidth: Int,
            showProgramSubtitle: Boolean,
            showGenreColor: Boolean,
            clickListener: RecyclerViewClickInterface<EpgProgram>
        ) {
            binding.root.apply {
                setOnClickListener { clickListener.onClick(this, position, model.program) }
                setOnLongClickListener { clickListener.onLongClick(this, position, model.program) }

                val lp = layoutParams as RecyclerView.LayoutParams
                lp.width = layoutWidth
                layoutParams = lp
            }

            binding.title.applyTextAndAdjustVisibility { interpretColoredText(model.program.title) }
            binding.subtitle.apply {
                text = context.interpretColoredText(model.program.subtitle)
                isVisible = text.isNotEmpty() && showProgramSubtitle
            }
            binding.duration.applyText { getString(R.string.minutes, model.program.duration) }
            binding.state.applyRecordingStateIcon(model.recording)
            binding.genre.apply {
                val color = model.program.determineContentTypeColor(context, 25)
                isVisible = showGenreColor && color != null
                color?.let { setBackgroundColor(it) }
            }
        }
    }
}
