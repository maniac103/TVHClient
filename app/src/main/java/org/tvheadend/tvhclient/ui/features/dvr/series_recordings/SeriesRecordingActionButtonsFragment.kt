package org.tvheadend.tvhclient.ui.features.dvr.series_recordings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import org.tvheadend.data.entity.SeriesRecordingWithChannel
import org.tvheadend.tvhclient.databinding.SeriesRecordingActionButtonsFragmentBinding
import org.tvheadend.tvhclient.ui.base.BaseFragment
import org.tvheadend.tvhclient.ui.common.editSelectedSeriesRecording
import org.tvheadend.tvhclient.ui.common.interfaces.RecordingRemovedInterface
import org.tvheadend.tvhclient.ui.common.showConfirmationToRemoveSelectedSeriesRecording
import timber.log.Timber

class SeriesRecordingActionButtonsFragment : BaseFragment(), RecordingRemovedInterface {
    private lateinit var binding: SeriesRecordingActionButtonsFragmentBinding
    private lateinit var seriesRecordingViewModel: SeriesRecordingViewModel
    private var recording: SeriesRecordingWithChannel? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = SeriesRecordingActionButtonsFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity()
        seriesRecordingViewModel = ViewModelProvider(activity)[SeriesRecordingViewModel::class.java]
        seriesRecordingViewModel.recordingLiveData.observe(viewLifecycleOwner) {
            Timber.d("View model returned a recording")
            recording = it
        }

        binding.remove.setOnClickListener {
            recording?.let { showConfirmationToRemoveSelectedSeriesRecording(activity, it.base, this) }
        }
        binding.edit.setOnClickListener {
            recording?.let { editSelectedSeriesRecording(activity, it.id) }
        }
    }

    override fun onRecordingRemoved() {
        // no-op
    }
}