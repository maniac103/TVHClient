package org.tvheadend.tvhclient.util.extensions

import android.content.Context
import org.tvheadend.data.entity.ChannelWithProgram
import org.tvheadend.data.entity.ProgramBaseInterface
import org.tvheadend.data.entity.ProgramInterface
import org.tvheadend.data.entity.RecordingInterface
import org.tvheadend.tvhclient.R
import java.util.Locale


fun RecordingInterface.determineContentTypeColor(context: Context) =
    context.determineContentTypeColor(contentType * 16)

fun RecordingInterface.determineRecordingStateText(context: Context): String? {
    val stateTextResId = when {
        isFailed -> R.string.recording_state_failed
        isCompleted -> R.string.recording_state_completed
        isMissed -> R.string.recording_state_missed
        isRecording -> R.string.recording_state_recording
        isScheduled -> R.string.recording_state_scheduled
        else -> null
    }
    return stateTextResId?.let { context.getString(it) }
}

fun RecordingInterface.determineFailedReasonText(context: Context): String? {
    val textResId = when {
        isAborted -> R.string.recording_canceled
        isMissed -> R.string.recording_time_missed
        isFailed -> R.string.recording_file_invalid
        isFileMissing -> R.string.recording_file_missing
        else -> null
    }
    return textResId?.let { context.getString(it) }
}

fun RecordingInterface.determineDataSizeText(context: Context): String? {
    if (isScheduled && !isRecording) {
        return null
    }
    return if (dataSize > 1048576) {
        context.getString(R.string.data_size, dataSize / 1048576, "MB")
    } else {
        context.getString(R.string.data_size, dataSize / 1024, "KB")
    }
}

fun RecordingInterface.determineDataErrorText(context: Context): String? {
    if ((isScheduled && !isRecording) || dataErrors.isNullOrEmpty()) {
        return null
    }
    return context.getString(R.string.data_errors, dataErrors)
}

fun RecordingInterface.determineSubscriptionErrorText(context: Context): String? {
    if (isScheduled || subscriptionError.isNullOrEmpty()) {
        return null
    }
    return context.getString(R.string.subscription_error, subscriptionError)
}

fun RecordingInterface.determineStreamErrorText(context: Context): String? {
    if (isScheduled || streamErrors.isNullOrEmpty()) {
        return null
    }
    return context.getString(R.string.stream_errors, streamErrors)
}

fun ChannelWithProgram.determineContentTypeColor(context: Context) =
    context.determineContentTypeColor(programContentType)

fun ProgramBaseInterface.determineContentTypeColor(context: Context, alphaOffset: Int = 0) =
    context.determineContentTypeColor(contentType, alphaOffset)

fun ProgramInterface.determineSeriesInfoText(context: Context): String {
    val season = context.getString(R.string.season)
    val episode = context.getString(R.string.episode)
    val part = context.getString(R.string.part)

    episodeOnscreen
        ?.takeIf { it.isNotEmpty() }
        ?.let { return it }

    val items = listOf(
        seasonNumber.takeIf { it > 0 }?.let { "${season.lowercase()} $it" },
        episodeNumber.takeIf { it > 0 }?.let { "${episode.lowercase()} $it" },
        partNumber.takeIf { it > 0 }?.let { "${part.lowercase()} $it" }
    )
    return items
        .filterNotNull()
        .joinToString(", ")
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}
