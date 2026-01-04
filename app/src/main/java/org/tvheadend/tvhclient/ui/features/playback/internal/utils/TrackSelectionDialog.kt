package org.tvheadend.tvhclient.ui.features.playback.internal.utils

import android.app.Dialog
import android.content.DialogInterface
import android.content.res.Resources
import android.os.Bundle
import android.util.SparseArray
import android.util.SparseIntArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import org.tvheadend.tvhclient.R
import androidx.core.util.size
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector

@UnstableApi
class TrackSelectionDialog : DialogFragment() {

    private val tabFragments: SparseArray<TrackSelectionViewFragment> = SparseArray()
    private val tabTrackTypeIndices = SparseIntArray()
    private var titleId = 0
    private lateinit var onClickListener: DialogInterface.OnClickListener

    @OptIn(UnstableApi::class)
    private fun init(tracks: Tracks,
                     initialParameters: TrackSelectionParameters,
                     onClickListener: DialogInterface.OnClickListener) {

        this.titleId = R.string.track_selection_title
        this.onClickListener = onClickListener

        SUPPORTED_TRACK_TYPES.forEachIndexed { index, type ->
            val groups = tracks.groups.filter { it.type == type }
            val tabFragment = TrackSelectionViewFragment()
            tabFragment.init(
                groups,
                initialParameters.disabledTrackTypes.contains(type),
                initialParameters.overrides
            )
            tabFragments.put(index, tabFragment)
            tabTrackTypeIndices.put(type, index)
        }
    }

    @OptIn(UnstableApi::class)
    private fun getIsDisabled(type: Int): Boolean {
        val rendererView = tabFragments[tabTrackTypeIndices[type]]
        return rendererView?.isDisabled == true
    }

    private fun getOverrides(type: Int): Map<TrackGroup, TrackSelectionOverride>? {
        val rendererView = tabFragments[tabTrackTypeIndices[type]]
        return rendererView?.overrides
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // We need to own the view to let tab layout work correctly on all API levels. We can't use
        // AlertDialog because it owns the view itself, so we use AppCompatDialog instead, themed using
        // the AlertDialog theme overlay with force-enabled title.
        val dialog = AppCompatDialog(requireActivity(), R.style.TrackSelectionDialogThemeOverlay)
        dialog.setTitle(titleId)
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val dialogView = inflater.inflate(R.layout.track_selection_dialog, container, false)
        val tabLayout: TabLayout = dialogView.findViewById(R.id.track_selection_dialog_tab_layout)
        val viewPager: ViewPager = dialogView.findViewById(R.id.track_selection_dialog_view_pager)
        val cancelButton = dialogView.findViewById<Button>(R.id.track_selection_dialog_cancel_button)
        val okButton = dialogView.findViewById<Button>(R.id.track_selection_dialog_ok_button)

        viewPager.adapter = FragmentAdapter(childFragmentManager)
        tabLayout.setupWithViewPager(viewPager)
        tabLayout.visibility = if (tabFragments.size > 1) View.VISIBLE else View.GONE
        cancelButton.setOnClickListener { dismiss() }
        okButton.setOnClickListener {
            onClickListener.onClick(dialog, DialogInterface.BUTTON_POSITIVE)
            dismiss()
        }
        return dialogView
    }

    private inner class FragmentAdapter(fragmentManager: FragmentManager?) : FragmentPagerAdapter(fragmentManager!!, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        override fun getItem(position: Int): Fragment {
            return tabFragments.valueAt(position)
        }

        override fun getCount(): Int {
            return tabFragments.size
        }

        override fun getPageTitle(position: Int): CharSequence = tabTrackTypeIndices
            .indexOfValue(position)
            .takeIf { it >= 0 }
            ?.let { tabTrackTypeIndices.keyAt(it) }
            ?.let { getTrackTypeString(resources, it) }
            ?: ""
    }

    companion object {

        fun willHaveContent(player: Player) =
            player.currentTracks.groups.any { SUPPORTED_TRACK_TYPES.contains(it.type) }

        /**
         * Creates a dialog for a given [DefaultTrackSelector], whose parameters
         * will be automatically updated when tracks are selected.
         *
         * @param trackSelector     The [DefaultTrackSelector].
         */
        fun createForPlayer(player: Player): TrackSelectionDialog {
            val trackSelectionDialog = TrackSelectionDialog()
            val parameters = player.trackSelectionParameters

            trackSelectionDialog.init(player.currentTracks, parameters) { _: DialogInterface?, _: Int ->
                val builder = parameters.buildUpon()
                SUPPORTED_TRACK_TYPES.forEach { type ->
                    builder.setTrackTypeDisabled(type, trackSelectionDialog.getIsDisabled(type))
                    builder.clearOverridesOfType(type)
                    trackSelectionDialog.getOverrides(type)?.values?.forEach { builder.addOverride(it) }
                }
                player.trackSelectionParameters = builder.build()
            }
            return trackSelectionDialog
        }

        private fun getTrackTypeString(resources: Resources, trackType: Int): String {
            return when (trackType) {
                C.TRACK_TYPE_VIDEO -> resources.getString(R.string.player_track_type_video)
                C.TRACK_TYPE_AUDIO -> resources.getString(R.string.player_track_type_audio)
                C.TRACK_TYPE_TEXT -> resources.getString(R.string.player_track_type_text)
                else -> "unknown"
            }
        }

        private val SUPPORTED_TRACK_TYPES = listOf(C.TRACK_TYPE_VIDEO, C.TRACK_TYPE_AUDIO, C.TRACK_TYPE_TEXT)
    }

    init {
        // Retain instance across activity re-creation to prevent losing access to init data.
        retainInstance = true
    }
}