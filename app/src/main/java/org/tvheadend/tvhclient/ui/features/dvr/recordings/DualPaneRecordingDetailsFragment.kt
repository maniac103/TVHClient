package org.tvheadend.tvhclient.ui.features.dvr.recordings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import org.tvheadend.tvhclient.databinding.DualPaneRecordingDetailsFragmentBinding
import org.tvheadend.tvhclient.ui.base.BaseFragment
import org.tvheadend.tvhclient.util.applyNavigationBarPadding
import timber.log.Timber

class DualPaneRecordingDetailsFragment : BaseFragment() {
    private lateinit var binding: DualPaneRecordingDetailsFragmentBinding
    private lateinit var recordingViewModel: RecordingViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DualPaneRecordingDetailsFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.content.applyNavigationBarPadding()

        recordingViewModel = ViewModelProvider(requireActivity())[RecordingViewModel::class.java]
        recordingViewModel.currentIdLiveData.value = requireArguments().getInt("id")

        recordingViewModel.recordingLiveData.observe(viewLifecycleOwner) { rec ->
            Timber.d("View model returned a recording")
            if (rec != null) {
                binding.title.text = rec.title
                binding.subtitle.text = rec.subtitle
            }
        }
    }

    companion object {
        fun newInstance(dvrId: Int) = DualPaneRecordingDetailsFragment().apply {
            arguments = bundleOf("id" to dvrId)
        }
    }
}