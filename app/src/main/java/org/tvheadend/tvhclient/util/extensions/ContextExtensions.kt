package org.tvheadend.tvhclient.util.extensions

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.snackbar.Snackbar
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.service.SyncStateReceiver
import org.tvheadend.tvhclient.service.SyncStateResult
import org.tvheadend.tvhclient.ui.common.SnackbarMessageReceiver
import org.tvheadend.tvhclient.ui.common.getLocale
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.max
import kotlin.text.isNotEmpty

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

fun Context.determineDaysOfWeekText(daysOfWeekBitmask: Int): String {
    val daysOfWeekList = resources.getStringArray(R.array.day_short_names)
    return (0..6)
        .mapNotNull { if (daysOfWeekBitmask shr it and 1 == 1) daysOfWeekList[it] else null }
        .joinToString(", ")
}

fun Context.determinePriorityText(priority: Int): String? {
    val priorityNames = resources.getStringArray(R.array.dvr_priority_names)
    return when (priority) {
        in 0..4 -> priorityNames[priority]
        6 -> priorityNames[5]
        else -> null
    }
}

fun Context.formatStartStopTime(startTime: Long, stopTime: Long): String {
    val df = if (prefs.localizedTimeFormat) {
        // Show the date as defined with the currently active locale.
        // For the date display the short version will be used
        java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT, getLocale(resources))
    } else {
        // Show the date using the default format like 31.07.2013
        SimpleDateFormat("HH:mm", Locale.US)
    }

    val startText = if (startTime < 0) getString(R.string.any) else df.format(startTime)
    val stopText = if (stopTime < 0) getString(R.string.any) else df.format(stopTime)
    return "$startText - $stopText"
}

fun Context.formatTime(time: Long): String {
    if (time < 0) {
        return getString(R.string.any)
    }

    val df = if (prefs.localizedTimeFormat) {
        // Show the date as defined with the currently active locale.
        // For the date display the short version will be used
        java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT, getLocale(resources))
    } else {
        // Show the date using the default format like 31.07.2013
        SimpleDateFormat("HH:mm", Locale.US)
    }
    return df.format(time)
}

fun Context.formatDate(date: Long): String {
    if (date < 0) {
        return getString(R.string.any)
    }

    val oneDayMillis = 24L * 60 * 60 * 1000
    val dateDiffInDays = date / oneDayMillis - System.currentTimeMillis() / oneDayMillis

    return when (dateDiffInDays) {
        0L -> getString(R.string.today)
        1L -> getString(R.string.tomorrow)
        -1L -> getString(R.string.yesterday)
        in -6L..-2L, in 2L..6L -> {
            // show matching day of week
            val sdf = SimpleDateFormat("EEEE", getLocale(resources))
            sdf.format(date)
        }
        else -> {
            val df = if (prefs.localizedTimeFormat) {
                // Show the date as defined with the currently active locale.
                // For the date display the short version will be used
                java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT, getLocale(resources))
            } else {
                // Show the date using the default format like 31.07.2013
                SimpleDateFormat("dd.MM.yyyy", Locale.US)
            }
            df.format(date)
        }
    }
}

fun Context.determineContentTypeColor(contentType: Int, alphaOffset: Int = 0): Int? {
    val colorResId = when (contentType / 16) {
        1 -> R.color.EPG_MOVIES
        2 -> R.color.EPG_NEWS
        3 -> R.color.EPG_SHOWS
        4 -> R.color.EPG_SPORTS
        5 -> R.color.EPG_CHILD
        6 -> R.color.EPG_MUSIC
        7 -> R.color.EPG_ARTS
        8 -> R.color.EPG_SOCIAL
        9 -> R.color.EPG_SCIENCE
        10 -> R.color.EPG_HOBBY
        11 -> R.color.EPG_SPECIAL
        else -> null
    }

    return colorResId?.let {
        val color = ContextCompat.getColor(this, it)
        val alpha = max(((prefs.genreColorTransparencyPercent.toFloat() - alphaOffset) / 100.0f * 255.0f).toInt(), 0)
        Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }
}

fun Context.determineContentTypeText(contentType: Int): String? {
    val arrayResIds = listOf(
        null, // 0 is undefined
        R.array.pr_content_type1,
        R.array.pr_content_type2,
        R.array.pr_content_type3,
        R.array.pr_content_type4,
        R.array.pr_content_type5,
        R.array.pr_content_type6,
        R.array.pr_content_type7,
        R.array.pr_content_type8,
        R.array.pr_content_type9,
        R.array.pr_content_type10,
        R.array.pr_content_type11,
    )
    val arrayIndex = contentType / 16
    val arrayValues = arrayResIds.getOrNull(arrayIndex)?.let { resources.getStringArray(it) }
    return arrayValues?.getOrNull(contentType % 16)
}

fun Context.interpretColoredText(text: String?): CharSequence? {
    if (text.isNullOrEmpty()) {
        return null
    }

    if (!text.contains("[COLOR ") || !text.contains("[/COLOR]")) {
        return text
    }

    val builder = SpannableStringBuilder()
    builder.append(text.substringBefore("[COLOR ", ""))

    text.split("[COLOR").forEach { str ->
        val currentPosition = builder.length
        val colorName = str.substringBefore("]", "").trim()
        val coloredText = str.substringAfter("]", "")
            .substringBefore("[/COLOR]", "")
        val remainingText = str.substringAfter("[/COLOR]", "")

        builder.append(coloredText)
        builder.append(remainingText)

        resources.getIdentifier(colorName, "color", packageName)
            .takeIf { it > 0 }
            ?.let { ForegroundColorSpan(ContextCompat.getColor(this, it)) }
            ?.let { builder.setSpan(it, currentPosition, currentPosition + coloredText.length, 0) }
    }
    return builder
}
