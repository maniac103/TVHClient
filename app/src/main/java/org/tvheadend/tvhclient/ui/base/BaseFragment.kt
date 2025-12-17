package org.tvheadend.tvhclient.ui.base

import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import org.tvheadend.data.entity.Connection
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.common.interfaces.LayoutControlInterface
import org.tvheadend.tvhclient.ui.common.interfaces.ToolbarInterface
import org.tvheadend.tvhclient.util.extensions.prefs
import timber.log.Timber

abstract class BaseFragment : Fragment() {
    protected lateinit var sharedPreferences: SharedPreferences
    protected lateinit var baseViewModel: BaseViewModel
    protected lateinit var toolbarInterface: ToolbarInterface
    protected var isDualPane: Boolean = false
    protected var htspVersion: Int = 13
    protected var isConnectionToServerAvailable: Boolean = false
    protected lateinit var connection: Connection

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (activity is ToolbarInterface) {
            toolbarInterface = activity as ToolbarInterface
        }

        sharedPreferences = requireContext().prefs
        baseViewModel = ViewModelProvider(requireActivity())[BaseViewModel::class.java]
        baseViewModel.connectionToServerAvailableLiveData.observe(viewLifecycleOwner) { isAvailable ->
            Timber.d("Received live data, connection to server availability changed to $isAvailable")
            isConnectionToServerAvailable = isAvailable
        }

        connection = baseViewModel.connection
        htspVersion = baseViewModel.htspVersion

        // Check if we have a frame in which to embed the details fragment.
        // Make the frame layout visible and set the weights again in case
        // it was hidden by the call to forceSingleScreenLayout()
        isDualPane = resources.getBoolean(R.bool.isDualScreen)
        if (isDualPane) {
            if (activity is LayoutControlInterface) {
                (activity as LayoutControlInterface).enableDualScreenLayout()
            }
        } else {
            if (activity is LayoutControlInterface) {
                (activity as LayoutControlInterface).enableSingleScreenLayout()
            }
        }

        setHasOptionsMenu(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                activity?.finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun removeDetailsFragment() {
        activity?.supportFragmentManager?.findFragmentById(R.id.details)?.let {
            activity?.supportFragmentManager?.commit {
                remove(it)
                setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            }
        }
    }
}
