package org.tvheadend.tvhclient.ui.features.dvr.series_recordings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import org.tvheadend.data.entity.TimerRecordingWithChannel
import org.tvheadend.tvhclient.databinding.TimerRecordingActionButtonsFragmentBinding
import org.tvheadend.tvhclient.ui.base.BaseFragment
import org.tvheadend.tvhclient.ui.common.editSelectedTimerRecording
import org.tvheadend.tvhclient.ui.common.interfaces.RecordingRemovedInterface
import org.tvheadend.tvhclient.ui.common.showConfirmationToRemoveSelectedTimerRecording
import org.tvheadend.tvhclient.ui.features.dvr.timer_recordings.TimerRecordingViewModel
import timber.log.Timber

class TimerRecordingActionButtonsFragment : BaseFragment(), RecordingRemovedInterface {
    private lateinit var binding: TimerRecordingActionButtonsFragmentBinding
    private lateinit var timerRecordingViewModel: TimerRecordingViewModel
    private var recording: TimerRecordingWithChannel? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = TimerRecordingActionButtonsFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity()
        timerRecordingViewModel = ViewModelProvider(activity)[TimerRecordingViewModel::class.java]
        timerRecordingViewModel.recordingLiveData.observe(viewLifecycleOwner) {
            Timber.d("View model returned a recording")
            recording = it
        }

        binding.remove.setOnClickListener {
            recording?.let { showConfirmationToRemoveSelectedTimerRecording(activity, it.base, this) }
        }
        binding.edit.setOnClickListener {
            recording?.let { editSelectedTimerRecording(activity, it.id) }
        }
    }

    override fun onRecordingRemoved() {
        // no-op
    }
}