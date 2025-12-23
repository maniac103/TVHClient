package org.tvheadend.tvhclient.ui.features.programs

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import org.tvheadend.data.entity.ProgramBaseInterface
import org.tvheadend.data.entity.ProgramWithChannel
import org.tvheadend.data.entity.Recording
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.ProgramDetailsActivityBinding
import org.tvheadend.tvhclient.ui.base.BaseActivity
import org.tvheadend.tvhclient.ui.common.addNotificationProgramIsAboutToStart
import org.tvheadend.tvhclient.ui.common.castSelectedChannel
import org.tvheadend.tvhclient.ui.common.editSelectedRecording
import org.tvheadend.tvhclient.ui.common.onAttach
import org.tvheadend.tvhclient.ui.common.playSelectedChannel
import org.tvheadend.tvhclient.ui.common.preparePopupOrToolbarMiscMenu
import org.tvheadend.tvhclient.ui.common.preparePopupOrToolbarRecordingMenu
import org.tvheadend.tvhclient.ui.common.preparePopupOrToolbarSearchMenu
import org.tvheadend.tvhclient.ui.common.recordSelectedProgram
import org.tvheadend.tvhclient.ui.common.recordSelectedProgramAsSeriesRecording
import org.tvheadend.tvhclient.ui.common.recordSelectedProgramWithCustomProfile
import org.tvheadend.tvhclient.ui.common.searchTitleInTheLocalDatabase
import org.tvheadend.tvhclient.ui.common.searchTitleOnFileAffinityWebsite
import org.tvheadend.tvhclient.ui.common.searchTitleOnGoogle
import org.tvheadend.tvhclient.ui.common.searchTitleOnImdbWebsite
import org.tvheadend.tvhclient.ui.common.searchTitleOnYoutube
import org.tvheadend.tvhclient.ui.common.showConfirmationToCancelSelectedRecording
import org.tvheadend.tvhclient.ui.common.showConfirmationToRemoveSelectedRecording
import org.tvheadend.tvhclient.ui.common.showConfirmationToStopSelectedRecording
import org.tvheadend.tvhclient.util.applyNavigationBarPadding
import org.tvheadend.tvhclient.util.extensions.getCastSession
import timber.log.Timber

class ProgramDetailsActivity : BaseActivity() {
    private lateinit var binding: ProgramDetailsActivityBinding
    private lateinit var programViewModel: ProgramViewModel

    private var program: ProgramWithChannel? = null
    private var recording: Recording? = null

    private var htspVersion: Int = 13
    private var isConnectionToServerAvailable: Boolean = false
    private var programIdToBeEditedWhenBeingRecorded = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        programViewModel = ViewModelProvider(this)[ProgramViewModel::class.java]

