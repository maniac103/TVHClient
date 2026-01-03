package org.tvheadend.tvhclient.ui.features.dvr.series_recordings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.tvheadend.data.ServerCapabilities
import org.tvheadend.data.entity.SeriesRecordingWithChannel
import org.tvheadend.data.entity.ServerProfile
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.SeriesRecordingEditActivityBinding
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

class SeriesRecordingEditActivity : BaseActivity() {

    private lateinit var binding: SeriesRecordingEditActivityBinding
    private lateinit var seriesRecordingViewModel: SeriesRecordingViewModel
    private lateinit var recordingProfilesList: Array<String>
    private var profile: ServerProfile? = null

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

        val id = intent.getStringExtra("id") ?: ""
        if (savedInstanceState == null) {
            seriesRecordingViewModel.currentIdLiveData.value = id
        }

        setTitle(getString(if (id.isEmpty()) R.string.add_recording else R.string.edit_recording))

        val inputLiveData = CombinedPairLiveData(
            seriesRecordingViewModel.recordingLiveData,
            globalStatusViewModel.connectedServerLiveData
        ) { rec, serverData -> rec to serverData?.capabilities }

        inputLiveData.observeOnce(this) { (rec, caps) ->
            if (rec == null || caps == null) {
                finish()
            } else {
                updateUI(rec, caps)
            }
        }
    }

    private fun updateUI(recording: SeriesRecordingWithChannel, caps: ServerCapabilities) {
        binding.enabledWrapper.isVisible = caps.recordingEnabledSupported
        binding.isEnabled.isChecked = recording.isEnabled

        binding.title.setText(recording.title)
        binding.name.setText(recording.name)

        binding.directoryWrapper.isVisible = caps.recordingDirectorySupported
        binding.directory.setText(recording.directory)

        binding.channelName.applyText { recording.channelName ?: getString(R.string.all_channels) }
        binding.channelName.setOnClickListener {
            handleChannelListSelection(
                this,
                seriesRecordingViewModel.getChannelList(),
                caps.recordingOnAllChannelsSupported,
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
        if (binding.dvrConfigWrapper.isVisible) {
            binding.dvrConfig.apply {
                setText(recordingProfilesList[seriesRecordingViewModel.recordingProfileNameId])
                setOnClickListener {
                    handleRecordingProfileSelection(
                        this@SeriesRecordingEditActivity,
                        recordingProfilesList,
                        seriesRecordingViewModel.recordingProfileNameId
                    ) { profileIndex ->
                        binding.dvrConfig.setText(recordingProfilesList[profileIndex])
                        seriesRecordingViewModel.recordingProfileNameId = profileIndex
                    }
                }
            }
        }

        binding.startTime.apply {
            setText(getTimeStringFromTimeInMillis(minutesToTimeMillis(recording.start)))
            setOnClickListener {
                val picker = showTimePicker(minutesToTimeMillis(recording.start))
                picker.addOnPositiveButtonClickListener {
                    recording.start = 60L * picker.hour + picker.minute
                    handleStartStopTimeUpdate(recording)
                }
                picker.show(supportFragmentManager, "startTime")
            }
        }

        binding.startWindowTime.apply {
            setText(getTimeStringFromTimeInMillis(minutesToTimeMillis(recording.startWindow)))
            setOnClickListener {
                val picker = showTimePicker(minutesToTimeMillis(recording.startWindow))
                picker.addOnPositiveButtonClickListener {
                    recording.startWindow = 60L * picker.hour + picker.minute
                    handleStartStopTimeUpdate(recording)
                }
                picker.show(supportFragmentManager, "startWindowTime")
            }
        }

        binding.startExtra.setText(recording.startExtra.toString())
        binding.stopExtra.setText(recording.stopExtra.toString())

        binding.daysOfWeek.selectedDaysBitmask = recording.daysOfWeek
        binding.daysOfWeek.setOnSelectedDaysChangedListener { recording.daysOfWeek = it }

        binding.minimumDuration.setText((recording.minDuration / 60).toString())
        binding.maximumDuration.setText((recording.maxDuration / 60).toString())

        binding.timeEnabled.isChecked = seriesRecordingViewModel.isTimeEnabled
        handleTimeEnabledClick(binding.timeEnabled.isChecked)
        binding.timeEnabled.setOnCheckedChangeListener { _, checked ->
            handleTimeEnabledClick(checked)
        }

        binding.duplicateDetectionWrapper.isVisible = caps.duplicateDetectionSupported
        binding.duplicateDetection.setText(seriesRecordingViewModel.duplicateDetectionList[recording.dupDetect])
        binding.duplicateDetection.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.select_duplicate_detection)
                .setSingleChoiceItems(seriesRecordingViewModel.duplicateDetectionList, recording.dupDetect) { _, index ->
                    recording.dupDetect = index
                    binding.duplicateDetection.setText(seriesRecordingViewModel.duplicateDetectionList[index])
                }
                .show()
        }

        binding.title.afterTextChanged { recording.title = it }
        binding.name.afterTextChanged { recording.name = it }
        binding.directory.afterTextChanged { recording.directory = it }
        binding.minimumDuration.afterTextChanged { recording.minDuration = it.toInt() }
        binding.maximumDuration.afterTextChanged { recording.maxDuration = it.toInt() }
        binding.startExtra.afterTextChanged { recording.startExtra = it.toLong() }
        binding.stopExtra.afterTextChanged { recording.stopExtra = it.toLong() }
        binding.isEnabled.setOnCheckedChangeListener { _, isChecked -> recording.isEnabled = isChecked }
        binding.save.setOnClickListener { save(recording, caps) }
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
    private fun save(recording: SeriesRecordingWithChannel, caps: ServerCapabilities) {
        if (recording.title.isNullOrEmpty()) {
            sendSnackbarMessage(R.string.error_empty_title)
            return
        }

        // The maximum durationTextView must be at least the minimum durationTextView
        if (recording.minDuration > 0 && recording.maxDuration > 0 && recording.maxDuration < recording.minDuration) {
            recording.maxDuration = recording.minDuration
        }

        val intent = seriesRecordingViewModel.getIntentData(this, recording.base)

        // Add the recording profile if available and enabled
        if (profile != null && caps.recordingProfileSupported && binding.dvrConfig.text.isNotEmpty()) {
            intent.putExtra("configName", binding.dvrConfig.text.toString())
        }

        // Update the recording in case the id is not empty, otherwise add a new one.
        // When adding a new recording, the id is an empty string as a default.
        if (recording.id.isNotEmpty()) {
            intent.action = "updateAutorecEntry"
            intent.putExtra("id", recording.id)
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

    private fun handleStartStopTimeUpdate(recording: SeriesRecordingWithChannel) {
        // If the start time is after the start window time, update the start window time with the start value
        if (recording.start > recording.startWindow) {
            recording.startWindow = recording.start
        // If the start window time is before the start time, update the start time with the start window value
        } else if (recording.startWindow < recording.start) {
            recording.start = recording.startWindow
        }

        binding.startTime.setText(getTimeStringFromTimeInMillis(minutesToTimeMillis(recording.start)))
        binding.startWindowTime.setText(getTimeStringFromTimeInMillis(minutesToTimeMillis(recording.startWindow)))
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
