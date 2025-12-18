package org.tvheadend.tvhclient.ui.features.information

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import org.tvheadend.data.entity.Channel
import org.tvheadend.data.entity.Input
import org.tvheadend.data.entity.Subscription
import org.tvheadend.tvhclient.service.ConnectionService
import org.tvheadend.tvhclient.ui.base.BaseViewModel
import org.tvheadend.tvhclient.util.extensions.channelDataSource
import org.tvheadend.tvhclient.util.extensions.inputDataSource
import org.tvheadend.tvhclient.util.extensions.prefs
import org.tvheadend.tvhclient.util.extensions.programDataSource
import org.tvheadend.tvhclient.util.extensions.recordingDataSource
import org.tvheadend.tvhclient.util.extensions.seriesRecordingDataSource
import org.tvheadend.tvhclient.util.extensions.serverStatusDataSource
import org.tvheadend.tvhclient.util.extensions.subscriptionDataSource
import org.tvheadend.tvhclient.util.extensions.timerRecordingDataSource
import org.tvheadend.tvhclient.util.livedata.CombinedPairLiveData
import org.tvheadend.tvhclient.util.livedata.CombinedTupleLiveData
import timber.log.Timber

class StatusViewModel(private val application: Application) : BaseViewModel(application) {
    val serverStatusLiveData = application.serverStatusDataSource.liveDataActiveItem
    val channelCount = application.channelDataSource.getLiveDataItemCount()
    val programCount = application.programDataSource.getLiveDataItemCount()
    val timerRecordingCount = application.timerRecordingDataSource.getLiveDataItemCount()
    val seriesRecordingCount = application.seriesRecordingDataSource.getLiveDataItemCount()
    val completedRecordingCount = application.recordingDataSource.getLiveDataCountByType("completed")
    val scheduledRecordingCount = application.recordingDataSource.getLiveDataCountByType("scheduled")
    val failedRecordingCount = application.recordingDataSource.getLiveDataCountByType("failed")
    val removedRecordingCount = application.recordingDataSource.getLiveDataCountByType("removed")

    var runningRecordingCount = 0
    var availableStorageSpace = 0

    val showRunningRecordingCount = CombinedPairLiveData(
        application.recordingDataSource.getLiveDataCountByType("running"),
        application.prefs.notificationRunningRecordingCountLiveData()
    ) { runningRecordings, show ->
        runningRecordingCount = runningRecordings
        show && runningRecordings > 0
    }

    val showLowStorageSpace = CombinedTupleLiveData(
        application.serverStatusDataSource.liveDataActiveItem,
        application.prefs.notificationLowStorageEnabledLiveData(),
        application.prefs.notificationLowStorageThresholdGbLiveData()
    ) { serverStatus, show, threshold ->
        if (serverStatus != null) {
            availableStorageSpace = (serverStatus.freeDiskSpace / (1024 * 1024 * 1024)).toInt()
            Timber.d("Server status free space has changed to $availableStorageSpace, threshold is $threshold, checking if notification shall be shown")
            show && availableStorageSpace <= threshold
        } else {
            false
        }
    }

    val subscriptions: LiveData<List<Subscription>> = application.subscriptionDataSource.getLiveDataItems()
    val inputs: LiveData<List<Input>> = application.inputDataSource.getLiveDataItems()

    private val discSpaceUpdateTask: Runnable
    private val diskSpaceUpdateHandler = Handler(Looper.getMainLooper())

    init {
        Timber.d("Initializing")

        discSpaceUpdateTask = Runnable {
            val activityManager = application.applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val runningAppProcessInfo = activityManager.runningAppProcesses?.get(0)

            if (runningAppProcessInfo != null
                    && runningAppProcessInfo.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {

                Timber.d("Application is in the foreground, starting service to get disk space ")
                val intent = Intent(application.applicationContext, ConnectionService::class.java)
                intent.action = "getDiskSpace"
                application.applicationContext.startService(intent)
            }
            Timber.d("Restarting disc space update handler in 60s")
            diskSpaceUpdateHandler.postDelayed(discSpaceUpdateTask, 60000)
        }
    }

    override fun onCleared() {
        stopDiskSpaceUpdateHandler()
        super.onCleared()
    }

    fun getChannelById(id: Int): Channel? {
        return application.channelDataSource.getItemById(id)
    }

    fun startDiskSpaceUpdateHandler() {
        Timber.d("Starting disk space update handler")
        diskSpaceUpdateHandler.post(discSpaceUpdateTask)
    }

    fun stopDiskSpaceUpdateHandler() {
        Timber.d("Stopping disk space update handler")
        diskSpaceUpdateHandler.removeCallbacks(discSpaceUpdateTask)
    }
}