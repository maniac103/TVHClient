package org.tvheadend.tvhclient.ui.features.playback.internal.utils

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.TrackSelectionView
import org.tvheadend.tvhclient.R

@UnstableApi
class TrackSelectionViewFragment : Fragment(), TrackSelectionView.TrackSelectionListener {

    private lateinit var trackGroups: List<Tracks.Group>
    private var allowAdaptiveSelections = false
    private var allowMultipleOverrides = false

    var isDisabled = false
    var overrides: Map<TrackGroup, TrackSelectionOverride> = emptyMap()

    init {
        // Retain instance across activity re-creation to prevent losing access to init data.
        retainInstance = true
    }

    fun init(trackGroups: List<Tracks.Group>,
             initialIsDisabled: Boolean,
             initialOverrides: Map<TrackGroup, TrackSelectionOverride>,
             allowAdaptiveSelections: Boolean = true,
             allowMultipleOverrides: Boolean = false) {

        this.trackGroups = trackGroups
        this.isDisabled = initialIsDisabled
        this.overrides = initialOverrides
        this.allowAdaptiveSelections = allowAdaptiveSelections
        this.allowMultipleOverrides = allowMultipleOverrides
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.exo_track_selection_dialog, container, false)
        val trackSelectionView: TrackSelectionView = rootView.findViewById(R.id.exo_track_selection_view)

        trackSelectionView.setShowDisableOption(true)
        trackSelectionView.setAllowMultipleOverrides(allowMultipleOverrides)
        trackSelectionView.setAllowAdaptiveSelections(allowAdaptiveSelections)
        trackSelectionView.init(trackGroups, isDisabled, overrides, null, this)
        return rootView
    }

    override fun onTrackSelectionChanged(
        isDisabled: Boolean,
        overrides: Map<TrackGroup, TrackSelectionOverride>
    ) {
        this.isDisabled = isDisabled
        this.overrides = overrides
    }
}