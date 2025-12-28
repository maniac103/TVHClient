package org.tvheadend.tvhclient.ui.features.dvr.recordings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import org.tvheadend.data.ServerCapabilities
import org.tvheadend.data.entity.RecordingInterface
import org.tvheadend.data.entity.RecordingWithChannel
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.RecordingDetailsActivityBinding
import org.tvheadend.tvhclient.ui.base.BaseActivity
import org.tvheadend.tvhclient.ui.common.castSelectedRecording
import org.tvheadend.tvhclient.ui.common.downloadSelectedRecording
import org.tvheadend.tvhclient.ui.common.editSelectedRecording
import org.tvheadend.tvhclient.ui.common.interfaces.RecordingRemovedInterface
import org.tvheadend.tvhclient.ui.common.playSelectedRecording
import org.tvheadend.tvhclient.ui.common.preparePopupOrToolbarSearchMenu
import org.tvheadend.tvhclient.ui.common.searchTitleInTheLocalDatabase
import org.tvheadend.tvhclient.ui.common.searchTitleOnFileAffinityWebsite
import org.tvheadend.tvhclient.ui.common.searchTitleOnGoogle
import org.tvheadend.tvhclient.ui.common.searchTitleOnImdbWebsite
import org.tvheadend.tvhclient.ui.common.searchTitleOnYoutube
import org.tvheadend.tvhclient.ui.common.showConfirmationToCancelSelectedRecording
import org.tvheadend.tvhclient.ui.common.showConfirmationToRemoveSelectedRecording
import org.tvheadend.tvhclient.ui.common.showConfirmationToStopSelectedRecording
import org.tvheadend.tvhclient.util.applyNavigationBarPadding
import org.tvheadend.tvhclient.util.extensions.applyIcon
import org.tvheadend.tvhclient.util.extensions.applyText
import org.tvheadend.tvhclient.util.extensions.applyTextAndAdjustVisibility
import org.tvheadend.tvhclient.util.extensions.determineContentTypeText
import org.tvheadend.tvhclient.util.extensions.determineDataErrorText
import org.tvheadend.tvhclient.util.extensions.determineDataSizeText
import org.tvheadend.tvhclient.util.extensions.determineRecordingStateText
import org.tvheadend.tvhclient.util.extensions.determineStreamErrorText
import org.tvheadend.tvhclient.util.extensions.determineSubscriptionErrorText
import org.tvheadend.tvhclient.util.extensions.formatDate
import org.tvheadend.tvhclient.util.extensions.formatStartStopTime
import org.tvheadend.tvhclient.util.extensions.getCastSession
import org.tvheadend.tvhclient.util.extensions.interpretColoredText
import timber.log.Timber

class RecordingDetailsActivity : BaseActivity(), RecordingRemovedInterface {
    private lateinit var binding: RecordingDetailsActivityBinding

    private lateinit var recordingViewModel: RecordingViewModel

