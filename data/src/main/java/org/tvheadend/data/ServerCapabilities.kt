package org.tvheadend.data

class ServerCapabilities(private val htspVersion: Int) {
    val seriesRecordingSupported get() = htspVersion >= 13
    val recordingProfileSupported get() = htspVersion >= 16
    val timerRecordingSupported get() = htspVersion >= 18
    val timerRecordingEnabledSupported get() = htspVersion >= 19
    val recordingDirectorySupported get() = htspVersion >= 19
    val duplicateDetectionSupported get() = htspVersion >= 20
    val recordingOnAllChannelsSupported get() = htspVersion >= 21
    val recordingTitleSupported get() = htspVersion >= 21
    val recordingSubtitleSupported get() = htspVersion >= 21
    val recordingDescriptionSupported get() = htspVersion >= 21
    val recordingSummarySupported get() = htspVersion >= 21
    val recordingEnabledSupported get() = htspVersion >= 23
    val recordingDuplicateSupported get() = htspVersion >= 33
}