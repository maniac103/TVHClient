package org.tvheadend.tvhclient.ui.features.playback.internal.utils

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.RendererCapabilities
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.trackselection.MappingTrackSelector
import timber.log.Timber

@UnstableApi
class CustomEventLogger(private val trackSelector: MappingTrackSelector) : AnalyticsListener {

    @androidx.annotation.OptIn(UnstableApi::class)
    override fun onTracksChanged(eventTime: AnalyticsListener.EventTime, tracks: Tracks) {
        val mappedTrackInfo = trackSelector.currentMappedTrackInfo
        if (mappedTrackInfo == null) {
            Timber.d("No media tracks available")
            return
        }

        Timber.d("Available media tracks:")

        tracks.groups.forEach { group ->
            Timber.d("  Track group ${group.type}: adaptive streaming supported: ${group.isAdaptiveSupported}")
            (0 until group.length).forEach { index ->
                val isEnabled = getTrackStatusString(group.isSupported)
                val metadata = Format.toLogString(group.mediaTrackGroup.getFormat(index))
                val formatSupport = getFormatSupportString(group.getTrackSupport(index))
                Timber.d("    Track:$index, selected=$isEnabled, $metadata, supported=$formatSupport")
            }
            (0 until group.length)
                .firstOrNull { group.isTrackSelected(it) }
                ?.let { group.mediaTrackGroup.getFormat(it).metadata }
                ?.let { Timber.d("  Metadata: $it") }
        }

        // Log tracks not associated with a renderer.
        val unassociatedTrackGroups = mappedTrackInfo.unmappedTrackGroups
        if (unassociatedTrackGroups.length > 0) {
            Timber.d("  Renderer:none")
            for (groupIndex in 0 until unassociatedTrackGroups.length) {
                Timber.d("    Group:$groupIndex")
                val trackGroup = unassociatedTrackGroups[groupIndex]
                for (trackIndex in 0 until trackGroup.length) {
                    val isEnabled = getTrackStatusString(false)
                    val metadata = Format.toLogString(trackGroup.getFormat(trackIndex))
                    val formatSupport = getFormatSupportString(C.FORMAT_UNSUPPORTED_TYPE)
                    Timber.d("      Track:$trackIndex, selected=$isEnabled, $metadata, supported=$formatSupport")
                }
            }
        }
    }

    private fun getFormatSupportString(formatSupport: Int): String {
        return when (formatSupport) {
            C.FORMAT_HANDLED -> "yes"
            C.FORMAT_EXCEEDS_CAPABILITIES -> "no, exceeds capabilities"
            C.FORMAT_UNSUPPORTED_DRM -> "no, unsupported drm"
            C.FORMAT_UNSUPPORTED_SUBTYPE -> "no, unsupported type"
            C.FORMAT_UNSUPPORTED_TYPE -> "no"
            else -> "unknown"
        }
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    private fun getAdaptiveSupportString(trackCount: Int, adaptiveSupport: Int): String {
        return if (trackCount < 2) {
            "n/a"
        } else when (adaptiveSupport) {
            RendererCapabilities.ADAPTIVE_SEAMLESS -> "yes"
            RendererCapabilities.ADAPTIVE_NOT_SEAMLESS -> "yes but not seamless"
            RendererCapabilities.ADAPTIVE_NOT_SUPPORTED -> "no"
            else -> "unknown"
        }
    }

    private fun getTrackStatusString(enabled: Boolean): String {
        return if (enabled) "yes" else "no"
    }
}