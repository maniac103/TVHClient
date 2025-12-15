package org.tvheadend.tvhclient.ui.features.playback.external

import android.content.Intent
import android.net.Uri
import androidx.core.view.isVisible
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadOptions
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.common.images.WebImage
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.util.extensions.getCastSession
import timber.log.Timber
import androidx.core.net.toUri

class CastRecordingActivity : BasePlaybackActivity() {

    override fun onTicketReceived() {

        val castSession = this.getCastSession()
        if (castSession == null) {
            binding.progressBar.isVisible = false
            binding.status.text = getString(R.string.no_cast_session)
            return
        }

        val recording = viewModel.recording ?: return
        val recordingTitle : String = recording.title ?: ""
        val recordingSubtitle : String = recording.subtitle ?: ""
        val movieMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE)
        movieMetadata.putString(MediaMetadata.KEY_TITLE, recordingTitle)
        movieMetadata.putString(MediaMetadata.KEY_SUBTITLE, recordingSubtitle)

        val icon = recording.channelIcon
        if (!icon.isNullOrEmpty()) {
            val iconUrl = if (icon.startsWith("http")) {
                icon
            } else {
                viewModel.getServerUrl() + "/" + icon
            }
            Timber.d("Recording channel icon url: $iconUrl")
            movieMetadata.addImage(WebImage(iconUrl.toUri()))   // small cast icon
            movieMetadata.addImage(WebImage(iconUrl.toUri()))   // large background icon
        }

        val castingProfileId = viewModel.getServerStatus().castingServerProfileId
        if (castingProfileId == 0) {
            binding.progressBar.isVisible = false
            binding.status.text = getString(R.string.error_starting_playback_no_profile)
            return
        }

        val url = viewModel.getPlaybackUrl(true, castingProfileId)
        Timber.d("Casting recording from server with url $url")
        val mediaInfo = MediaInfo.Builder(url)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType("video/webm")
                .setMetadata(movieMetadata)
                .setStreamDuration(recording.stop - recording.start)
                .build()

        val remoteMediaClient = castSession.remoteMediaClient
        if (remoteMediaClient == null) {
            binding.progressBar.isVisible = false
            binding.status.setText(R.string.cast_error_no_media_client_available)
            return
        }

        remoteMediaClient.registerCallback(CastRemoteMediaClientCallback(remoteMediaClient))

        val mediaLoadOptions = MediaLoadOptions.Builder()
                .setAutoplay(true)
                .setPlayPosition(0)
                .build()

        remoteMediaClient.registerCallback(object : RemoteMediaClient.Callback() {
            override fun onStatusUpdated() {
                Timber.d("Status updated")
                val intent = Intent(this@CastRecordingActivity, ExpandedControlsActivity::class.java)
                startActivity(intent)
                remoteMediaClient.unregisterCallback(this)
                finish()
            }

            override fun onSendingRemoteMediaRequest() {
                Timber.d("Sending remote media request")
            }
        })

        remoteMediaClient.load(mediaInfo, mediaLoadOptions)
    }
}
