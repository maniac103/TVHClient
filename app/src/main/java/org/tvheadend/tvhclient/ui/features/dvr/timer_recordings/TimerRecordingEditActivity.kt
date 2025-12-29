package org.tvheadend.tvhclient.ui.features.dvr.timer_recordings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import org.tvheadend.data.ServerCapabilities
import org.tvheadend.data.entity.Channel
import org.tvheadend.data.entity.ServerProfile
import org.tvheadend.data.entity.TimerRecordingWithChannel
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.TimerRecordingEditActivityBinding
import org.tvheadend.tvhclient.ui.base.BaseActivity
import org.tvheadend.tvhclient.ui.features.dvr.RecordingConfigSelectedListener
import org.tvheadend.tvhclient.ui.features.dvr.getSelectedProfileId
import org.tvheadend.tvhclient.ui.features.dvr.getTimeStringFromTimeInMillis
import org.tvheadend.tvhclient.ui.features.dvr.handleChannelListSelection
import org.tvheadend.tvhclient.ui.features.dvr.handlePrioritySelection
import org.tvheadend.tvhclient.ui.features.dvr.handleRecordingProfileSelection
import org.tvheadend.tvhclient.util.applyNavigationBarPadding
import org.tvheadend.tvhclient.util.extensions.afterTextChanged
import org.tvheadend.tvhclient.util.extensions.applyText
import org.tvheadend.tvhclient.util.extensions.determinePriorityText
import org.tvheadend.tvhclient.util.extensions.sendSnackbarMessage
import timber.log.Timber
import java.util.Calendar

class TimerRecordingEditActivity : BaseActivity(), RecordingConfigSelectedListener {

    private lateinit var binding: TimerRecordingEditActivityBinding
    private lateinit var timerRecordingViewModel: TimerRecordingViewModel
    private lateinit var recordingProfilesList: Array<String>

    private lateinit var recording: TimerRecordingWithChannel
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

        if (savedInstanceState == null) {
            timerRecordingViewModel.loadRecordingByIdSync(intent.getStringExtra("id") ?: "")
        }

        setTitle(
            getString(if (timerRecordingViewModel.recording.id.isEmpty()) R.string.add_recording else R.string.edit_recording)
        )

        recording = timerRecordingViewModel.recording
        caps = ServerCapabilities(globalStatusViewModel.htspVersionLiveData.value ?: 0)

