package org.tvheadend.tvhclient.ui.features.dvr.recordings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.tvheadend.data.ServerCapabilities
import org.tvheadend.data.entity.RecordingWithChannel
import org.tvheadend.data.entity.ServerProfile
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.RecordingEditActivityBinding
import org.tvheadend.tvhclient.ui.base.BaseActivity
import org.tvheadend.tvhclient.ui.features.dvr.getDateStringFromTimeInMillis
import org.tvheadend.tvhclient.ui.features.dvr.getSelectedProfileId
import org.tvheadend.tvhclient.ui.features.dvr.getTimeStringFromTimeInMillis
import org.tvheadend.tvhclient.ui.features.dvr.handleChannelListSelection
import org.tvheadend.tvhclient.ui.features.dvr.handlePrioritySelection
import org.tvheadend.tvhclient.ui.features.dvr.handleRecordingProfileSelection
import org.tvheadend.tvhclient.ui.features.dvr.replaceHourAndMinute
import org.tvheadend.tvhclient.ui.features.dvr.showDatePicker
import org.tvheadend.tvhclient.ui.features.dvr.showTimePicker
import org.tvheadend.tvhclient.util.applyNavigationBarPadding
import org.tvheadend.tvhclient.util.extensions.afterTextChanged
import org.tvheadend.tvhclient.util.extensions.applyText
import org.tvheadend.tvhclient.util.extensions.determinePriorityText
import org.tvheadend.tvhclient.util.extensions.observeOnce
import org.tvheadend.tvhclient.util.extensions.sendSnackbarMessage
import org.tvheadend.tvhclient.util.livedata.CombinedPairLiveData

class RecordingEditActivity : BaseActivity() {

    private lateinit var binding: RecordingEditActivityBinding
    private lateinit var recordingViewModel: RecordingViewModel
    private lateinit var recordingProfilesList: Array<String>
    private var profile: ServerProfile? = null
    private lateinit var caps: ServerCapabilities

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = RecordingEditActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar(binding.toolbar, binding.appBar)
        binding.content.applyNavigationBarPadding()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        recordingViewModel = ViewModelProvider(this)[RecordingViewModel::class.java]

        recordingProfilesList = recordingViewModel.getRecordingProfileNames()
        profile = recordingViewModel.getRecordingProfile()
        recordingViewModel.recordingProfileNameId = getSelectedProfileId(profile, recordingProfilesList)

        val id = intent.getIntExtra("id", 0)
        if (savedInstanceState == null) {
            recordingViewModel.currentIdLiveData.value = id
        }

        setTitle(if (id == 0) R.string.add_recording else R.string.edit_recording)

        val inputLiveData = CombinedPairLiveData(
            recordingViewModel.recordingLiveData,
            globalStatusViewModel.htspVersionLiveData
        ) { rec, htspVersion -> rec to ServerCapabilities(htspVersion) }

