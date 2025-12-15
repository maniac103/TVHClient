package org.tvheadend.tvhclient.ui.features.playback.external

import android.content.Intent
import android.net.Uri
import timber.log.Timber
import androidx.core.net.toUri

class PlayChannelActivity : BasePlaybackActivity() {

    override fun onTicketReceived() {
        val url = viewModel.getPlaybackUrl()
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(url.toUri(), "video/mp4")

        if (!viewModel.channel?.name.isNullOrEmpty()) {
            intent.putExtra("itemTitle", viewModel.channel?.name)
            intent.putExtra("title", viewModel.channel?.name)
        }
        Timber.d("Playing channel from server with url $url")
        startExternalPlayer(intent)
    }
}
