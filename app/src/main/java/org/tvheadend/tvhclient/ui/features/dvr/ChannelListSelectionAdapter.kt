package org.tvheadend.tvhclient.ui.features.dvr

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.isInvisible
import com.squareup.picasso.Picasso
import org.tvheadend.data.entity.Channel
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.ChannelListSelectionDialogAdapterBinding
import org.tvheadend.tvhclient.util.getIconUrl

class ChannelListSelectionAdapter internal constructor(context: Context, channelList: List<Channel>) :
    ArrayAdapter<Channel>(context, R.layout.channel_list_selection_dialog_adapter, channelList.toTypedArray()) {

    private var callback: Callback? = null

    interface Callback {
        fun onItemClicked(channel: Channel)
    }

    fun setCallback(callback: Callback) {
        this.callback = callback
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val binding = if (convertView == null) {
            ChannelListSelectionDialogAdapterBinding.inflate(LayoutInflater.from((context)))
        } else {
            convertView.tag as ChannelListSelectionDialogAdapterBinding
        }

        binding.root.tag = binding
        getItem(position)?.let { channel ->
            binding.root.setOnClickListener { callback?.onItemClicked(channel) }
            if (!channel.icon.isNullOrEmpty()) {
                Picasso.get()
                    .load(getIconUrl(context, channel.icon))
                    .into(binding.icon)
            }
            binding.icon.isInvisible = channel.icon.isNullOrEmpty()
            binding.title.text = channel.name
        }
        return binding.root
    }
}