        binding = ProgramDetailsActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar(binding.toolbar, binding.appBar)
        binding.content.applyNavigationBarPadding()

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu_cancel)
        }

        programViewModel = ViewModelProvider(this)[ProgramViewModel::class.java]
        programViewModel.eventIdLiveData.value = intent.getIntExtra("eventId", 0)
        programViewModel.channelIdLiveData.value = intent.getIntExtra("channelId", 0)

        programViewModel.program.observe(this) {
            Timber.d("View model returned a program")
            setTitle(it?.title)
            program = it
            binding.program = program
            binding.viewModel = programViewModel
            updateActionButtons()
            invalidateOptionsMenu()
        }

        programViewModel.recordings.observe(this) { recordings ->
            Timber.d("View model returned ${recordings.size} recordings")

            recordings
                .firstOrNull { it.eventId == programIdToBeEditedWhenBeingRecorded && programIdToBeEditedWhenBeingRecorded > 0 }
                ?.let {
                    programIdToBeEditedWhenBeingRecorded = 0
                    editSelectedRecording(this, it.id)
                }
            recording = recordings.firstOrNull { it.eventId == program?.eventId }?.base

            binding.recording = recording
            updateActionButtons()
            invalidateOptionsMenu()
        }

        globalStatusViewModel.connectionToServerAvailableLiveData.observe(this) { isAvailable ->
            Timber.d("Received live data, connection to server availability changed to $isAvailable")
            isConnectionToServerAvailable = isAvailable
            updateActionButtons()
            invalidateOptionsMenu()
        }

        globalStatusViewModel.htspVersionLiveData.observe(this) {
            htspVersion = it ?: 0
            invalidateOptionsMenu()
        }

        binding.play.setOnClickListener {
            program?.let { playSelectedChannel(this, it.channelId) }
        }
        binding.cast.setOnClickListener {
            program?.let { castSelectedChannel(this, it.channelId) }
        }
        binding.record.setOnClickListener {
            program?.let { recordSelectedProgram(this, it.eventId, programViewModel.getRecordingProfile(), htspVersion) }
        }
        binding.recordingAction.setOnClickListener {
            when {
                recording?.isRecording == true -> showConfirmationToStopSelectedRecording(this, recording, null)
                recording?.isScheduled == true -> showConfirmationToCancelSelectedRecording(this, recording, null)
                recording?.isCompleted == true -> showConfirmationToRemoveSelectedRecording(this, recording, null)
            }
        }

        binding.recordOverflow.isCheckable = false
        binding.recordOverflow.setOnClickListener {
            val menu = PopupMenu(this, binding.recordOverflow)
            menu.inflate(R.menu.recording_options)
            preparePopupOrToolbarRecordingMenu(this, menu.menu, recording, isConnectionToServerAvailable, htspVersion)

            menu.setOnMenuItemClickListener { item ->
                val program = program ?: return@setOnMenuItemClickListener false
                val profile = programViewModel.getRecordingProfile()
                when (item.itemId) {
                    R.id.menu_record_program_and_edit -> {
                        programIdToBeEditedWhenBeingRecorded = program.eventId
                        recordSelectedProgram(this, program.eventId, profile, htspVersion)
                    }

                    R.id.menu_record_program_with_custom_profile ->
                        recordSelectedProgramWithCustomProfile(this, program.eventId, program.channelId, programViewModel.getRecordingProfileNames(), profile)

                    R.id.menu_record_program_as_series_recording ->
                        recordSelectedProgramAsSeriesRecording(this, program.title, program.channelId, profile, htspVersion)

                    else -> false
                }
            }
            menu.show()
        }
    }

    override fun attachBaseContext(context: Context) {
        super.attachBaseContext(onAttach(context))
    }

    private fun updateActionButtons() {
        val program = program ?: return
        val recording = recording
        val currentTime = System.currentTimeMillis()

        binding.play.isVisible = when {
            !isConnectionToServerAvailable -> false
            recording != null && recording.dataSize > 0 -> true
            currentTime > program.start && currentTime < program.stop -> true
            else -> false
        }
        binding.cast.isVisible = binding.play.isVisible && getCastSession() != null

        binding.record.isVisible = when {
            !isConnectionToServerAvailable -> false
            recording == null -> true
            recording.isRecording -> false
            recording.isScheduled -> false
            recording.isCompleted -> false
            recording.isFailed -> false
            recording.isFileMissing -> false
            recording.isMissed -> false
            recording.isAborted -> false
            else -> true
        }
        binding.recordOverflow.isVisible = binding.record.isVisible

        val actionIconResId = when {
            recording?.isRecording == true -> R.drawable.ic_menu_stop
            recording?.isCompleted == true -> R.drawable.ic_menu_delete
            recording?.isScheduled == true -> R.drawable.ic_menu_cancel
            else -> 0
        }
        binding.recordingAction.isVisible = actionIconResId != 0
        if (actionIconResId != 0) {
            binding.recordingAction.setIconResource(actionIconResId)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.external_search_options_menu, menu)
        menuInflater.inflate(R.menu.program_details, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        preparePopupOrToolbarRecordingMenu(this, menu, recording, isConnectionToServerAvailable, htspVersion)
        preparePopupOrToolbarMiscMenu(this, menu, program, isConnectionToServerAvailable)
        preparePopupOrToolbarSearchMenu(menu, program?.title, isConnectionToServerAvailable)

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }

        val program = program ?: return super.onOptionsItemSelected(item)

        return when (item.itemId) {
            R.id.menu_search_imdb -> searchTitleOnImdbWebsite(this, program.title)
            R.id.menu_search_fileaffinity -> searchTitleOnFileAffinityWebsite(this, program.title)
            R.id.menu_search_youtube -> searchTitleOnYoutube(this, program.title)
            R.id.menu_search_google -> searchTitleOnGoogle(this, program.title)
            R.id.menu_search_epg -> searchTitleInTheLocalDatabase(this, baseViewModel, program.title)
            R.id.menu_add_notification -> addNotificationProgramIsAboutToStart(this, program, programViewModel.getRecordingProfile())

            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        fun makeIntent(context: Context, program: ProgramBaseInterface) =
            makeIntent(context, program.eventId, program.channelId)

        fun makeIntent(context: Context, eventId: Int, channelId: Int) =
            Intent(context, ProgramDetailsActivity::class.java)
                .putExtra("channelId", channelId)
                .putExtra("eventId", eventId)
    }
}
