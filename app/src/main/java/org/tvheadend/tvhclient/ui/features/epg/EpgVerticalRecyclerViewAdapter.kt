package org.tvheadend.tvhclient.ui.features.epg

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import org.tvheadend.data.entity.EpgProgram
import org.tvheadend.data.entity.RecordingWithChannel
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.EpgVerticalRecyclerviewAdapterBinding
import timber.log.Timber

internal class EpgVerticalRecyclerViewAdapter(private val epgViewModel: EpgViewModel, private val fragmentId: Int, private val lifecycleOwner: LifecycleOwner) : RecyclerView.Adapter<EpgVerticalRecyclerViewAdapter.EpgViewPagerViewHolder>() {

    private val viewPool: RecyclerView.RecycledViewPool = RecyclerView.RecycledViewPool()
    private val programLists = ArrayList<EpgViewModel.EpgChannelEntry>()
    private val recordingList = ArrayList<RecordingWithChannel>()

    var viewWidth = 0
        set(value) {
            if (value != field) {
                field = value
                notifyDataSetChanged()
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpgViewPagerViewHolder {
        val binding = EpgVerticalRecyclerviewAdapterBinding.inflate(LayoutInflater.from(parent.context))
        return EpgViewPagerViewHolder(binding.root, binding, fragmentId, viewPool, epgViewModel, lifecycleOwner)
    }

    override fun onBindViewHolder(holder: EpgViewPagerViewHolder, position: Int) {
        val entry = programLists[position]
        Timber.d("Binding ${entry.programs.size} programs for channel ${entry.channel.name} in viewpager fragment $fragmentId")
        holder.bindData(entry.programs, recordingList, viewWidth)
    }

    override fun getItemCount(): Int {
        return programLists.size
    }

    override fun getItemViewType(position: Int): Int {
        return R.layout.epg_vertical_recyclerview_adapter
    }

    fun loadProgramData(entries: List<EpgViewModel.EpgChannelEntry>, recordings: List<RecordingWithChannel>) {
        Timber.d("Loading programs for viewpager fragment $fragmentId")
        programLists.clear()
        programLists.addAll(entries)
        recordingList.clear()
        recordingList.addAll(recordings)

        notifyDataSetChanged()
    }

    class EpgViewPagerViewHolder(override val containerView: View,
                                 val binding: EpgVerticalRecyclerviewAdapterBinding,
                                 fragmentId: Int,
                                 viewPool: RecyclerView.RecycledViewPool,
                                 private val epgViewModel: EpgViewModel,
                                 lifecycleOwner: LifecycleOwner) : RecyclerView.ViewHolder(binding.root), LayoutContainer {

        private val recyclerViewAdapter: EpgHorizontalChildRecyclerViewAdapter

        init {
            binding.horizontalChildRecyclerView.layoutManager = CustomHorizontalLayoutManager(containerView.context)
            binding.horizontalChildRecyclerView.setRecycledViewPool(viewPool)
            recyclerViewAdapter = EpgHorizontalChildRecyclerViewAdapter(epgViewModel, fragmentId, lifecycleOwner)
            binding.horizontalChildRecyclerView.adapter = recyclerViewAdapter
        }

        fun bindData(programs: List<EpgProgram>, recordings: List<RecordingWithChannel>, viewWidth: Int) {
            recyclerViewAdapter.parentWidth = viewWidth

            binding.horizontalChildRecyclerView.isInvisible = programs.isEmpty()
            recyclerViewAdapter.addItems(programs, recordings)

            binding.noPrograms.isVisible = programs.isEmpty()
        }

        internal class CustomHorizontalLayoutManager(context: Context) : LinearLayoutManager(context, HORIZONTAL, false) {

            override fun canScrollHorizontally(): Boolean {
                return false
            }
        }
    }
}
