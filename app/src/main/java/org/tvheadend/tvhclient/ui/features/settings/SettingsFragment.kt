package org.tvheadend.tvhclient.ui.features.settings

import android.content.Intent
import android.content.SharedPreferences
import android.os.*
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.app.TaskStackBuilder
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.features.MainActivity
import org.tvheadend.tvhclient.util.extensions.sendSnackbarMessage
import timber.log.Timber

class SettingsFragment : BaseSettingsFragment(), Preference.OnPreferenceClickListener, ActivityCompat.OnRequestPermissionsResultCallback {
    override val preferencesResId = R.xml.preferences
    override val titleResId = R.string.settings

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        PreferenceManager.setDefaultValues(requireActivity(), R.xml.preferences, false)

        findPreference<Preference>("list_connections")?.onPreferenceClickListener = this
        findPreference<Preference>("user_interface")?.onPreferenceClickListener = this
        findPreference<Preference>("profiles")?.onPreferenceClickListener = this
        findPreference<Preference>("playback")?.onPreferenceClickListener = this
        findPreference<Preference>("unlocker")?.onPreferenceClickListener = this
        findPreference<Preference>("advanced")?.onPreferenceClickListener = this
        findPreference<Preference>("changelog")?.onPreferenceClickListener = this
        findPreference<Preference>("language")?.onPreferenceClickListener = this
        findPreference<Preference>("selected_theme")?.onPreferenceClickListener = this
        findPreference<Preference>("information")?.onPreferenceClickListener = this
        findPreference<Preference>("privacy_policy")?.onPreferenceClickListener = this
    }

    override fun onResume() {
        super.onResume()
        updateDownloadDirSummary()
    }

    private fun updateDownloadDirSummary() {
        Timber.d("Updating download directory summary")
        val path = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Timber.d("Android API version is ${Build.VERSION.SDK_INT}, loading download folder from preference")
            sharedPreferences.getString("download_directory", Environment.DIRECTORY_DOWNLOADS)
        } else {
            Timber.d("Android API version is ${Build.VERSION.SDK_INT}, using default folder")
            Environment.DIRECTORY_DOWNLOADS
        }
        Timber.d("Setting download directory summary to $path")
        findPreference<Preference>("download_directory")?.summary = getString(R.string.pref_download_directory_sum, path)
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) {
        super.onSharedPreferenceChanged(prefs, key)
        when (key) {
            "selected_theme" -> handlePreferenceThemeChanged()
            "language" -> {
                activity?.let {
                    val intent = Intent(it, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    it.startActivity(intent)
                }
            }
        }
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        when (preference.key) {
            "profiles" -> handlePreferenceProfilesSelected()
            else -> settingsViewModel.setNavigationMenuId(preference.key)
        }
        return true
    }

    private fun handlePreferenceThemeChanged() {
        activity?.let {
            TaskStackBuilder.create(it)
                    .addNextIntent(Intent(it, MainActivity::class.java))
                    .addNextIntent(it.intent)
                    .startActivities()
        }
    }

    private fun handlePreferenceProfilesSelected() {
        if (view != null) {
            if (settingsViewModel.currentServerStatus.htspVersion < 16) {
                context?.sendSnackbarMessage(R.string.feature_not_supported_by_server)
            } else {
                settingsViewModel.setNavigationMenuId("profiles")
            }
        }
    }
}