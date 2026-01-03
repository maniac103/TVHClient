package org.tvheadend.tvhclient.ui.features.information

import android.app.ActivityManager
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.getSystemService
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.StatusFragmentBinding
import org.tvheadend.tvhclient.service.ConnectionService
import org.tvheadend.tvhclient.ui.base.BaseFragment
import org.tvheadend.tvhclient.ui.common.interfaces.LayoutControlInterface
import org.tvheadend.tvhclient.ui.features.dvr.recordings.RecordingViewModel
import org.tvheadend.tvhclient.util.applyNavigationBarPadding
import org.tvheadend.tvhclient.util.extensions.applyText
import timber.log.Timber

class StatusFragment : BaseFragment() {

    private lateinit var binding: StatusFragmentBinding
    private val loadDataHandler = Handler(Looper.getMainLooper())

    private val loadDataTask: Runnable = Runnable {
        val context = context ?: return@Runnable
        val am = context.getSystemService<ActivityManager>() ?: return@Runnable
        val runningAppProcessInfo = am.runningAppProcesses[0]
        if (runningAppProcessInfo.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
            Timber.d("Application is in the foreground, starting service to get updated subscriptions and input information")
            val intent = Intent(activity, ConnectionService::class.java)
                .setAction("getSubscriptions")
            context.startService(intent)

            intent.action = "getInputs"
            context.startService(intent)
        }

        Timber.d("Restarting additional information update handler in 60s")
        loadDataHandler.postDelayed(loadDataTask, 60000)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = StatusFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.content.applyNavigationBarPadding()

        val activity = requireActivity()
        (activity as? LayoutControlInterface)?.forceSingleScreenLayout()

        toolbarInterface.setTitle(getString(R.string.status))
        toolbarInterface.setSubtitle(null)

        globalStatusViewModel.connectionLiveData.observe(viewLifecycleOwner) { conn ->
            conn?.let {
                binding.connectionName.text = conn.name
                binding.connectionUrl.text = conn.serverUrl
            }
        }

        globalStatusViewModel.connectionToServerAvailableLiveData.observe(viewLifecycleOwner) { connectionAvailable ->
            Timber.d("Connection to server availability changed to $connectionAvailable")
            if (connectionAvailable) {
                Timber.d("Starting additional information update handler")
                loadDataHandler.post(loadDataTask)
            } else {
                loadDataHandler.removeCallbacks(loadDataTask)
            }
            binding.contentContainer.isVisible = connectionAvailable
            binding.notConnected.isVisible = !connectionAvailable
        }

        globalStatusViewModel.htspVersionLiveData.observe(viewLifecycleOwner) { version ->
            version?.let {
                binding.seriesRecordings.isVisible = htspVersion >= 13
                binding.timerRecordings.isVisible = htspVersion >= 18
            }
        }

        val statusViewModel = ViewModelProvider(activity)[StatusViewModel::class.java]
        statusViewModel.channelCount.observe(viewLifecycleOwner) { count ->
            binding.channels.applyText { "$count ${getString(R.string.available)}" }
        }
        statusViewModel.programCount.observe(viewLifecycleOwner) { count ->
            binding.programs.applyText { resources.getQuantityString(R.plurals.programs, count ?: 0, count) }
        }
        statusViewModel.seriesRecordingCount.observe(viewLifecycleOwner) { count ->
            binding.seriesRecordings.applyText { resources.getQuantityString(R.plurals.series_recordings, count, count) }
        }
        statusViewModel.timerRecordingCount.observe(viewLifecycleOwner) { count ->
            binding.timerRecordings.applyText { resources.getQuantityString(R.plurals.timer_recordings, count, count) }
        }
        statusViewModel.completedRecordingCount.observe(viewLifecycleOwner) { count ->
            binding.completedRecordings.applyText { resources.getQuantityString(R.plurals.completed_recordings, count, count) }
        }
        statusViewModel.scheduledRecordingCount.observe(viewLifecycleOwner) { count ->
            binding.upcomingRecordings.applyText { resources.getQuantityString(R.plurals.upcoming_recordings, count, count) }
        }
        statusViewModel.failedRecordingCount.observe(viewLifecycleOwner) { count ->
            binding.failedRecordings.applyText { resources.getQuantityString(R.plurals.failed_recordings, count, count) }
        }
        statusViewModel.removedRecordingCount.observe(viewLifecycleOwner) { count ->
            binding.removedRecordings.applyText { resources.getQuantityString(R.plurals.removed_recordings, count, count) }
        }
        statusViewModel.serverStatusLiveData.observe(viewLifecycleOwner) { serverStatus ->
            serverStatus?.let { status ->
                binding.serverApiVersion.text = status.htspVersion.toString()
                binding.server.text = "${status.serverName} ${status.serverVersion}"

                val formatSpaceValue = { value: Long, labelResId: Int ->
                    val valueText = if (value > (1024 * 1024)) {
                        "${(value / 1024 / 1024 / 1024)} GB"
                    } else {
                        "${(value / 1024 / 1024)} MB"
                    }
                    valueText + " " + getString(labelResId)
                }
                binding.freeDiskspace.text = formatSpaceValue(status.freeDiskSpace, R.string.available)
                binding.totalDiskspace.text = formatSpaceValue(status.totalDiskSpace, R.string.total)
            }
        }

        // Get the programs that are currently being recorded
        val recordingViewModel = ViewModelProvider(activity)[RecordingViewModel::class.java]
        recordingViewModel.scheduledRecordings.observe(viewLifecycleOwner) { recordings ->
            if (recordings != null) {
                val currentRecText = StringBuilder()
                recordings
                    .filter { it.isRecording }
                    .forEach { rec ->
                        currentRecText
                            .append(getString(R.string.currently_recording))
                            .append(": ")
                            .append(rec.title)
                        statusViewModel.getChannelById(rec.channelId)?.let { channel ->
                            currentRecText
                                .append(" (")
                                .append(getString(R.string.channel))
                                .append(" ")
                                .append(channel.name)
                                .append(")\n")
                        }
                    }
                // Show which programs are being recorded
                binding.currentlyRecording.text =
                    if (currentRecText.isNotEmpty()) currentRecText.toString() else getString(R.string.nothing)
            }
        }
    }


    override fun onPause() {
        super.onPause()
        loadDataHandler.removeCallbacks(loadDataTask)
    }
}
