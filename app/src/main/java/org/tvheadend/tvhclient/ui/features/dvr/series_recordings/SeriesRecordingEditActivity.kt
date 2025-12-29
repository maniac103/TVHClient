package org.tvheadend.tvhclient.ui.features.dvr.series_recordings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.tvheadend.data.ServerCapabilities
import org.tvheadend.data.entity.Channel
import org.tvheadend.data.entity.ServerProfile
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.SeriesRecordingEditActivityBinding
import org.tvheadend.tvhclient.ui.base.BaseActivity
import org.tvheadend.tvhclient.ui.features.dvr.RecordingConfigSelectedListener
import org.tvheadend.tvhclient.ui.features.dvr.getSelectedProfileId
import org.tvheadend.tvhclient.ui.features.dvr.getTimeStringFromTimeInMillis
import org.tvheadend.tvhclient.ui.features.dvr.handleChannelListSelection
import org.tvheadend.tvhclient.ui.features.dvr.handlePrioritySelection
import org.tvheadend.tvhclient.ui.features.dvr.handleRecordingProfileSelection
import org.tvheadend.tvhclient.ui.features.dvr.replaceHourAndMinute
import org.tvheadend.tvhclient.ui.features.dvr.showTimePicker
import org.tvheadend.tvhclient.util.applyNavigationBarPadding
import org.tvheadend.tvhclient.util.extensions.afterTextChanged
import org.tvheadend.tvhclient.util.extensions.applyText
import org.tvheadend.tvhclient.util.extensions.determinePriorityText
import org.tvheadend.tvhclient.util.extensions.sendSnackbarMessage
import timber.log.Timber

class SeriesRecordingEditActivity : BaseActivity(), RecordingConfigSelectedListener {

    private lateinit var binding: SeriesRecordingEditActivityBinding
    private lateinit var seriesRecordingViewModel: SeriesRecordingViewModel
    private lateinit var recordingProfilesList: Array<String>
    private var profile: ServerProfile? = null
    private lateinit var caps: ServerCapabilities

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = SeriesRecordingEditActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar(binding.toolbar, binding.appBar)
        binding.content.applyNavigationBarPadding()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        seriesRecordingViewModel = ViewModelProvider(this)[SeriesRecordingViewModel::class.java]

        recordingProfilesList = seriesRecordingViewModel.getRecordingProfileNames()
        profile = seriesRecordingViewModel.getRecordingProfile()
        seriesRecordingViewModel.recordingProfileNameId = getSelectedProfileId(profile, recordingProfilesList)

        if (savedInstanceState == null) {
            seriesRecordingViewModel.loadRecordingByIdSync(intent.getStringExtra("id") ?: "")
        }

        setTitle(
            getString(if (seriesRecordingViewModel.recording.id.isEmpty()) R.string.add_recording else R.string.edit_recording)
        )

        val recording = seriesRecordingViewModel.recording
        caps = ServerCapabilities(globalStatusViewModel.htspVersionLiveData.value ?: 0)

        binding.enabledWrapper.isVisible = caps.recordingEnabledSupported
        binding.isEnabled.isChecked = seriesRecordingViewModel.recording.isEnabled

        binding.title.setText(seriesRecordingViewModel.recording.title)
        binding.name.setText(seriesRecordingViewModel.recording.name)

        binding.directoryWrapper.isVisible = caps.recordingDirectorySupported
        binding.directory.setText(seriesRecordingViewModel.recording.directory)

        binding.channelName.applyText { seriesRecordingViewModel.recording.channelName ?: getString(R.string.all_channels) }
        binding.channelName.setOnClickListener {
            handleChannelListSelection(
                this,
                seriesRecordingViewModel.getChannelList(),
                caps.recordingOnAllChannelsSupported,
                this
            )
        }

        binding.priority.applyText { determinePriorityText(seriesRecordingViewModel.recording.priority) }
        binding.priority.setOnClickListener {
            handlePrioritySelection(this, seriesRecordingViewModel.recording.priority, this)
        }

