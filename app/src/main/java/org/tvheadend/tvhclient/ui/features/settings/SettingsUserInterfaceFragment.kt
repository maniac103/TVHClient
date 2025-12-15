package org.tvheadend.tvhclient.ui.features.settings

import android.os.Bundle
import android.view.View
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.util.extensions.sendSnackbarMessage
import timber.log.Timber

class SettingsUserInterfaceFragment : BaseSettingsFragment(), Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {
    override val preferencesResId = R.xml.preferences_ui
    override val titleResId = R.string.pref_user_interface

    private var programArtworkEnabledPreference: SwitchPreferenceCompat? = null
    private var castMiniControllerPreference: SwitchPreferenceCompat? = null
    private var multipleChannelTagsPreference: SwitchPreferenceCompat? = null
    private var hoursOfEpgDataPreference: EditTextPreference? = null
    private var daysOfEpgDataPreference: EditTextPreference? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        programArtworkEnabledPreference = findPreference("program_artwork_enabled")
        programArtworkEnabledPreference?.onPreferenceClickListener = this
        castMiniControllerPreference = findPreference("casting_minicontroller_enabled")
        castMiniControllerPreference?.onPreferenceClickListener = this
        multipleChannelTagsPreference = findPreference("multiple_channel_tags_enabled")
        multipleChannelTagsPreference?.onPreferenceClickListener = this

        hoursOfEpgDataPreference = findPreference("hours_of_epg_data_per_screen")
        hoursOfEpgDataPreference?.onPreferenceChangeListener = this
        daysOfEpgDataPreference = findPreference("days_of_epg_data")
        daysOfEpgDataPreference?.onPreferenceChangeListener = this
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        when (preference.key) {
            "casting" -> handlePreferenceCastingSelected()
        }
        return true
    }

    private fun handlePreferenceCastingSelected() {
        if (settingsViewModel.currentServerStatus.htspVersion < 16) {
            context?.sendSnackbarMessage(R.string.feature_not_supported_by_server)
            castMiniControllerPreference?.isChecked = false
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        Timber.d("Preference ${preference.key} changed, checking if it is valid")
        when (preference.key) {
            "hours_of_epg_data_per_screen" ->
                try {
                    val value = Integer.valueOf(newValue as String)
                    if (value < 1 || value > 24) {
                        context?.sendSnackbarMessage("The value must be an integer between 1 and 24")
                        return false
                    }
                    return true
                } catch (ex: NumberFormatException) {
                    context?.sendSnackbarMessage("The value must be an integer between 1 and 24")
                    return false
                }

            "days_of_epg_data" ->
                try {
                    val value = Integer.valueOf(newValue as String)
                    if (value < 1 || value > 14) {
                        context?.sendSnackbarMessage("The value must be an integer between 1 and 14")
                        return false
                    }
                    return true
                } catch (ex: NumberFormatException) {
                    context?.sendSnackbarMessage("The value must be an integer between 1 and 14")
                    return false
                }
            else -> return true
        }
    }
}
