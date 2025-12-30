package org.tvheadend.tvhclient.ui.features.dvr.timer_recordings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import org.tvheadend.data.ServerCapabilities
import org.tvheadend.data.entity.TimerRecordingWithChannel
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.TimerRecordingDetailsFragmentBinding
import org.tvheadend.tvhclient.ui.base.BaseFragment
import org.tvheadend.tvhclient.ui.common.*
import org.tvheadend.tvhclient.ui.common.interfaces.ClearSearchResultsOrPopBackStackInterface
import org.tvheadend.tvhclient.ui.common.interfaces.RecordingRemovedInterface
import org.tvheadend.tvhclient.ui.features.dvr.minutesToTimeMillis
import org.tvheadend.tvhclient.util.extensions.applyText
import org.tvheadend.tvhclient.util.extensions.applyTextAndAdjustVisibility
import org.tvheadend.tvhclient.util.extensions.determineDaysOfWeekText
import org.tvheadend.tvhclient.util.extensions.determinePriorityText
import org.tvheadend.tvhclient.util.extensions.formatTime

class TimerRecordingDetailsFragment : BaseFragment(), RecordingRemovedInterface, ClearSearchResultsOrPopBackStackInterface {

    private lateinit var timerRecordingViewModel: TimerRecordingViewModel
    private var recording: TimerRecordingWithChannel? = null
    private lateinit var binding: TimerRecordingDetailsFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = TimerRecordingDetailsFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        timerRecordingViewModel = ViewModelProvider(requireActivity())[TimerRecordingViewModel::class.java]

        if (!isDualPane) {
            toolbarInterface.setTitle(getString(R.string.details))
            toolbarInterface.setSubtitle(null)
        }

        arguments?.let {
            timerRecordingViewModel.currentIdLiveData.value = it.getString("id", "")
        }

        timerRecordingViewModel.recordingLiveData.observe(viewLifecycleOwner) {
            recording = it
            showRecordingDetails()
        }
    }

    private fun showRecordingDetails() {
        recording?.let { rec ->
            val caps = ServerCapabilities(htspVersion)
            binding.disabled.isVisible = caps.recordingEnabledSupported == true && !rec.isEnabled
            binding.titleLabel.isVisible = !isDualPane
            binding.title.apply {
                text = rec.title?.takeIf { it.isNotEmpty() } ?: context.getString(R.string.hint_not_set)
                isVisible = !isDualPane
            }
            binding.name.applyTextAndAdjustVisibility { rec.name?.takeIf { it.isNotEmpty() } ?: rec.title }
            binding.channel.applyText { rec.channelName ?: getString(R.string.all_channels) }
            binding.startTime.applyText { formatTime(minutesToTimeMillis(rec.start)) }
            binding.stopTime.applyText { formatTime(minutesToTimeMillis(rec.stop)) }
            binding.duration.applyText { getString(R.string.minutes, rec.duration) }
            binding.daysOfWeek.applyText { determineDaysOfWeekText(rec.daysOfWeek) }
            binding.priority.applyText { determinePriorityText(rec.priority) }
            binding.directory.applyText { rec.directory ?: getString(R.string.hint_not_set) }

            // The toolbar is hidden as a default to prevent pressing any icons if no recording
            // has been loaded yet. The toolbar is shown here because a recording was loaded
            binding.nestedToolbar.isVisible = true
            activity?.invalidateOptionsMenu()
        } ?: run {
            binding.scrollview.isVisible = false
            binding.status.text = getString(R.string.error_loading_recording_details)
            binding.status.isVisible = true
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val recording = this.recording ?: return
        preparePopupOrToolbarSearchMenu(menu, recording.title, isConnectionToServerAvailable)

        binding.nestedToolbar.menu.findItem(R.id.menu_edit_recording)?.isVisible = true
        binding.nestedToolbar.menu.findItem(R.id.menu_remove_recording)?.isVisible = true
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.external_search_options_menu, menu)
        binding.nestedToolbar.inflateMenu(R.menu.recording_details_toolbar_menu)
        binding.nestedToolbar.setOnMenuItemClickListener { this.onOptionsItemSelected(it) }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val ctx = context ?: return super.onOptionsItemSelected(item)
        val recording = this.recording?.base ?: return super.onOptionsItemSelected(item)

        return when (item.itemId) {
            R.id.menu_edit_recording -> editSelectedTimerRecording(requireActivity(), recording.id)
            R.id.menu_remove_recording -> showConfirmationToRemoveSelectedTimerRecording(ctx, recording, this)

            R.id.menu_search_imdb -> return searchTitleOnImdbWebsite(ctx, recording.title)
            R.id.menu_search_fileaffinity -> return searchTitleOnFileAffinityWebsite(ctx, recording.title)
            R.id.menu_search_youtube -> return searchTitleOnYoutube(ctx, recording.title)
            R.id.menu_search_google -> return searchTitleOnGoogle(ctx, recording.title)
            R.id.menu_search_epg -> return searchTitleInTheLocalDatabase(requireActivity(), baseViewModel, recording.title)
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onRecordingRemoved() {
        if (!isDualPane) {
            activity?.onBackPressed()
        } else {
            val detailsFragment = activity?.supportFragmentManager?.findFragmentById(R.id.details)
            if (detailsFragment != null) {
                activity?.supportFragmentManager?.commit {
                    remove(detailsFragment)
                }
            }
        }
    }

    companion object {

        fun newInstance(id: String): TimerRecordingDetailsFragment {
            val f = TimerRecordingDetailsFragment()
            val args = Bundle()
            args.putString("id", id)
            f.arguments = args
            return f
        }
    }
}
