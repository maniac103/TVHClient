package org.tvheadend.tvhclient.ui.features.epg

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.tvheadend.data.entity.EpgProgram
import org.tvheadend.data.entity.Recording
import org.tvheadend.data.entity.RecordingWithChannel
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.EpgHorizontalChildRecyclerviewAdapterBinding
import org.tvheadend.tvhclient.util.extensions.isEqualTo
import java.util.*
import kotlin.math.max
import kotlin.math.min

internal class EpgHorizontalChildRecyclerViewAdapter(private val viewModel: EpgViewModel, private val fragmentId: Int, private val lifecycleOwner: LifecycleOwner) : RecyclerView.Adapter<EpgHorizontalChildRecyclerViewAdapter.EpgProgramListViewHolder>() {
    init {
        viewModel.hoursOfEpgDataPerScreen.observe(lifecycleOwner) { hours ->
            hoursPerScreen = hours
            notifyDataSetChanged()
        }
    }

    private val programList = ArrayList<ItemModel>()
    private val recordingList = ArrayList<Recording>()
    private var hoursPerScreen = 1
    var parentWidth = 1
        set(value) {
            if (value != field) {
                field = value
                notifyDataSetChanged()
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpgProgramListViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val itemBinding = EpgHorizontalChildRecyclerviewAdapterBinding.inflate(layoutInflater, parent, false)
        val viewHolder = EpgProgramListViewHolder(itemBinding, viewModel)
        itemBinding.lifecycleOwner = lifecycleOwner
        return viewHolder
    }

    override fun onBindViewHolder(holder: EpgProgramListViewHolder, position: Int) {
        val model = programList[position]
        val program = model.program

        val startTime = max(program.start, viewModel.getStartTime(fragmentId))
        val stopTime = min(program.stop, viewModel.getEndTime(fragmentId))
        val durationInHours = (stopTime - startTime).toFloat() / 1000F / 60F / 60F
        val layoutWidth = (durationInHours * parentWidth / hoursPerScreen).toInt()

        holder.bind(model, layoutWidth)
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
            var recordingExists = false

            for (recording in recordings) {
                if (model.program.eventId > 0 && model.program.eventId == recording.eventId) {
                    val oldRecording = model.recording
                    model.recording = recording

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

    internal class EpgProgramListViewHolder(private val binding: EpgHorizontalChildRecyclerviewAdapterBinding,
                                            private val viewModel: EpgViewModel) : RecyclerView.ViewHolder(binding.root) {
        fun bind(model: ItemModel, layoutWidth: Int) {
            binding.model = model
            binding.layoutWidth = layoutWidth
            binding.viewModel = viewModel
            binding.executePendingBindings()
        }
    }
}
