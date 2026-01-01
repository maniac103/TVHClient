package org.tvheadend.tvhclient.ui.features.dvr.timer_recordings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.tvheadend.data.ServerCapabilities
import org.tvheadend.data.entity.ServerProfile
import org.tvheadend.data.entity.TimerRecordingWithChannel
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.TimerRecordingEditActivityBinding
import org.tvheadend.tvhclient.ui.base.BaseActivity
import org.tvheadend.tvhclient.ui.features.dvr.getSelectedProfileId
import org.tvheadend.tvhclient.ui.features.dvr.getTimeStringFromTimeInMillis
import org.tvheadend.tvhclient.ui.features.dvr.handleChannelListSelection
import org.tvheadend.tvhclient.ui.features.dvr.handlePrioritySelection
import org.tvheadend.tvhclient.ui.features.dvr.handleRecordingProfileSelection
import org.tvheadend.tvhclient.ui.features.dvr.minutesToTimeMillis
import org.tvheadend.tvhclient.ui.features.dvr.showTimePicker
import org.tvheadend.tvhclient.util.applyNavigationBarPadding
import org.tvheadend.tvhclient.util.extensions.afterTextChanged
import org.tvheadend.tvhclient.util.extensions.applyText
import org.tvheadend.tvhclient.util.extensions.determinePriorityText
import org.tvheadend.tvhclient.util.extensions.observeOnce
import org.tvheadend.tvhclient.util.extensions.sendSnackbarMessage
import org.tvheadend.tvhclient.util.livedata.CombinedPairLiveData
import timber.log.Timber

class TimerRecordingEditActivity : BaseActivity() {

    private lateinit var binding: TimerRecordingEditActivityBinding
    private lateinit var timerRecordingViewModel: TimerRecordingViewModel
    private lateinit var recordingProfilesList: Array<String>

    private lateinit var caps: ServerCapabilities
    private var profile: ServerProfile? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = TimerRecordingEditActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar(binding.toolbar, binding.appBar)
        binding.content.applyNavigationBarPadding()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        timerRecordingViewModel = ViewModelProvider(this)[TimerRecordingViewModel::class.java]
        recordingProfilesList = timerRecordingViewModel.getRecordingProfileNames()

        profile = timerRecordingViewModel.getRecordingProfile()
        timerRecordingViewModel.recordingProfileNameId = getSelectedProfileId(profile, recordingProfilesList)

        val id = intent.getStringExtra("id") ?: ""
        if (savedInstanceState == null) {
            timerRecordingViewModel.currentIdLiveData.value = id
        }

        setTitle(getString(if (id.isEmpty()) R.string.add_recording else R.string.edit_recording))

        val inputLiveData = CombinedPairLiveData(
            timerRecordingViewModel.recordingLiveData,
            globalStatusViewModel.htspVersionLiveData
        ) { rec, htspVersion -> rec to ServerCapabilities(htspVersion) }