        inputLiveData.observeOnce(this) { (rec, caps) ->
            this.caps = caps
            updateUI(rec)
        }
    }

    private fun updateUI(recording: RecordingWithChannel) {
        binding.titleWrapper.isVisible = caps.recordingTitleSupported
        binding.title.setText(recording.title)

        binding.subtitleWrapper.isVisible = caps.recordingSubtitleSupported
        binding.subtitle.setText(recording.subtitle)

        binding.summaryWrapper.isVisible = caps.recordingSummarySupported
        binding.summary.setText(recording.summary)

        binding.descriptionWrapper.isVisible = caps.recordingDescriptionSupported
        binding.description.setText(recording.description)

        binding.stopTime.apply {
            setText(getTimeStringFromTimeInMillis(recording.stop))
            setOnClickListener {
                val picker = showTimePicker(recording.stop)
                picker.addOnPositiveButtonClickListener {
                    recording.stop = replaceHourAndMinute(recording.stop, picker.hour, picker.minute)
                    setText(getTimeStringFromTimeInMillis(recording.stop))
                }
                picker.show(supportFragmentManager, "stopTime")
            }
        }
        binding.stopDate.apply {
            setText(getDateStringFromTimeInMillis(recording.stop))
            setOnClickListener {
                val picker = showDatePicker(recording.stop)
                picker.addOnPositiveButtonClickListener {
                    picker.selection?.let {
                        recording.stop = it
                        setText(getDateStringFromTimeInMillis(it))
                    }
                }
                picker.show(supportFragmentManager, "stopDate")
            }
        }

        binding.stopExtra.setText(recording.stopExtra.toString())

        binding.channelWrapper.isVisible = !recording.isRecording
        if (!binding.channelWrapper.isVisible) {
            binding.channelName.applyText { recording.channelName ?: getString(R.string.all_channels) }
            binding.channelName.setOnClickListener {
                // Determine if the server supports recording on all channels
                handleChannelListSelection(
                    this,
                    recordingViewModel.getChannelList(),
                    caps.recordingOnAllChannelsSupported
                ) { channel ->
                    recording.channelId = channel.id
                    binding.channelName.setText(channel.name)
                }
            }
        }

        binding.enabledWrapper.isVisible = caps.recordingEnabledSupported && !recording.isRecording
        binding.isEnabled.isChecked = recording.isEnabled

        binding.priority.isVisible = !recording.isRecording
        binding.priority.applyText { determinePriorityText(recording.priority) }
        binding.priority.setOnClickListener {
            handlePrioritySelection(this, recording.priority) { prio ->
                recording.priority = prio
                binding.priority.applyText { determinePriorityText(prio) }
            }
        }

        binding.dvrConfigWrapper.isVisible = !recordingProfilesList.isEmpty() && !recording.isRecording
        if (binding.dvrConfigWrapper.isVisible) {
            binding.dvrConfig.apply {
                setText(recordingProfilesList[recordingViewModel.recordingProfileNameId])
                setOnClickListener {
                    handleRecordingProfileSelection(
                        this@RecordingEditActivity,
                        recordingProfilesList,
                        recordingViewModel.recordingProfileNameId,
                    ) { profileIndex ->
                        recordingViewModel.recordingProfileNameId = profileIndex
                        binding.dvrConfig.setText(recordingProfilesList[profileIndex])
                    }
                }
            }
        }

        if (recording.isRecording) {
            binding.startDateWrapper.isVisible = false
            binding.startTimeWrapper.isVisible = false
            binding.startExtraWrapper.isVisible = false
        } else {
            binding.startTime.apply {
                setText(getTimeStringFromTimeInMillis(recording.start))
                setOnClickListener {
                    val picker = showTimePicker(recording.start)
                    picker.addOnPositiveButtonClickListener {
                        recording.start = replaceHourAndMinute(recording.start, picker.hour, picker.minute)
                        setText(getTimeStringFromTimeInMillis(recording.start))
                    }
                    picker.show(supportFragmentManager, "startTime")
                }
            }
            binding.startDate.apply {
                setText(getDateStringFromTimeInMillis(recording.start))
                setOnClickListener {
                    val picker = showDatePicker(recording.start)
                    picker.addOnPositiveButtonClickListener {
                        picker.selection?.let {
                            recording.start = it
                            setText(getDateStringFromTimeInMillis(it))
                        }
                    }
                    picker.show(supportFragmentManager, "stopDate")
                }
            }
            binding.startExtra.setText(recording.startExtra.toString())
        }

        binding.title.afterTextChanged { recording.title = it }
        binding.subtitle.afterTextChanged { recording.subtitle = it }
        binding.description.afterTextChanged { recording.description = it }
        binding.startExtra.afterTextChanged { recording.startExtra = it.toLong() }
        binding.stopExtra.afterTextChanged { recording.stopExtra = it.toLong() }
        binding.isEnabled.setOnCheckedChangeListener { _, isChecked -> recording.isEnabled = isChecked }
        binding.save.setOnClickListener { save(recording) }
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
     * created recordingViewModel.recording.
     */
    private fun save(recording: RecordingWithChannel) {
        if (recording.title.isNullOrEmpty() && caps.recordingTitleSupported) {
            sendSnackbarMessage(R.string.error_empty_title)
            return
        }
        if (recording.channelId == 0 && !caps.recordingOnAllChannelsSupported) {
            sendSnackbarMessage(R.string.error_no_channel_selected)
            return
        }

        if (recording.start >= recording.stop) {
            sendSnackbarMessage(R.string.error_start_time_past_stop_time)
            return
        }

        val intent = recordingViewModel.getIntentData(this, recording.base)
        if (profile != null && caps.recordingProfileSupported && binding.dvrConfig.text.isNotEmpty()) {
            intent.putExtra("configName", binding.dvrConfig.text.toString())
        }

        if (recording.id > 0) {
            intent.action = "updateDvrEntry"
            intent.putExtra("id", recording.id)
        } else {
            intent.action = "addDvrEntry"
        }
        startService(intent)
        finish()
    }

    /**
     * Asks the user to confirm canceling the current activity. If no is
     * chosen the user can continue to add or edit the recordingViewModel.recording. Otherwise
     * the input will be discarded and the activity will be closed.
     */
    private fun cancel() {
        // Show confirmation dialog to cancel
        MaterialAlertDialogBuilder(this)
            .setMessage(R.string.cancel_edit_recording)
            .setPositiveButton(R.string.discard) { _, _ -> finish() }
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    override fun onBackPressed() {
        cancel()
    }

    companion object {
        fun makeIntent(context: Context, id: Int? = null) =
            Intent(context, RecordingEditActivity::class.java)
                .putExtra("id", id)
    }
}
