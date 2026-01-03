package org.tvheadend.tvhclient.ui.base

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.common.GlobalStatusViewModel
import org.tvheadend.tvhclient.ui.common.interfaces.LayoutControlInterface
import org.tvheadend.tvhclient.ui.common.interfaces.ToolbarInterface

abstract class BaseFragment : Fragment() {
    protected lateinit var baseViewModel: BaseViewModel
    protected lateinit var globalStatusViewModel: GlobalStatusViewModel
    protected lateinit var toolbarInterface: ToolbarInterface
    protected var isDualPane: Boolean = false
    protected var serverData: GlobalStatusViewModel.ConnectedServerData? = null
    protected val isConnectionToServerAvailable get() = serverData?.connected == true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (activity is ToolbarInterface) {
            toolbarInterface = activity as ToolbarInterface
        }

        baseViewModel = ViewModelProvider(requireActivity())[BaseViewModel::class.java]
        globalStatusViewModel = (requireContext().applicationContext as MainApplication).globalStatus

        globalStatusViewModel.connectedServerLiveData.observe(viewLifecycleOwner) { onConnectedServerChanged(it) }
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

    @CallSuper
    protected open fun onConnectedServerChanged(data: GlobalStatusViewModel.ConnectedServerData?) {
        serverData = data
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
