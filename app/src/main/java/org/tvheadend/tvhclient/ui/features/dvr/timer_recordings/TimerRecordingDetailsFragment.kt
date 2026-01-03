package org.tvheadend.tvhclient.ui.features.dvr.timer_recordings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import org.tvheadend.data.entity.TimerRecordingWithChannel
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.TimerRecordingDetailsFragmentBinding
import org.tvheadend.tvhclient.ui.base.BaseFragment
import org.tvheadend.tvhclient.ui.common.GlobalStatusViewModel
import org.tvheadend.tvhclient.ui.common.preparePopupOrToolbarSearchMenu
import org.tvheadend.tvhclient.ui.common.searchTitleInTheLocalDatabase
import org.tvheadend.tvhclient.ui.common.searchTitleOnFileAffinityWebsite
import org.tvheadend.tvhclient.ui.common.searchTitleOnGoogle
import org.tvheadend.tvhclient.ui.common.searchTitleOnImdbWebsite
import org.tvheadend.tvhclient.ui.common.searchTitleOnYoutube
import org.tvheadend.tvhclient.util.extensions.applyIcon
import org.tvheadend.tvhclient.util.extensions.applyText
import org.tvheadend.tvhclient.util.extensions.determineDaysOfWeekText
import org.tvheadend.tvhclient.util.extensions.determinePriorityText
import org.tvheadend.tvhclient.util.extensions.formatStartStopTime

class TimerRecordingDetailsFragment : BaseFragment(), MenuProvider {
    private lateinit var binding: TimerRecordingDetailsFragmentBinding
    private lateinit var timerRecordingViewModel: TimerRecordingViewModel
    private var recording: TimerRecordingWithChannel? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = TimerRecordingDetailsFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity()

        timerRecordingViewModel = ViewModelProvider(activity)[TimerRecordingViewModel::class.java]
        timerRecordingViewModel.recordingLiveData.observe(viewLifecycleOwner) {
            recording = it
            updateContent()
        }

        activity.addMenuProvider(this, viewLifecycleOwner)
    }

    override fun onConnectedServerChanged(data: GlobalStatusViewModel.ConnectedServerData?) {
        super.onConnectedServerChanged(data)
        updateContent()
        activity?.invalidateMenu()
    }

    private fun updateContent() {
        val recording = recording ?: return
        val caps = serverData?.capabilities ?: return

        binding.disabled.isVisible = caps.recordingEnabledSupported && !recording.isEnabled
        binding.disabledIcon.isVisible = binding.disabled.isVisible

        binding.channel.applyText { recording.channelName ?: getString(R.string.all_channels) }
        binding.channelIcon.applyIcon(recording.channelIcon)

        binding.startStopTime.applyText { formatStartStopTime(recording.start, recording.stop) }
        binding.duration.applyText { getString(R.string.minutes, recording.duration) }
        binding.daysOfWeek.applyText { determineDaysOfWeekText(recording.daysOfWeek) }

        binding.priority.applyText { determinePriorityText(recording.priority) }
        binding.directory.applyText { recording.directory ?: getString(R.string.hint_not_set) }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.external_search_options_menu, menu)
        preparePopupOrToolbarSearchMenu(menu, recording?.title, serverData)
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        val recording = recording ?: return false
        val activity = activity ?: return false

        return when (item.itemId) {
            R.id.menu_search_imdb -> searchTitleOnImdbWebsite(activity, recording.title)
            R.id.menu_search_fileaffinity -> searchTitleOnFileAffinityWebsite(activity, recording.title)
            R.id.menu_search_youtube -> searchTitleOnYoutube(activity, recording.title)
            R.id.menu_search_google -> searchTitleOnGoogle(activity, recording.title)
            R.id.menu_search_epg -> searchTitleInTheLocalDatabase(activity, baseViewModel, recording.title)
            else -> false
        }
    }
}
