package org.tvheadend.data

class ServerCapabilities(private val htspVersion: Int) {
    val recordingProfileSupported get() = htspVersion >= 16
    val recordingEnabledSupported get() = htspVersion >= 19
    val recordingDirectorySupported get() = htspVersion >= 19
    val recordingOnAllChannelsSupported get() = htspVersion >= 21

    val recordingDuplicateSupported get() = htspVersion >= 33
}