        inputLiveData.observeOnce(this) { (rec, caps) ->
            if (rec == null) {
                finish()
            } else {
                this.caps = caps
                updateUI(rec)
            }
        }
    }

    private fun updateUI(recording: TimerRecordingWithChannel) {
        binding.enabledWrapper.isVisible = caps.timerRecordingEnabledSupported
        binding.isEnabled.isChecked = recording.isEnabled

        binding.title.setText(recording.title)
        binding.name.setText(recording.name)

        binding.directoryWrapper.isVisible = caps.recordingDirectorySupported
        binding.directory.setText(recording.directory)

        binding.channelName.applyText { recording.channelName ?: getString(R.string.all_channels) }
        binding.channelName.setOnClickListener {
            // Determine if the server supports recording on all channels
            handleChannelListSelection(
                this,
                timerRecordingViewModel.getChannelList(),
                caps.recordingOnAllChannelsSupported
            ) { channel ->
                recording.channelId = channel.id
                binding.channelName.setText(channel.name)
            }
        }

        binding.priority.applyText { determinePriorityText(recording.priority) }
        binding.priority.setOnClickListener {
            handlePrioritySelection(this, recording.priority) { prio ->
                recording.priority = prio
                binding.priority.applyText { determinePriorityText(recording.priority) }
            }
        }

        binding.dvrConfigWrapper.isVisible = recordingProfilesList.isNotEmpty()
        if (recordingProfilesList.isNotEmpty()) {
            binding.dvrConfig.setText(recordingProfilesList[timerRecordingViewModel.recordingProfileNameId])
            binding.dvrConfig.setOnClickListener {
                handleRecordingProfileSelection(
                    this,
                    recordingProfilesList,
                    timerRecordingViewModel.recordingProfileNameId
                ) { profileIndex ->
                    timerRecordingViewModel.recordingProfileNameId = profileIndex
                    binding.dvrConfig.setText(recordingProfilesList[profileIndex])
                }
            }
        }

        binding.startTime.setText(getTimeStringFromTimeInMillis(minutesToTimeMillis(recording.start)))
        binding.startTime.setOnClickListener {
            val picker = showTimePicker(minutesToTimeMillis(recording.start))
            picker.addOnPositiveButtonClickListener {
                recording.start = 60L * picker.hour + picker.minute
                handleStartStopTimeUpdate(recording)
            }
            picker.show(supportFragmentManager, "startTime")
        }

        binding.stopTime.setText(getTimeStringFromTimeInMillis(minutesToTimeMillis(recording.stop)))
        binding.stopTime.setOnClickListener {
            val picker = showTimePicker(minutesToTimeMillis(recording.stop))
            picker.addOnPositiveButtonClickListener {
                recording.stop = 60L * picker.hour + picker.minute
                handleStartStopTimeUpdate(recording)
            }
            picker.show(supportFragmentManager, "stopTime")
        }

        binding.daysOfWeek.selectedDaysBitmask = recording.daysOfWeek
        binding.daysOfWeek.setOnSelectedDaysChangedListener { recording.daysOfWeek = it }

        binding.timeEnabled.isChecked = timerRecordingViewModel.isTimeEnabled
        handleTimeEnabledClick(binding.timeEnabled.isChecked)
        binding.timeEnabled.setOnClickListener {
            handleTimeEnabledClick(binding.timeEnabled.isChecked)
        }

        binding.title.afterTextChanged { recording.title = it }
        binding.name.afterTextChanged { recording.name = it }
        binding.directory.afterTextChanged { recording.directory = it }
        binding.isEnabled.setOnCheckedChangeListener { _, isChecked -> recording.isEnabled = isChecked }
        binding.save.setOnClickListener { save(recording) }
    }

    private fun handleTimeEnabledClick(checked: Boolean) {
        Timber.d("Setting time enabled ${binding.timeEnabled.isChecked}")
        timerRecordingViewModel.isTimeEnabled = checked

        binding.startTimeWrapper.isEnabled = checked
        binding.stopTimeWrapper.isEnabled = checked
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                cancel()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun save(recording: TimerRecordingWithChannel) {
        if (recording.title.isNullOrEmpty()) {
            sendSnackbarMessage(R.string.error_empty_title)
            return
        }

        val intent = timerRecordingViewModel.getIntentData(this, recording.base)

        // Add the recording profile if available and enabled
        if (profile != null && caps.recordingProfileSupported && binding.dvrConfig.text.isNotEmpty()) {
            intent.putExtra("configName", binding.dvrConfig.text.toString())
        }

        // Update the recording in case the id is not empty, otherwise add a new one.
        // When adding a new recording, the id is an empty string as a default.
        if (recording.id.isNotEmpty()) {
            intent.action = "updateTimerecEntry"
            intent.putExtra("id", recording.id)
        } else {
            intent.action = "addTimerecEntry"
        }
        startService(intent)
        finish()
    }

    private fun cancel() {
        Timber.d("cancel")
        // Show confirmation dialog to cancel
        MaterialAlertDialogBuilder(this)
            .setMessage(R.string.cancel_add_recording)
            .setPositiveButton(R.string.discard) { _, _ ->
                Timber.d("discarding popping back stack")
                finish()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                Timber.d("dismissing dialog")
                dialog.dismiss()
            }
            .show()
    }

    private fun handleStartStopTimeUpdate(recording: TimerRecordingWithChannel) {
        // If the start time is after the stop time, update the stop time with the start value
        if (recording.start > recording.stop) {
            recording.stop = recording.start
        // If the stop time is before the start time, update the start time with the stop value
        } else if (recording.stop < recording.start) {
            recording.start = recording.stop
        }

        binding.startTime.setText(getTimeStringFromTimeInMillis(minutesToTimeMillis(recording.start)))
        binding.stopTime.setText(getTimeStringFromTimeInMillis(minutesToTimeMillis(recording.stop)))
    }

    override fun onBackPressed() {
        Timber.d("onBackPressed")
        cancel()
    }

    companion object {
        fun makeIntent(context: Context, id: String? = null) =
            Intent(context, TimerRecordingEditActivity::class.java)
                .putExtra("id", id)
    }
}
