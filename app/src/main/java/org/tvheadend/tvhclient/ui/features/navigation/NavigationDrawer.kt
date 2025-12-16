package org.tvheadend.tvhclient.ui.features.navigation

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.util.TypedValue
import android.view.View
import androidx.annotation.AttrRes
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.preference.PreferenceManager
import com.google.android.material.color.MaterialColors
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.mikepenz.materialdrawer.holder.ImageHolder
import com.mikepenz.materialdrawer.holder.StringHolder
import com.mikepenz.materialdrawer.model.DividerDrawerItem
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.mikepenz.materialdrawer.model.ProfileDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.descriptionText
import com.mikepenz.materialdrawer.model.interfaces.iconRes
import com.mikepenz.materialdrawer.model.interfaces.nameRes
import com.mikepenz.materialdrawer.model.interfaces.nameText
import com.mikepenz.materialdrawer.util.addItems
import com.mikepenz.materialdrawer.util.updateBadge
import com.mikepenz.materialdrawer.widget.AccountHeaderView
import com.mikepenz.materialdrawer.widget.MaterialDrawerSliderView
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.features.channels.ChannelListFragment
import org.tvheadend.tvhclient.ui.features.dvr.recordings.CompletedRecordingListFragment
import org.tvheadend.tvhclient.ui.features.dvr.recordings.FailedRecordingListFragment
import org.tvheadend.tvhclient.ui.features.dvr.recordings.RemovedRecordingListFragment
import org.tvheadend.tvhclient.ui.features.dvr.recordings.ScheduledRecordingListFragment
import org.tvheadend.tvhclient.ui.features.dvr.series_recordings.SeriesRecordingListFragment
import org.tvheadend.tvhclient.ui.features.dvr.timer_recordings.TimerRecordingListFragment
import org.tvheadend.tvhclient.ui.features.epg.EpgFragment
import org.tvheadend.tvhclient.ui.features.information.HelpAndSupportFragment
import org.tvheadend.tvhclient.ui.features.information.StatusFragment
import org.tvheadend.tvhclient.ui.features.information.StatusViewModel
import org.tvheadend.tvhclient.ui.features.information.WebViewFragment
import org.tvheadend.tvhclient.ui.features.settings.SettingsActivity
import timber.log.Timber
import androidx.core.graphics.drawable.toDrawable
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class NavigationDrawer(private val activity: AppCompatActivity,
                       private val drawer: MaterialDrawerSliderView,
                       private val drawerLayout: DrawerLayout,
                       private val navigationViewModel: NavigationViewModel,
                       statusViewModel: StatusViewModel,
                       private val isDualPane: Boolean) {
    private lateinit var headerView: AccountHeaderView

    init {
        createHeader()
        createMenu()

        val drawerBackgroundColor = (drawer.background as? ColorDrawable)?.color
        val cornerRadius = drawer.resources.getDimensionPixelSize(R.dimen.drawer_corner_radius)
        val shapeBuilder = ShapeAppearanceModel.builder().setAllCornerSizes(cornerRadius.toFloat())
        if (drawer.layoutDirection == View.LAYOUT_DIRECTION_RTL) {
            shapeBuilder.setTopRightCornerSize(0f)
            shapeBuilder.setBottomRightCornerSize(0f)
        } else {
            shapeBuilder.setTopLeftCornerSize(0f)
            shapeBuilder.setBottomLeftCornerSize(0f)
        }

        val drawerBackground = MaterialShapeDrawable.createWithElevationOverlay(
            activity,
            0F,
            drawerBackgroundColor?.let { ColorStateList.valueOf(it) }
        )
        drawerBackground.shapeAppearanceModel = shapeBuilder.build()
        drawer.background = drawerBackground
        drawer.clipToOutline = true

        navigationViewModel.connectionLiveData.observe(activity) {
            this.showConnectionsInDrawerHeader()
            it?.let { headerView.setActiveProfile(it.id.toLong()) }
        }

        observeCount(statusViewModel.channelCount, MENU_CHANNELS)
        observeCount(statusViewModel.seriesRecordingCount, MENU_SERIES_RECORDINGS)
        observeCount(statusViewModel.timerRecordingCount, MENU_TIMER_RECORDINGS)
        observeCount(statusViewModel.completedRecordingCount, MENU_COMPLETED_RECORDINGS)
        observeCount(statusViewModel.scheduledRecordingCount, MENU_SCHEDULED_RECORDINGS)
        observeCount(statusViewModel.failedRecordingCount, MENU_FAILED_RECORDINGS)
        observeCount(statusViewModel.removedRecordingCount, MENU_REMOVED_RECORDINGS)
    }

    private fun createHeader() {
        headerView = AccountHeaderView(activity).apply {
            profileImagesVisible = false
            selectionListEnabledForSingleProfile = false

            val bgColor = MaterialColors.getColor(this, R.attr.colorPrimaryContainer)
            headerBackground = ImageHolder(bgColor.toDrawable())

            onAccountHeaderListener = { _, profile, current ->
                drawerLayout.closeDrawers()

                // Do nothing if the same profile has been selected
                if (current) {
                    true
                } else {
                    MaterialAlertDialogBuilder(activity)
                        .setTitle(R.string.connect_to_new_server)
                        .setNegativeButton(R.string.cancel) { _, _ ->
                            setActiveProfile(navigationViewModel.connection.id.toLong())
                        }
                        .setPositiveButton(R.string.connect) { _, _ ->
                            setActiveProfile(profile.identifier)
                            if (navigationViewModel.setSelectedConnectionAsActive(profile.identifier.toInt())) {
                                navigationViewModel.updateConnectionAndRestartApplication(activity)
                            }
                        }
                        .setCancelable(false)
                        .show()
                    false
                }
            }

            attachToSliderView(drawer)
        }
    }

    private fun observeCount(countLiveData: LiveData<Int>, menuIdentifier: Int) {
        countLiveData.observe(activity) { count ->
            drawer.updateBadge(menuIdentifier.toLong(), StringHolder(count.toString()))
        }
    }

    private fun createMenu() {
        val channelItem = PrimaryDrawerItem().apply {
            identifier = MENU_CHANNELS.toLong()
            nameRes = R.string.channels
            iconRes = getResourceIdFromAttr(R.attr.ic_menu_channels)
        }
        val programGuideItem = PrimaryDrawerItem().apply {
            identifier = MENU_PROGRAM_GUIDE.toLong()
            nameRes = R.string.pref_program_guide
            iconRes = getResourceIdFromAttr(R.attr.ic_menu_program_guide)
        }
        val completedRecordingsItem = PrimaryDrawerItem().apply {
            identifier = MENU_COMPLETED_RECORDINGS.toLong()
            nameRes = R.string.completed_recordings
            iconRes = getResourceIdFromAttr(R.attr.ic_menu_completed_recordings)
        }
        val scheduledRecordingsItem = PrimaryDrawerItem().apply {
            identifier = MENU_SCHEDULED_RECORDINGS.toLong()
            nameRes = R.string.scheduled_recordings
            iconRes = getResourceIdFromAttr(R.attr.ic_menu_scheduled_recordings)
        }
        val seriesRecordingsItem = PrimaryDrawerItem().apply {
            identifier = MENU_SERIES_RECORDINGS.toLong()
            nameRes = R.string.series_recordings
            iconRes = getResourceIdFromAttr(R.attr.ic_menu_scheduled_recordings)
        }
        val timerRecordingsItem = PrimaryDrawerItem().apply {
            identifier = MENU_TIMER_RECORDINGS.toLong()
            nameRes = R.string.timer_recordings
            iconRes = getResourceIdFromAttr(R.attr.ic_menu_scheduled_recordings)
        }
        val failedRecordingsItem = PrimaryDrawerItem().apply {
            identifier = MENU_FAILED_RECORDINGS.toLong()
            nameRes = R.string.failed_recordings
            iconRes = getResourceIdFromAttr(R.attr.ic_menu_failed_recordings)
        }
        val removedRecordingsItem = PrimaryDrawerItem().apply {
            identifier = MENU_REMOVED_RECORDINGS.toLong()
            nameRes = R.string.removed_recordings
            iconRes = getResourceIdFromAttr(R.attr.ic_menu_removed_recordings)
        }
        val statusItem = PrimaryDrawerItem().apply {
            identifier = MENU_STATUS.toLong()
            nameRes = R.string.status
            iconRes = getResourceIdFromAttr(R.attr.ic_menu_status)
        }
        val settingsItem = PrimaryDrawerItem().apply {
            identifier = MENU_SETTINGS.toLong()
            nameRes = R.string.settings
            iconRes = getResourceIdFromAttr(R.attr.ic_menu_settings)
            isSelectable = false
        }
        val helpItem = PrimaryDrawerItem().apply {
            identifier = MENU_HELP.toLong()
            nameRes = R.string.help_and_support
            iconRes = getResourceIdFromAttr(R.attr.ic_menu_help)
        }

        drawer.addItems(
            channelItem,
            programGuideItem,
            DividerDrawerItem(),
            completedRecordingsItem,
            scheduledRecordingsItem,
            seriesRecordingsItem,
            timerRecordingsItem,
            failedRecordingsItem,
            removedRecordingsItem,
            DividerDrawerItem(),
            settingsItem,
            helpItem,
            statusItem
        )
        drawer.onDrawerItemClickListener = { _, item, _ ->
            drawerLayout.closeDrawers()
            navigationViewModel.setNavigationMenuId(item.identifier.toInt())
            true
        }
    }

    private fun getResourceIdFromAttr(@AttrRes attr: Int): Int {
        val typedValue = TypedValue()
        activity.theme.resolveAttribute(attr, typedValue, true)
        return typedValue.resourceId
    }

    private fun showConnectionsInDrawerHeader() {
        // Remove old profiles from the header
        headerView.profiles
            ?.map { it.identifier }
            ?.forEach { headerView.removeProfileByIdentifier(it) }
        // Add the existing connections as new profiles
        if (navigationViewModel.connections.isNotEmpty()) {
            navigationViewModel.connections.forEach {
                headerView.addProfiles(
                    ProfileDrawerItem().apply {
                        identifier = it.id.toLong()
                        nameText = it.name ?: ""
                        descriptionText = it.serverUrl ?: ""
                    }
                )
            }
        } else {
            headerView.addProfiles(
                ProfileDrawerItem().apply {
                    nameRes = R.string.no_connection_available
                }
            )
        }
    }

    fun getSelectedMenu(): Int {
        return drawer.selectedItemIdentifier.toInt()
    }

    fun setSelectedNavigationDrawerMenuFromFragmentType(fragment: Fragment?) {
        when (fragment) {
            is ChannelListFragment -> drawer.setSelection(MENU_CHANNELS.toLong(), false)
            is EpgFragment -> drawer.setSelection(MENU_PROGRAM_GUIDE.toLong(), false)
            is CompletedRecordingListFragment -> drawer.setSelection(MENU_COMPLETED_RECORDINGS.toLong(), false)
            is ScheduledRecordingListFragment -> drawer.setSelection(MENU_SCHEDULED_RECORDINGS.toLong(), false)
            is SeriesRecordingListFragment -> drawer.setSelection(MENU_SERIES_RECORDINGS.toLong(), false)
            is TimerRecordingListFragment -> drawer.setSelection(MENU_TIMER_RECORDINGS.toLong(), false)
            is FailedRecordingListFragment -> drawer.setSelection(MENU_FAILED_RECORDINGS.toLong(), false)
            is RemovedRecordingListFragment -> drawer.setSelection(MENU_REMOVED_RECORDINGS.toLong(), false)
            is StatusFragment -> drawer.setSelection(MENU_STATUS.toLong(), false)
            is WebViewFragment -> drawer.setSelection(MENU_HELP.toLong(), false)
        }
    }

    /**
     * Creates and returns a new fragment that is associated with the given menu
     */
    private fun getFragmentFromSelectedNavigationDrawerMenu(position: Int): Fragment? {
        return when (position) {
            MENU_CHANNELS -> ChannelListFragment()
            MENU_PROGRAM_GUIDE -> EpgFragment()
            MENU_COMPLETED_RECORDINGS -> CompletedRecordingListFragment()
            MENU_SCHEDULED_RECORDINGS -> ScheduledRecordingListFragment()
            MENU_SERIES_RECORDINGS -> SeriesRecordingListFragment()
            MENU_TIMER_RECORDINGS -> TimerRecordingListFragment()
            MENU_FAILED_RECORDINGS -> FailedRecordingListFragment()
            MENU_REMOVED_RECORDINGS -> RemovedRecordingListFragment()
            MENU_HELP -> HelpAndSupportFragment()
            MENU_STATUS -> StatusFragment()
            else -> null
        }
    }

    /**
     * Called when a menu item from the navigation drawer was selected. It loads
     * and shows the correct fragment or fragments depending on the selected
     * menu item.
     *
     * @param id Selected position within the menu array
     */
    fun handleDrawerItemSelected(id: Int) {
        Timber.d("Handling new navigation menu id $id")

        if (id == MENU_SETTINGS) {
            activity.startActivity(Intent(activity, SettingsActivity::class.java))
            return
        }

        // A new or existing main fragment shall be shown. So save the menu position so we
        // know which one was selected. Additionally remove any old details fragment in case
        // dual pane mode is active to prevent showing wrong details data.
        // Finally show the new main fragment and add it to the back stack
        // only if it is a new fragment and not an existing one.
        val fragment = getFragmentFromSelectedNavigationDrawerMenu(id)
        if (fragment != null) {
            if (isDualPane) {
                val detailsFragment = activity.supportFragmentManager.findFragmentById(R.id.details)
                if (detailsFragment != null) {
                    activity.supportFragmentManager.beginTransaction().remove(detailsFragment).commit()
                }
            }
            activity.supportFragmentManager.beginTransaction().replace(R.id.main, fragment).let {
                val addFragmentToBackStack = PreferenceManager.getDefaultSharedPreferences(activity).getBoolean("navigation_history_enabled", activity.resources.getBoolean(R.bool.pref_default_navigation_history_enabled))
                if (addFragmentToBackStack) {
                    it.addToBackStack(null)
                }
                it.commit()
            }
        }
    }

    companion object {

        // The index for the navigation drawer menus
        const val MENU_CHANNELS = 0
        const val MENU_PROGRAM_GUIDE = 1
        const val MENU_COMPLETED_RECORDINGS = 2
        const val MENU_SCHEDULED_RECORDINGS = 3
        const val MENU_SERIES_RECORDINGS = 4
        const val MENU_TIMER_RECORDINGS = 5
        const val MENU_FAILED_RECORDINGS = 6
        const val MENU_REMOVED_RECORDINGS = 7
        const val MENU_STATUS = 8
        const val MENU_SETTINGS = 9
        const val MENU_HELP = 10
    }
}
