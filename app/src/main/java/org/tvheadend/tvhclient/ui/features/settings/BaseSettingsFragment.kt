package org.tvheadend.tvhclient.ui.features.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import org.tvheadend.tvhclient.ui.common.interfaces.ToolbarInterface
import org.tvheadend.tvhclient.util.applyNavigationBarPadding
import org.tvheadend.tvhclient.util.extensions.prefs
import timber.log.Timber

abstract class BaseSettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
    protected abstract val preferencesResId: Int
    protected abstract val titleResId: Int
    protected open val subtitle: String? = null

    lateinit var settingsViewModel: SettingsViewModel

    override fun onCreateRecyclerView(
        inflater: LayoutInflater,
        parent: ViewGroup,
        savedInstanceState: Bundle?
    ): RecyclerView {
        val view = super.onCreateRecyclerView(inflater, parent, savedInstanceState)
        view.applyNavigationBarPadding()
        return view
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(preferencesResId, rootKey)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        settingsViewModel = ViewModelProvider(activity as SettingsActivity)[SettingsViewModel::class.java]
    }

    override fun onStart() {
        super.onStart()

        (activity as? ToolbarInterface)?.let { tbi ->
            tbi.setTitle(getString(titleResId))
            tbi.setSubtitle(subtitle)
        }
    }

    override fun onResume() {
        super.onResume()
        requireActivity().prefs.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        requireActivity().prefs.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) {
        Timber.d("Preference $key has changed")
    }

    @Suppress("deprecation") // setTargetFragment is deprecated, but needed by super class
    override fun onDisplayPreferenceDialog(preference: Preference) {
        val fragment = when (preference) {
            is ListPreference -> MaterialListPreferenceDialog.create(preference)
            is EditTextPreference -> MaterialEditTextPreferenceDialog.create(preference)
            else -> null
        }
        if (fragment != null) {
            fragment.setTargetFragment(this, 0)
            fragment.show(parentFragmentManager, "list_preference_dialog")
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }
}