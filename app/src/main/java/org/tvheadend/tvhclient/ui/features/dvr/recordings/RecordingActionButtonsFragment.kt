package org.tvheadend.tvhclient.ui.features.dvr.recordings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import org.tvheadend.data.entity.RecordingWithChannel
import org.tvheadend.tvhclient.databinding.RecordingActionButtonsFragmentBinding
import org.tvheadend.tvhclient.ui.base.BaseFragment
import org.tvheadend.tvhclient.ui.common.castSelectedRecording
import org.tvheadend.tvhclient.ui.common.downloadSelectedRecording
import org.tvheadend.tvhclient.ui.common.editSelectedRecording
import org.tvheadend.tvhclient.ui.common.interfaces.RecordingRemovedInterface
import org.tvheadend.tvhclient.ui.common.playSelectedRecording
import org.tvheadend.tvhclient.ui.common.showConfirmationToCancelSelectedRecording
import org.tvheadend.tvhclient.ui.common.showConfirmationToRemoveSelectedRecording
import org.tvheadend.tvhclient.ui.common.showConfirmationToStopSelectedRecording
import org.tvheadend.tvhclient.util.extensions.getCastSession
import timber.log.Timber

class RecordingActionButtonsFragment : BaseFragment(), RecordingRemovedInterface {
    private lateinit var binding: RecordingActionButtonsFragmentBinding
    private lateinit var recordingViewModel: RecordingViewModel
    private var recording: RecordingWithChannel? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = RecordingActionButtonsFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity()
        recordingViewModel = ViewModelProvider(activity)[RecordingViewModel::class.java]
        recordingViewModel.recordingLiveData.observe(viewLifecycleOwner) {
            Timber.d("View model returned a recording")
            recording = it
            updateActionButtons()
        }

        globalStatusViewModel.connectionToServerAvailableLiveData.observe(viewLifecycleOwner) { isAvailable ->
            isConnectionToServerAvailable = isAvailable
            updateActionButtons()
        }

        binding.play.setOnClickListener {
            recording?.let { playSelectedRecording(activity, it.id) }
        }
        binding.cast.setOnClickListener {
            recording?.let { castSelectedRecording(activity, it.id) }
        }
        binding.stop.setOnClickListener {
            showConfirmationToStopSelectedRecording(activity, recording?.base, null)
        }
        binding.cancel.setOnClickListener {
            showConfirmationToCancelSelectedRecording(activity, recording?.base, null)
        }
        binding.remove.setOnClickListener {
            showConfirmationToRemoveSelectedRecording(activity, recording?.base, this)
        }
        binding.download.setOnClickListener {
            recording?.let { downloadSelectedRecording(activity, it.id) }
        }
        binding.edit.setOnClickListener {
            recording?.let { editSelectedRecording(activity, it.id) }
        }
    }

    override fun onRecordingRemoved() {
        // no-op
    }

    private fun updateActionButtons() {
        val recording = recording

        binding.play.isVisible = isConnectionToServerAvailable && when {
            recording?.isCompleted == true -> true
            recording?.isRecording == true -> true
            recording != null && recording.dataSize > 0 -> true
            else -> false
        }
        binding.cast.isVisible = binding.play.isVisible && context?.getCastSession() != null
        binding.edit.isVisible = isConnectionToServerAvailable && (recording?.isScheduled == true || recording?.isRecording == true)
        binding.remove.isVisible = isConnectionToServerAvailable && recording?.isCompleted == true
        binding.cancel.isVisible = isConnectionToServerAvailable && recording?.isScheduled == true && !recording.isRecording
        binding.stop.isVisible = isConnectionToServerAvailable && recording?.isRecording == true
        binding.download.isVisible = isConnectionToServerAvailable && recording?.isCompleted == true
    }
}