package org.tvheadend.tvhclient.ui.features.playback.external

import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.preference.PreferenceManager
import timber.log.Timber
import java.io.File
import androidx.core.net.toUri

class PlayRecordingActivity : BasePlaybackActivity() {

    override fun onTicketReceived() {
        val url = viewModel.getPlaybackUrl()
        val intent = Intent(Intent.ACTION_VIEW)
        val title = viewModel.recording?.title ?: ""
        intent.putExtra("itemTitle", title)
        intent.putExtra("title",title)

        // Check if the recording exists in the download folder, if not stream it from the server
        val file = File(Environment.DIRECTORY_DOWNLOADS, "$title.mkv")

        if (file.exists()) {
            Timber.d("Playing recording from local file ${file.absolutePath}")
            intent.setDataAndType(file.absolutePath.toUri(), "video/mp4")
        } else {
            Timber.d("Playing recording from server with url: $url")
            intent.setDataAndType(url.toUri(), "video/mp4")
        }
        startExternalPlayer(intent)
    }
}