        binding.dvrConfigWrapper.isVisible = recordingProfilesList.isNotEmpty()
        if (binding.dvrConfigWrapper.isVisible) {
            binding.dvrConfig.apply {
                setText(recordingProfilesList[seriesRecordingViewModel.recordingProfileNameId])
                setOnClickListener {
                    handleRecordingProfileSelection(
                        this@SeriesRecordingEditActivity,
                        recordingProfilesList,
                        seriesRecordingViewModel.recordingProfileNameId,
                        this@SeriesRecordingEditActivity
                    )
                }
            }
        }

        binding.startTime.apply {
            setText(getTimeStringFromTimeInMillis(seriesRecordingViewModel.startTimeInMillis))
            setOnClickListener {
                val picker = showTimePicker(seriesRecordingViewModel.startTimeInMillis)
                picker.addOnPositiveButtonClickListener {
                    val millis = replaceHourAndMinute(seriesRecordingViewModel.startTimeInMillis, picker.hour, picker.minute)
                    seriesRecordingViewModel.startTimeInMillis = millis
                    handleStartStopTimeUpdate()
                }
                picker.show(supportFragmentManager, "startTime")
            }
        }

        binding.startWindowTime.apply {
            setText(getTimeStringFromTimeInMillis(seriesRecordingViewModel.startWindowTimeInMillis))
            setOnClickListener {
                val picker = showTimePicker(seriesRecordingViewModel.startWindowTimeInMillis)
                picker.addOnPositiveButtonClickListener {
                    val millis = replaceHourAndMinute(seriesRecordingViewModel.startWindowTimeInMillis, picker.hour, picker.minute)
                    seriesRecordingViewModel.startWindowTimeInMillis = millis
                    handleStartStopTimeUpdate()
                }
                picker.show(supportFragmentManager, "startWindowTime")
            }
        }

        binding.startExtra.setText(seriesRecordingViewModel.recording.startExtra.toString())
        binding.stopExtra.setText(seriesRecordingViewModel.recording.stopExtra.toString())

        binding.daysOfWeek.selectedDaysBitmask = seriesRecordingViewModel.recording.daysOfWeek
        binding.daysOfWeek.setOnSelectedDaysChangedListener { days ->
            seriesRecordingViewModel.recording.daysOfWeek = days
        }

        binding.minimumDuration.setText((seriesRecordingViewModel.recording.minDuration / 60).toString())
        binding.maximumDuration.setText((seriesRecordingViewModel.recording.maxDuration / 60).toString())

        binding.timeEnabled.isChecked = seriesRecordingViewModel.isTimeEnabled
        handleTimeEnabledClick(binding.timeEnabled.isChecked)
        binding.timeEnabled.setOnCheckedChangeListener { _, checked ->
            handleTimeEnabledClick(checked)
        }

        binding.duplicateDetectionWrapper.isVisible = caps.duplicateDetectionSupported
        binding.duplicateDetection.setText(seriesRecordingViewModel.duplicateDetectionList[seriesRecordingViewModel.recording.dupDetect])
        binding.duplicateDetection.setOnClickListener {
            handleDuplicateDetectionSelection(seriesRecordingViewModel.duplicateDetectionList, seriesRecordingViewModel.recording.dupDetect)
        }