    private var recording: RecordingWithChannel? = null
    private var capabilities: ServerCapabilities? = null
    private var isConnectionToServerAvailable: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = RecordingDetailsActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar(binding.toolbar, binding.appBar)
        binding.content.applyNavigationBarPadding()

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu_cancel)
        }

        recordingViewModel = ViewModelProvider(this)[RecordingViewModel::class.java]
        recordingViewModel.currentIdLiveData.value = intent.getIntExtra("id", 0)

        recordingViewModel.recordingLiveData.observe(this) {
            Timber.d("View model returned a recording")
            recording = it
            updateContent()
            updateActionButtons()
            invalidateOptionsMenu()
        }

        globalStatusViewModel.connectionToServerAvailableLiveData.observe(this) { isAvailable ->
            Timber.d("Received live data, connection to server availability changed to $isAvailable")
            isConnectionToServerAvailable = isAvailable
            updateActionButtons()
            invalidateOptionsMenu()
        }

        globalStatusViewModel.htspVersionLiveData.observe(this) {
            capabilities = it?.let { ServerCapabilities(it) }
            updateContent()
            updateActionButtons()
        }

        binding.play.setOnClickListener {
            recording?.let { playSelectedRecording(this, it.id) }
        }
        binding.cast.setOnClickListener {
            recording?.let { castSelectedRecording(this, it.id) }
        }
        binding.stop.setOnClickListener {
            showConfirmationToStopSelectedRecording(this, recording?.base, null)
        }
        binding.cancel.setOnClickListener {
            showConfirmationToCancelSelectedRecording(this, recording?.base, null)
        }
        binding.remove.setOnClickListener {
            showConfirmationToRemoveSelectedRecording(this, recording?.base, this)
        }
        binding.download.setOnClickListener {
            recording?.let { downloadSelectedRecording(this, it.id) }
        }
        binding.edit.setOnClickListener {
            recording?.let { editSelectedRecording(this, it.id) }
        }
    }

    override fun onRecordingRemoved() {
        finish()
    }

    private fun updateContent() {
        val recording = recording ?: return
        val caps = capabilities ?: return

        supportActionBar?.apply {
            title = recording.title
            subtitle = recording.subtitle
        }
        binding.state.applyTextAndAdjustVisibility { recording.determineRecordingStateText(this) }
        binding.dataSize.applyTextAndAdjustVisibility { recording.determineDataSizeText(this) }
        binding.subscriptionError.applyTextAndAdjustVisibility { recording.determineSubscriptionErrorText(this) }
        binding.streamErrors.applyTextAndAdjustVisibility { recording.determineStreamErrorText(this) }
        binding.dataErrors.applyTextAndAdjustVisibility { recording.determineDataErrorText(this) }
        binding.disabled.isVisible = caps.recordingEnabledSupported && !recording.isEnabled
        binding.isSeriesRecording.isVisible = !recording.autorecId.isNullOrEmpty()
        binding.isTimerRecording.isVisible = !recording.timerecId.isNullOrEmpty()

        // Info card
        binding.channelIcon.applyIcon(recording.channelIcon)
        binding.channel.applyTextAndAdjustVisibility { recording.channelName ?: getString(R.string.all_channels) }
        binding.contentType.applyTextAndAdjustVisibility { determineContentTypeText(recording.contentType * 16) }
        binding.episode.applyTextAndAdjustVisibility(recording.episode)
        binding.summary.applyTextAndAdjustVisibility(recording.summary)
        binding.summaryIcon.isVisible = binding.episode.text.isNotEmpty() || binding.summary.text.isNotEmpty()
        binding.summaryBarrier.isVisible = binding.summaryIcon.isVisible

        binding.description.applyTextAndAdjustVisibility { interpretColoredText(recording.description) }
        binding.descriptionIcon.isVisible = binding.description.isVisible
        binding.descriptionBarrier.isVisible = binding.descriptionIcon.isVisible

        binding.comment.applyTextAndAdjustVisibility(recording.comment)
        binding.commentIcon.isVisible = binding.comment.isVisible

        // Date/time card
        binding.date.applyText { formatDate(recording.start) }
        binding.time.applyText { formatStartStopTime(recording.start, recording.stop) }
        binding.duration.applyText { getString(R.string.minutes, recording.duration) }
    }

    private fun updateActionButtons() {
        val recording = recording

        binding.play.isVisible = isConnectionToServerAvailable && when {
            recording?.isCompleted == true -> true
            recording?.isRecording == true -> true
            recording != null && recording.dataSize > 0 -> true
            else -> false
        }
        binding.cast.isVisible = binding.play.isVisible && getCastSession() != null
        binding.edit.isVisible = isConnectionToServerAvailable && (recording?.isScheduled == true || recording?.isRecording == true)
        binding.remove.isVisible = isConnectionToServerAvailable && recording?.isCompleted == true
        binding.cancel.isVisible = isConnectionToServerAvailable && recording?.isScheduled == true && !recording.isRecording
        binding.stop.isVisible = isConnectionToServerAvailable && recording?.isRecording == true
        binding.download.isVisible = isConnectionToServerAvailable && recording?.isCompleted == true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.external_search_options_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        preparePopupOrToolbarSearchMenu(menu, recording?.title, isConnectionToServerAvailable)
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }

        val recording = recording ?: return super.onOptionsItemSelected(item)

        return when (item.itemId) {
            R.id.menu_search_imdb -> searchTitleOnImdbWebsite(this, recording.title)
            R.id.menu_search_fileaffinity -> searchTitleOnFileAffinityWebsite(this, recording.title)
            R.id.menu_search_youtube -> searchTitleOnYoutube(this, recording.title)
            R.id.menu_search_google -> searchTitleOnGoogle(this, recording.title)
            R.id.menu_search_epg -> searchTitleInTheLocalDatabase(this, baseViewModel, recording.title)

            else -> return super.onOptionsItemSelected(item)
        }
    }

    companion object {
        fun makeIntent(context: Context, recording: RecordingInterface) =
            Intent(context, RecordingDetailsActivity::class.java)
                .putExtra("id", recording.id)
    }
}
