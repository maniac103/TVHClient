package org.tvheadend.tvhclient.ui.features.epg

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import org.tvheadend.data.entity.EpgChannel
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.EpgChannelRecyclerviewAdapterBinding
import org.tvheadend.tvhclient.ui.common.interfaces.RecyclerViewClickInterface
import org.tvheadend.tvhclient.util.extensions.applyChannelIcon
import java.util.*

class EpgChannelListRecyclerViewAdapter(
    private val clickCallback: RecyclerViewClickInterface<EpgChannel>
) : RecyclerView.Adapter<EpgChannelListRecyclerViewAdapter.EpgChannelViewHolder>() {
    private val channelList = ArrayList<EpgChannel>()

    var showChannelNumber = false
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpgChannelViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val itemBinding = EpgChannelRecyclerviewAdapterBinding.inflate(layoutInflater, parent, false)
        return EpgChannelViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: EpgChannelViewHolder, position: Int) {
        val channel = channelList[position]
        holder.bind(channel, position, showChannelNumber, clickCallback)
    }

    fun addItems(list: List<EpgChannel>) {
        channelList.clear()
        channelList.addAll(list)

        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return channelList.size
    }

    override fun getItemViewType(position: Int): Int {
        return R.layout.epg_channel_recyclerview_adapter
    }

    fun getItem(position: Int): EpgChannel? = channelList.getOrNull(position)

    class EpgChannelViewHolder(private val binding: EpgChannelRecyclerviewAdapterBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            channel: EpgChannel,
            position: Int,
            showChannelNumber: Boolean,
            clickCallback: RecyclerViewClickInterface<EpgChannel>
        ) {
            binding.channelIcon.apply {
                setOnClickListener { clickCallback.onClick(this, position, channel) }
                applyChannelIcon(channel, binding.channelIconText)
            }
            binding.channelIconText.setOnClickListener { v -> clickCallback.onClick(v, position, channel) }
            binding.channelNumber.apply {
                text = channel.displayNumber
                isVisible = showChannelNumber
            }
        }
    }
}