        binding.title.afterTextChanged { seriesRecordingViewModel.recording.title = it }
        binding.name.afterTextChanged { seriesRecordingViewModel.recording.name = it }
        binding.directory.afterTextChanged { seriesRecordingViewModel.recording.directory = it }
        binding.minimumDuration.afterTextChanged { seriesRecordingViewModel.recording.minDuration = it.toInt() }
        binding.maximumDuration.afterTextChanged { seriesRecordingViewModel.recording.maxDuration = it.toInt() }
        binding.startExtra.afterTextChanged { seriesRecordingViewModel.recording.startExtra = it.toLong() }
        binding.stopExtra.afterTextChanged { seriesRecordingViewModel.recording.stopExtra = it.toLong() }
        binding.isEnabled.setOnCheckedChangeListener { _, isChecked ->
            seriesRecordingViewModel.recording.isEnabled = isChecked
        }
        binding.save.setOnClickListener { save() }
    }

    private fun handleTimeEnabledClick(checked: Boolean) {
        Timber.d("Setting time enabled ${binding.timeEnabled.isChecked}")
        seriesRecordingViewModel.isTimeEnabled = checked
        binding.startTimeWrapper.isVisible = checked
        binding.startWindowTimeWrapper.isVisible = checked
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

    /**
     * Checks certain given values for plausibility and if everything is fine
     * creates the intent that will be passed to the service to save the newly
     * created recording.
     */
    private fun save() {
        if (seriesRecordingViewModel.recording.title.isNullOrEmpty()) {
            sendSnackbarMessage(R.string.error_empty_title)
            return
        }

        // The maximum durationTextView must be at least the minimum durationTextView
        if (seriesRecordingViewModel.recording.minDuration > 0
                && seriesRecordingViewModel.recording.maxDuration > 0
                && seriesRecordingViewModel.recording.maxDuration < seriesRecordingViewModel.recording.minDuration) {
            seriesRecordingViewModel.recording.maxDuration = seriesRecordingViewModel.recording.minDuration
        }

        val intent = seriesRecordingViewModel.getIntentData(this, seriesRecordingViewModel.recording.base)

        // Add the recording profile if available and enabled
        if (profile != null && caps.recordingProfileSupported && binding.dvrConfig.text.isNotEmpty()) {
            intent.putExtra("configName", binding.dvrConfig.text.toString())
        }

        // Update the recording in case the id is not empty, otherwise add a new one.
        // When adding a new recording, the id is an empty string as a default.
        if (seriesRecordingViewModel.recording.id.isNotEmpty()) {
            intent.action = "updateAutorecEntry"
            intent.putExtra("id", seriesRecordingViewModel.recording.id)
        } else {
            intent.action = "addAutorecEntry"
        }
        startService(intent)
        finish()
    }

    /**
     * Asks the user to confirm canceling the current activity. If no is
     * chosen the user can continue to add or edit the recording. Otherwise
     * the input will be discarded and the activity will be closed.
     */
    private fun cancel() {
        MaterialAlertDialogBuilder(this)
            .setMessage(R.string.cancel_add_recording)
            .setPositiveButton(R.string.discard) { _, _ -> finish() }
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    override fun onChannelSelected(channel: Channel) {
        seriesRecordingViewModel.recording.channelId = channel.id
        binding.channelName.setText(channel.name)
    }

    override fun onPrioritySelected(which: Int) {
        seriesRecordingViewModel.recording.priority = which
        binding.priority.applyText { determinePriorityText(seriesRecordingViewModel.recording.priority) }
    }

    override fun onProfileSelected(which: Int) {
        binding.dvrConfig.setText(recordingProfilesList[which])
        seriesRecordingViewModel.recordingProfileNameId = which
    }

    private fun handleStartStopTimeUpdate() {
        // If the start time is after the start window time, update the start window time with the start value
        if (seriesRecordingViewModel.startTimeInMillis > seriesRecordingViewModel.startWindowTimeInMillis) {
            seriesRecordingViewModel.startWindowTimeInMillis = seriesRecordingViewModel.startTimeInMillis
        // If the start window time is before the start time, update the start time with the start window value
        } else if (seriesRecordingViewModel.startWindowTimeInMillis < seriesRecordingViewModel.startTimeInMillis) {
            seriesRecordingViewModel.startTimeInMillis = seriesRecordingViewModel.startWindowTimeInMillis
        }

        binding.startTime.setText(getTimeStringFromTimeInMillis(seriesRecordingViewModel.startTimeInMillis))
        binding.startWindowTime.setText(getTimeStringFromTimeInMillis(seriesRecordingViewModel.startWindowTimeInMillis))
    }

    override fun onDaysSelected(selectedDays: Int) {
    }

    private fun onDuplicateDetectionValueSelected(which: Int) {
        seriesRecordingViewModel.recording.dupDetect = which
        binding.duplicateDetection.setText(seriesRecordingViewModel.duplicateDetectionList[which])
    }

    private fun handleDuplicateDetectionSelection(duplicateDetectionList: Array<String>, duplicateDetectionId: Int) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.select_duplicate_detection)
            .setSingleChoiceItems(duplicateDetectionList, duplicateDetectionId) { _, index ->
                onDuplicateDetectionValueSelected(index)
            }
            .show()
    }

    override fun onBackPressed() {
        cancel()
    }

    companion object {
        fun makeIntent(context: Context, id: String? = null) =
            Intent(context, SeriesRecordingEditActivity::class.java)
                .putExtra("id", id)
    }
}
