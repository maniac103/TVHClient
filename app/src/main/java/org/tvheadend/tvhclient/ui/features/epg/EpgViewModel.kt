package org.tvheadend.tvhclient.ui.features.epg

import android.app.Application
import android.content.ContextWrapper
import android.util.SparseArray
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.application
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tvheadend.data.entity.EpgChannel
import org.tvheadend.data.entity.EpgProgram
import org.tvheadend.data.source.ProgramDataSource
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.features.channels.BaseChannelViewModel
import org.tvheadend.tvhclient.ui.features.programs.ProgramDetailsActivity
import org.tvheadend.tvhclient.util.extensions.channelDataSource
import org.tvheadend.tvhclient.util.extensions.prefs
import org.tvheadend.tvhclient.util.extensions.programDataSource
import org.tvheadend.tvhclient.util.livedata.CombinedPairLiveData
import timber.log.Timber
import java.util.Calendar
import kotlin.time.Duration.Companion.minutes

class EpgViewModel(application: Application) : BaseChannelViewModel(application) {

    val registeredEpgFragments = SparseArray<Fragment>()
    val epgChannels = CombinedPairLiveData(selectedChannelTagIds, application.prefs.channelSortOrderLiveData()) { tagIds, sortOrder ->
        tagIds to sortOrder
    }.switchMap { (tagIds, sortOrder) ->
        application.channelDataSource.getAllEpgChannels(sortOrder.ordinal, tagIds)
    }

    private val startTimeInternal = MutableLiveData(0L)

    val startTime: LiveData<Long> get() = startTimeInternal

    private val programLiveDataPerFragment = mutableMapOf<Int, EpgForFragmentLiveData>()

    val showChannelNumber = application.prefs.showChannelNumbersLiveData()
    var showGenreColor = application.prefs.genreColorsForProgramGuideLiveData()
    var showProgramSubtitle = application.prefs.showProgramSubtitleLiveData()

    var hoursOfEpgDataPerScreen = application.prefs.epgHoursPerScreenLiveData()
    var daysOfEpgData = application.prefs.epgDaysToShowLiveData()

    /**
     * The number of screens that the view pager contains
     */
    val viewPagerFragmentCount: LiveData<Int> = CombinedPairLiveData(daysOfEpgData, hoursOfEpgDataPerScreen) { days, hours ->
        val fragmentCount = days * (24 / hours)
        Timber.d("View pager fragment count has changed to $fragmentCount")
        fragmentCount
    }

    data class EpgChannelEntry(val channel: EpgChannel, val programs: List<EpgProgram>)

    var verticalScrollOffset = 0
    var verticalScrollPosition = 0
    var selectedTimeOffset = 0


    init {
        Timber.d("Initializing")

        viewModelScope.launch {
            while (isActive) {
                startTime.value
                    ?.takeIf { Calendar.getInstance().timeInMillis - it > 60 * 60 * 1000 }
                    ?.let { updateStartTime() }
                delay(1.minutes)
            }
        }
    }

    /**
     * Calculates the start and end times that will be show in each view pager screen.
     * This is done here once to avoid recalculating it every time when scrolling horizontally.
     */
    private fun updateStartTime() {
        // Get the current time in milliseconds without the seconds but in 30 minute slots.
        // If the current time is later then 16:30 start from 16:30 otherwise from 16:00.
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.MINUTE, if (calendar.get(Calendar.MINUTE) > 30) 30 else 0)
        calendar.set(Calendar.SECOND, 0)
        if (startTimeInternal.value != calendar.timeInMillis) {
            startTimeInternal.value = calendar.timeInMillis
        }
    }

    fun getFragmentLiveData(fragmentId: Int): LiveData<List<EpgChannelEntry>> =
        programLiveDataPerFragment.getOrPut(fragmentId) {
            EpgForFragmentLiveData(
                fragmentId,
                application.programDataSource,
                viewModelScope,
                startTime,
                hoursOfEpgDataPerScreen,
                epgChannels
            )
        }

    fun getStartTime(fragmentId: Int): Long {
        val start = startTime.value ?: 0L
        val hours = hoursOfEpgDataPerScreen.value ?: 0
        return start + fragmentId * hours * 60 * 60 * 1000
    }

    fun getEndTime(fragmentId: Int): Long = getStartTime(fragmentId + 1)

    /**
     * Returns the activity from the view context so that
     * stuff like the fragment manager can be accessed
     *
     * @param view The view to retrieve the activity from
     * @return Activity or null if none was found
     */
    private fun getActivity(view: View): AppCompatActivity? {
        var context = view.context
        while (context is ContextWrapper) {
            if (context is AppCompatActivity) {
                return context
            }
            context = context.baseContext
        }
        return null
    }

    fun onClick(view: View, program: EpgProgram) {
        Timber.d("Clicked on program ${program.title}")
        val activity = getActivity(view) ?: return
        activity.startActivity(ProgramDetailsActivity.makeIntent(activity, program))
    }

    fun onLongClick(view: View, program: EpgProgram): Boolean {
        Timber.d("Long clicked on program ${program.title}")
        val activity = getActivity(view) ?: return false
        val fragment = activity.supportFragmentManager.findFragmentById(R.id.main)
        if (fragment is EpgFragment
                && fragment.isAdded
                && fragment.isResumed) {
            fragment.showPopupMenu(view, program)
        }
        return true
    }

    private class EpgForFragmentLiveData(private val fragmentId: Int,
                                         private val programDataSource: ProgramDataSource,
                                         private val scope: CoroutineScope,
                                         startTimeLiveData: LiveData<Long>,
                                         hoursOfEpgDataPerScreenLiveData: LiveData<Int>,
                                         epgChannelLiveData: LiveData<List<EpgChannel>>) : MediatorLiveData<List<EpgChannelEntry>>() {
        private var startTime: Long? = null
        private var hoursPerScreen: Int? = null
        private var epgChannels: List<EpgChannel>? = null
        private var active = false
        private var queryJob: Job? = null

        init {
            addSource(startTimeLiveData) { start ->
                startTime = start
                queryIfNeeded()
            }
            addSource(hoursOfEpgDataPerScreenLiveData) { hours ->
                hoursPerScreen = hours
                queryIfNeeded()
            }
            addSource(epgChannelLiveData) { channels ->
                epgChannels = channels
                queryIfNeeded()
            }
        }

        override fun onActive() {
            super.onActive()
            active = true
            queryIfNeeded()
        }

        override fun onInactive() {
            super.onInactive()
            active = false
        }

        private fun queryIfNeeded() {
            if (!active) {
                return
            }
            val startTime = startTime ?: return
            val hours = hoursPerScreen ?: return
            val channels = epgChannels ?: return

            queryJob?.cancel()
            queryJob = scope.launch {
                value = withContext(Dispatchers.IO) {
                    val durationMillis = hours * 60 * 60 * 1000
                    val start = startTime + fragmentId * durationMillis
                    val end = start + durationMillis

                    channels.map { channel ->
                        val programs = programDataSource.getItemByChannelIdAndBetweenTime(channel.id, start, end)
                        EpgChannelEntry(channel, programs)
                    }
                }
            }
        }
    }
}
