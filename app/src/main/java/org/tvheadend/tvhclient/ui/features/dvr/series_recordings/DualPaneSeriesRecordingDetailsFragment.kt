package org.tvheadend.tvhclient.ui.features.dvr.series_recordings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import org.tvheadend.tvhclient.databinding.DualPaneSeriesRecordingDetailsFragmentBinding
import org.tvheadend.tvhclient.ui.base.BaseFragment
import org.tvheadend.tvhclient.util.applyNavigationBarPadding
import timber.log.Timber

class DualPaneSeriesRecordingDetailsFragment : BaseFragment() {
    private lateinit var binding: DualPaneSeriesRecordingDetailsFragmentBinding
    private lateinit var seriesRecordingViewModel: SeriesRecordingViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DualPaneSeriesRecordingDetailsFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.content.applyNavigationBarPadding()

        seriesRecordingViewModel = ViewModelProvider(requireActivity())[SeriesRecordingViewModel::class.java]
        seriesRecordingViewModel.currentIdLiveData.value = requireArguments().getString("id") ?: ""

        seriesRecordingViewModel.recordingLiveData.observe(viewLifecycleOwner) { rec ->
            Timber.d("View model returned a recording")
            if (rec != null) {
                binding.title.text = rec.title
                binding.name.text = rec.name
                binding.name.isVisible = rec.name != null && rec.name != rec.title
            }
        }
    }

    companion object {
        fun newInstance(id: String) = DualPaneSeriesRecordingDetailsFragment().apply {
            arguments = bundleOf("id" to id)
        }
    }
}
