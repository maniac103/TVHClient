package org.tvheadend.tvhclient.util.extensions

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.snackbar.Snackbar
import org.tvheadend.data.source.ChannelDataSource
import org.tvheadend.data.source.ChannelTagDataSource
import org.tvheadend.data.source.ConnectionDataSource
import org.tvheadend.data.source.InputDataSource
import org.tvheadend.data.source.MiscDataSource
import org.tvheadend.data.source.ProgramDataSource
import org.tvheadend.data.source.RecordingDataSource
import org.tvheadend.data.source.SeriesRecordingDataSource
import org.tvheadend.data.source.ServerProfileDataSource
import org.tvheadend.data.source.ServerStatusDataSource
import org.tvheadend.data.source.SubscriptionDataSource
import org.tvheadend.data.source.TagAndChannelDataSource
import org.tvheadend.data.source.TimerRecordingDataSource
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.service.SyncStateReceiver
import org.tvheadend.tvhclient.service.SyncStateResult
import org.tvheadend.tvhclient.ui.common.SnackbarMessageReceiver
import timber.log.Timber

val Context.prefs get() = (applicationContext as MainApplication).preferences
val Context.channelDataSource get() = (applicationContext as MainApplication).appRepository.channelData
val Context.programDataSource get() = (applicationContext as MainApplication).appRepository.programData
val Context.recordingDataSource get() = (applicationContext as MainApplication).appRepository.recordingData
val Context.seriesRecordingDataSource get() = (applicationContext as MainApplication).appRepository.seriesRecordingData
val Context.timerRecordingDataSource get() = (applicationContext as MainApplication).appRepository.timerRecordingData
val Context.connectionDataSource get() = (applicationContext as MainApplication).appRepository.connectionData
val Context.channelTagDataSource get() = (applicationContext as MainApplication).appRepository.channelTagData
val Context.serverStatusDataSource get() = (applicationContext as MainApplication).appRepository.serverStatusData
val Context.serverProfileDataSource get() = (applicationContext as MainApplication).appRepository.serverProfileData
val Context.tagAndChannelDataSource get() = (applicationContext as MainApplication).appRepository.tagAndChannelData
val Context.miscDataSource get() = (applicationContext as MainApplication).appRepository.miscData
val Context.subscriptionDataSource get() = (applicationContext as MainApplication).appRepository.subscriptionData
val Context.inputDataSource get() = (applicationContext as MainApplication).appRepository.inputData

fun Context.sendSnackbarMessage(resId: Int, duration: Int = Snackbar.LENGTH_SHORT) {
    this.sendSnackbarMessage(this.getString(resId), duration)
}

fun Context.sendSnackbarMessage(msg: String, duration: Int = Snackbar.LENGTH_SHORT) {
    Timber.d("Sending broadcast to show snackbar message $msg")
    val intent = Intent(SnackbarMessageReceiver.SNACKBAR_ACTION)
    intent.putExtra(SnackbarMessageReceiver.SNACKBAR_CONTENT, msg)
    intent.putExtra(SnackbarMessageReceiver.SNACKBAR_DURATION, duration)
    LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
}

fun Context.getCastSession(): CastSession? {
    val castContext = this.getCastContext()
    if (castContext != null) {
        try {
            return castContext.sessionManager.currentCastSession
        } catch (e: IllegalStateException) {
            Timber.e("Could not get current cast session")
        }
    }
    return null
}

fun Context.getCastContext(): CastContext? {
    val playServicesAvailable = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)
    if (playServicesAvailable == ConnectionResult.SUCCESS) {
        try {
            return CastContext.getSharedInstance(this)
        } catch (e: RuntimeException) {
            Timber.e(e, "Could not get cast context")
        }
    }
    return null
}

fun Context.sendSyncStateMessage(state: SyncStateResult, message: String = "", details: String = "") {

    val intent = Intent(SyncStateReceiver.ACTION)
    intent.putExtra(SyncStateReceiver.STATE, state)
    if (message.isNotEmpty()) {
        intent.putExtra(SyncStateReceiver.MESSAGE, message)
    }
    if (details.isNotEmpty()) {
        intent.putExtra(SyncStateReceiver.DETAILS, details)
    }
    LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
}