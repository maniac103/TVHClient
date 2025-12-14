package org.tvheadend.tvhclient.ui.features.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.tvheadend.data.entity.Connection
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.util.applyNavigationBarPadding
import org.tvheadend.tvhclient.util.extensions.sendSnackbarMessage
import timber.log.Timber

class SettingsAddConnectionFragment : SettingsConnectionBaseFragment() {
    override fun onCreateRecyclerView(
        inflater: LayoutInflater,
        parent: ViewGroup,
        savedInstanceState: Bundle?
    ): RecyclerView {
        val view = super.onCreateRecyclerView(inflater, parent, savedInstanceState)
        view.applyNavigationBarPadding()
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbarInterface.setTitle(getString(R.string.add_connection))
        toolbarInterface.setSubtitle(null)

        // When the fragment is created for the first time, create a new empty
        // connection which can be populated with the new information.
        if (savedInstanceState == null) {
            settingsViewModel.connectionToEdit = Connection()
        }

        settingsViewModel.connectionCountLiveData.observe(viewLifecycleOwner) { count ->
            Timber.d("Received live data, connection count is $count, setting this one as the active")
            activeEnabledPreference.isChecked = (count == 0)
            settingsViewModel.connectionToEdit.isActive = (count == 0)
        }
    }

    override fun save() {
        when(val result = connectionValidator.isConnectionInputValid(settingsViewModel.connectionToEdit)) {
            is ValidationResult.Success -> {
                settingsViewModel.addConnection()
                activity.let {
                    if (it is RemoveFragmentFromBackstackInterface) {
                        it.removeFragmentFromBackstack()
                    }
                }
            }
            is ValidationResult.Failed -> {
                context?.sendSnackbarMessage(getErrorDescription(result.reason))
            }
        }
    }
}
