package org.tvheadend.data

class ServerCapabilities(private val htspVersion: Int) {
    val recordingEnabledSupported get() = htspVersion >= 19
    val recordingDuplicateSupported get() = htspVersion >= 33
}