        binding.save.setOnClickListener { save() }
        binding.isEnabled.apply {
            isVisible = caps.recordingEnabledSupported
            isChecked = recording.isEnabled
        }

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
                caps.recordingOnAllChannelsSupported,
                this@TimerRecordingEditActivity
            )
        }

        binding.priority.applyText { determinePriorityText(recording.priority) }
        binding.priority.setOnClickListener {
            handlePrioritySelection(this, recording.priority, this@TimerRecordingEditActivity)
        }

        binding.dvrConfigWrapper.isVisible = recordingProfilesList.isNotEmpty()
        if (recordingProfilesList.isNotEmpty()) {
            binding.dvrConfig.setText(recordingProfilesList[timerRecordingViewModel.recordingProfileNameId], TextView.BufferType.NORMAL)
            binding.dvrConfig.setOnClickListener {
                handleRecordingProfileSelection(
                    this,
                    recordingProfilesList,
                    timerRecordingViewModel.recordingProfileNameId,
                    this@TimerRecordingEditActivity
                )
            }
        }

        binding.startTime.setText(getTimeStringFromTimeInMillis(timerRecordingViewModel.startTimeInMillis), TextView.BufferType.NORMAL)
        binding.startTime.setOnClickListener {
            val picker = showTimePicker(timerRecordingViewModel.startTimeInMillis)
            picker.addOnPositiveButtonClickListener {
                val millis = replaceHourAndMinute(timerRecordingViewModel.startTimeInMillis, picker.hour, picker.minute)
                timerRecordingViewModel.startTimeInMillis = millis
                handleStartStopTimeUpdate()
            }
            picker.show(supportFragmentManager, "startTime")
        }

        binding.stopTime.setText(getTimeStringFromTimeInMillis(timerRecordingViewModel.stopTimeInMillis), TextView.BufferType.NORMAL)
        binding.stopTime.setOnClickListener {
            val picker = showTimePicker(timerRecordingViewModel.startTimeInMillis)
            picker.addOnPositiveButtonClickListener {
                val millis = replaceHourAndMinute(timerRecordingViewModel.stopTimeInMillis, picker.hour, picker.minute)
                timerRecordingViewModel.stopTimeInMillis = millis
                handleStartStopTimeUpdate()
            }
            picker.show(supportFragmentManager, "stopTime")
        }

        binding.daysOfWeek.selectedDaysBitmask = recording.daysOfWeek
        binding.daysOfWeek.setOnSelectedDaysChangedListener { days ->
            timerRecordingViewModel.recording.daysOfWeek = days
        }

        binding.timeEnabled.isChecked = timerRecordingViewModel.isTimeEnabled
        handleTimeEnabledClick(binding.timeEnabled.isChecked)

        binding.timeEnabled.setOnClickListener {
            handleTimeEnabledClick(binding.timeEnabled.isChecked)
        }

        binding.title.afterTextChanged { recording.title = it }
        binding.name.afterTextChanged { recording.name = it }
        binding.directory.afterTextChanged { recording.directory = it }
        binding.isEnabled.setOnCheckedChangeListener { _, isChecked ->
            recording.isEnabled = isChecked
        }
    }

    private fun showTimePicker(millis: Long): MaterialTimePicker {
        val c = Calendar.getInstance()
        c.timeInMillis = millis
        return MaterialTimePicker.Builder()
            // TODO: clock format
            .setHour(c.get(Calendar.HOUR_OF_DAY))
            .setMinute(c.get(Calendar.MINUTE))
            .build()
    }

    private fun replaceHourAndMinute(millis: Long, hour: Int, minute: Int): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = millis
        c.set(Calendar.HOUR_OF_DAY, hour)
        c.set(Calendar.MINUTE, minute)
        return c.timeInMillis
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

    private fun save() {
        if (timerRecordingViewModel.recording.title.isNullOrEmpty()) {
            sendSnackbarMessage(R.string.error_empty_title)
            return
        }

        val intent = timerRecordingViewModel.getIntentData(this, timerRecordingViewModel.recording.base)

        // Add the recording profile if available and enabled
        if (profile != null && caps.recordingProfileSupported && binding.dvrConfig.text.isNotEmpty()) {
            intent.putExtra("configName", binding.dvrConfig.text.toString())
        }

        // Update the recording in case the id is not empty, otherwise add a new one.
        // When adding a new recording, the id is an empty string as a default.
        if (timerRecordingViewModel.recording.id.isNotEmpty()) {
            intent.action = "updateTimerecEntry"
            intent.putExtra("id", timerRecordingViewModel.recording.id)
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

    override fun onChannelSelected(channel: Channel) {
        timerRecordingViewModel.recording.channelId = channel.id
        binding.channelName.setText(channel.name, TextView.BufferType.NORMAL)
    }

    override fun onPrioritySelected(which: Int) {
        timerRecordingViewModel.recording.priority = which
        binding.priority.applyText { determinePriorityText(timerRecordingViewModel.recording.priority) }
    }

    override fun onProfileSelected(which: Int) {
        binding.dvrConfig.setText(recordingProfilesList[which], TextView.BufferType.NORMAL)
        timerRecordingViewModel.recordingProfileNameId = which
    }

    private fun handleStartStopTimeUpdate() {
        // If the start time is after the stop time, update the stop time with the start value
        if (timerRecordingViewModel.startTimeInMillis > timerRecordingViewModel.stopTimeInMillis) {
            timerRecordingViewModel.stopTimeInMillis = timerRecordingViewModel.startTimeInMillis
        // If the stop time is before the start time, update the start time with the stop value
        } else if (timerRecordingViewModel.stopTimeInMillis < timerRecordingViewModel.recording.startTimeInMillis) {
            timerRecordingViewModel.startTimeInMillis = timerRecordingViewModel.stopTimeInMillis
        }

        binding.startTime.setText(getTimeStringFromTimeInMillis(timerRecordingViewModel.startTimeInMillis), TextView.BufferType.NORMAL)
        binding.stopTime.setText(getTimeStringFromTimeInMillis(timerRecordingViewModel.stopTimeInMillis), TextView.BufferType.NORMAL)
    }

    override fun onDaysSelected(selectedDays: Int) {
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
