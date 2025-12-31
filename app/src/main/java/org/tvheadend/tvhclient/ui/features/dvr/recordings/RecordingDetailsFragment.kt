package org.tvheadend.tvhclient.ui.features.dvr.recordings

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import org.tvheadend.data.ServerCapabilities
import org.tvheadend.data.entity.RecordingWithChannel
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.RecordingDetailsFragmentBinding
import org.tvheadend.tvhclient.ui.base.BaseFragment
import org.tvheadend.tvhclient.util.extensions.applyIcon
import org.tvheadend.tvhclient.util.extensions.applyText
import org.tvheadend.tvhclient.util.extensions.applyTextAndAdjustVisibility
import org.tvheadend.tvhclient.util.extensions.determineContentTypeColor
import org.tvheadend.tvhclient.util.extensions.determineContentTypeText
import org.tvheadend.tvhclient.util.extensions.determineDataErrorText
import org.tvheadend.tvhclient.util.extensions.determineDataSizeText
import org.tvheadend.tvhclient.util.extensions.determineRecordingStateText
import org.tvheadend.tvhclient.util.extensions.determineStreamErrorText
import org.tvheadend.tvhclient.util.extensions.determineSubscriptionErrorText
import org.tvheadend.tvhclient.util.extensions.formatDate
import org.tvheadend.tvhclient.util.extensions.formatStartStopTime
import org.tvheadend.tvhclient.util.extensions.interpretColoredText
import timber.log.Timber

class RecordingDetailsFragment : BaseFragment() {
    private lateinit var binding: RecordingDetailsFragmentBinding
    private lateinit var recordingViewModel: RecordingViewModel
    private var recording: RecordingWithChannel? = null
    private var capabilities: ServerCapabilities? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = RecordingDetailsFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recordingViewModel = ViewModelProvider(requireActivity())[RecordingViewModel::class.java]
        recordingViewModel.recordingLiveData.observe(viewLifecycleOwner) {
            Timber.d("View model returned a recording: $it")
            recording = it
            updateContent()
        }

        globalStatusViewModel.htspVersionLiveData.observe(viewLifecycleOwner) {
            capabilities = it?.let { ServerCapabilities(it) }
            updateContent()
        }
    }

    private fun updateContent() {
        val recording = recording ?: return
        val caps = capabilities ?: return

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
        binding.contentType.applyTextAndAdjustVisibility { recording.determineContentTypeText(this) }
        binding.contentTypeColor.apply {
            val color = recording.determineContentTypeColor(context)
            isVisible = color != null
            color?.let { imageTintList = ColorStateList.valueOf(it) }
        }
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
}