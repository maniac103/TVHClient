package org.tvheadend.tvhclient.ui.features.navigation

import android.content.Intent
import android.view.Menu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.forEach
import androidx.core.view.isVisible
import androidx.core.view.iterator
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
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
import org.tvheadend.tvhclient.ui.features.settings.SettingsActivity
import timber.log.Timber
import androidx.fragment.app.commit
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import org.tvheadend.data.entity.Connection
import org.tvheadend.tvhclient.databinding.NavDrawerHeaderBinding
import org.tvheadend.tvhclient.ui.features.information.WebViewFragment
import org.tvheadend.tvhclient.util.extensions.prefs

class NavigationDrawer(private val activity: AppCompatActivity,
                       private val drawer: NavigationView,
                       private val drawerLayout: DrawerLayout,
                       private val navigationViewModel: NavigationViewModel,
                       statusViewModel: StatusViewModel,
                       private val isDualPane: Boolean) {
    private val headerBinding: NavDrawerHeaderBinding
    private var inServerSelectionMode = false
    private var activeConnectionId = -1

    init {
        val headerView = drawer.inflateHeaderView(R.layout.nav_drawer_header)
        headerBinding = NavDrawerHeaderBinding.bind(headerView)

        headerBinding.switcher.setOnClickListener {
            inServerSelectionMode = !inServerSelectionMode
            drawer.menu.apply {
                setGroupVisible(R.id.program, !inServerSelectionMode)
                setGroupVisible(R.id.recordings, !inServerSelectionMode)
                setGroupVisible(R.id.misc, !inServerSelectionMode)
                setGroupVisible(R.id.server_connections, inServerSelectionMode)
            }
            drawer.menu.forEach { item ->
                item.actionView?.isVisible = !inServerSelectionMode
            }
            headerBinding.switcher.setImageResource(
                if (inServerSelectionMode) R.drawable.ic_dropup else R.drawable.ic_dropdown
            )
        }

        drawer.setNavigationItemSelectedListener { item ->
            if (item.groupId == R.id.server_connections) {
                // Do nothing if the same profile has been selected
                if (item.itemId != activeConnectionId) {
                    MaterialAlertDialogBuilder(activity)
                        .setTitle(R.string.connect_to_new_server)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.connect) { _, _ ->
                            if (navigationViewModel.setSelectedConnectionAsActive(item.itemId)) {
                                navigationViewModel.updateConnectionAndRestartApplication(activity)
                            }
                        }
                        .setCancelable(false)
                        .show()
                }
            } else {
                val identifier = ID_MAPPING[item.itemId] ?: return@setNavigationItemSelectedListener false
                navigationViewModel.setNavigationMenuId(identifier)
            }
            drawerLayout.closeDrawers()
            true
        }

        navigationViewModel.connectionLiveData.observe(activity) { active ->
            activeConnectionId = active?.id ?: -1
            showConnectionsInDrawerHeader(active)
        }

        observeCount(statusViewModel.channelCount, R.id.channels)
        observeCount(statusViewModel.seriesRecordingCount, R.id.series_recordings)
        observeCount(statusViewModel.timerRecordingCount, R.id.timer_recordings)
        observeCount(statusViewModel.completedRecordingCount, R.id.completed_recordings)
        observeCount(statusViewModel.scheduledRecordingCount, R.id.scheduled_recordings)
        observeCount(statusViewModel.failedRecordingCount, R.id.failed_recordings)
        observeCount(statusViewModel.removedRecordingCount, R.id.removed_recordings)
    }

    private fun observeCount(countLiveData: LiveData<Int>, menuItemId: Int) {
        countLiveData.observe(activity) { count ->
            drawer.menu.findItem(menuItemId)
                ?.let { it.actionView as? TextView }
                ?.let { textView -> textView.text = count.toString() }
        }
    }

    private fun showConnectionsInDrawerHeader(active: Connection?) {
        headerBinding.switcher.isVisible = navigationViewModel.connections.size > 1
        headerBinding.serverName.text = active?.name
        headerBinding.serverUrl.text = active?.serverUrl

        val itemsToBeRemoved = drawer.menu.iterator()
            .asSequence()
            .filter { it.groupId == R.id.server_connections }
            .toList()
        itemsToBeRemoved.forEach { drawer.menu.removeItem(it.itemId) }

        navigationViewModel.connections.forEach { connection ->
            drawer.menu.add(R.id.server_connections, connection.id, Menu.NONE, connection.name)
        }
        drawer.menu.setGroupVisible(R.id.server_connections, inServerSelectionMode) // make sure newly added items aren't shown by default
    }

    fun getSelectedMenu(): Long {
        return drawer.checkedItem?.itemId?.let { ID_MAPPING[it] } ?: 0L
    }

    fun setSelectedNavigationDrawerMenuFromFragmentType(fragment: Fragment?) {
        val itemId = when (fragment) {
            is ChannelListFragment -> R.id.channels
            is EpgFragment -> R.id.epg
            is CompletedRecordingListFragment -> R.id.completed_recordings
            is ScheduledRecordingListFragment -> R.id.scheduled_recordings
            is SeriesRecordingListFragment -> R.id.series_recordings
            is TimerRecordingListFragment -> R.id.timer_recordings
            is FailedRecordingListFragment -> R.id.failed_recordings
            is RemovedRecordingListFragment -> R.id.removed_recordings
            is StatusFragment -> R.id.status
            is WebViewFragment -> R.id.help
            else -> 0
        }
        drawer.setCheckedItem(itemId)
    }

    /**
     * Creates and returns a new fragment that is associated with the given menu
     */
    private fun getFragmentFromSelectedNavigationDrawerMenu(id: Long): Fragment? {
        return when (id) {
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
    fun handleDrawerItemSelected(id: Long) {
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
                activity.supportFragmentManager.findFragmentById(R.id.details)?.let {
                    activity.supportFragmentManager.commit {
                        remove(it)
                    }
                }
            }
            activity.supportFragmentManager.commit {
                replace(R.id.main, fragment)

                if (activity.prefs.navigationHistoryEnabled) {
                    addToBackStack(null)
                }
            }

            ID_MAPPING.filterValues { it == id }.keys.firstOrNull()?.let {
                drawer.setCheckedItem(it)
            }
        }
    }

    companion object {

        // The index for the navigation drawer menus (stored in preferences)
        const val MENU_CHANNELS = 0L
        const val MENU_PROGRAM_GUIDE = 1L
        const val MENU_COMPLETED_RECORDINGS = 2L
        const val MENU_SCHEDULED_RECORDINGS = 3L
        const val MENU_SERIES_RECORDINGS = 4L
        const val MENU_TIMER_RECORDINGS = 5L
        const val MENU_FAILED_RECORDINGS = 6L
        const val MENU_REMOVED_RECORDINGS = 7L
        const val MENU_STATUS = 8L
        const val MENU_SETTINGS = 9L
        const val MENU_HELP = 10L

        // Mapping of 'internal' (resource) IDs to 'external' IDs
        private val ID_MAPPING = mapOf(
            R.id.channels to MENU_CHANNELS,
            R.id.epg to MENU_PROGRAM_GUIDE,
            R.id.completed_recordings to MENU_COMPLETED_RECORDINGS,
            R.id.scheduled_recordings to MENU_SCHEDULED_RECORDINGS,
            R.id.series_recordings to MENU_SERIES_RECORDINGS,
            R.id.timer_recordings to MENU_TIMER_RECORDINGS,
            R.id.failed_recordings to MENU_FAILED_RECORDINGS,
            R.id.removed_recordings to MENU_REMOVED_RECORDINGS,
            R.id.status to MENU_STATUS,
            R.id.settings to MENU_SETTINGS,
            R.id.help to MENU_HELP
        )
    }
}
