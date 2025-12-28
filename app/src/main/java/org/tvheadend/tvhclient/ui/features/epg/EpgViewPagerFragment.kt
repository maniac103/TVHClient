package org.tvheadend.tvhclient.ui.features.epg

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.EpgViewpagerFragmentBinding
import org.tvheadend.tvhclient.util.extensions.applyText
import org.tvheadend.tvhclient.util.extensions.formatDate
import org.tvheadend.tvhclient.util.extensions.formatTime
import org.tvheadend.tvhclient.util.livedata.CombinedPairLiveData
import timber.log.Timber
import java.util.Calendar
import kotlin.time.Duration.Companion.minutes

class EpgViewPagerFragment : Fragment(), EpgScrollInterface {

    private lateinit var epgViewModel: EpgViewModel
    private lateinit var recyclerViewAdapter: EpgVerticalRecyclerViewAdapter

    private lateinit var constraintSet: ConstraintSet
    private lateinit var binding: EpgViewpagerFragmentBinding
    private var recyclerViewLinearLayoutManager: LinearLayoutManager? = null
    private var enableScrolling = false
    private var fragmentId = 0
    private var hoursPerScreen = 0
    private var startTime = 0L

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = EpgViewpagerFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("Initializing")
        epgViewModel = ViewModelProvider(requireActivity())[EpgViewModel::class.java]

        // Required to show the vertical current time indication
        constraintSet = ConstraintSet()
        constraintSet.clone(binding.constraintLayout)

        // Get the id that defines the position of the fragment in the viewpager
        fragmentId = arguments?.getInt("fragmentId") ?: 0

        recyclerViewAdapter = EpgVerticalRecyclerViewAdapter(epgViewModel, fragmentId, viewLifecycleOwner)
        recyclerViewLinearLayoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        binding.viewpagerRecyclerView.layoutManager = recyclerViewLinearLayoutManager
        binding.viewpagerRecyclerView.setHasFixedSize(true)
        binding.viewpagerRecyclerView.adapter = recyclerViewAdapter

        binding.viewpagerRecyclerView.addOnLayoutChangeListener { _, left, _, right, _, _, _, _, _ ->
            recyclerViewAdapter.viewWidth = right - left
        }

        binding.viewpagerRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState != SCROLL_STATE_IDLE) {
                    enableScrolling = true
                } else if (enableScrolling) {
                    enableScrolling = false
                    activity?.let {
                        val fragment = it.supportFragmentManager.findFragmentById(R.id.main)
                        if (fragment is EpgScrollInterface) {
                            (fragment as EpgScrollInterface).onScrollStateChanged()
                        }
                    }
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (enableScrolling) {
                    activity?.let {
                        val position = recyclerViewLinearLayoutManager?.findFirstVisibleItemPosition() ?: -1
                        val childView = recyclerViewLinearLayoutManager?.getChildAt(0)
                        val offset = if (childView == null) 0 else childView.top - recyclerView.paddingTop
                        val fragment = it.supportFragmentManager.findFragmentById(R.id.main)
                        if (fragment is EpgScrollInterface && position >= 0) {
                            (fragment as EpgScrollInterface).onScroll(position, offset)
                        }
                    }
                }
            }
        })

        val programsAndRecordingsLiveData = CombinedPairLiveData(
            epgViewModel.getFragmentLiveData(fragmentId),
            epgViewModel.recordings
        ) { programs, recordings -> programs to recordings }

        programsAndRecordingsLiveData.observe(viewLifecycleOwner) { (entries, recordings) ->
            entries.forEach { Timber.d("Loaded ${it.programs.size} programs for channel ${it.channel.name}") }
            recyclerViewAdapter.loadProgramData(entries, recordings)
            binding.progress.isVisible = false
        }

        epgViewModel.getFragmentStartAndEndTimes(fragmentId).observe(viewLifecycleOwner) { (start, end) ->
            binding.viewpagerTitleDate.applyText { formatDate(start) }
            binding.viewpagerTitleStartHours.applyText { formatTime(start) }
            binding.viewpagerTitleEndHours.applyText { formatTime(end) }
        }

        val showTimeIndication = fragmentId == 0
        binding.currentTime.isVisible = showTimeIndication

        if (showTimeIndication) {
            // Create the handler and the timer task that will update the
            // entire view every 30 minutes if the first screen is visible.
            // This prevents the time indication from moving to far to the right
            viewLifecycleOwner.lifecycleScope.launch {
                delay(1.minutes)
                while (isActive) {
                    recyclerViewAdapter.notifyDataSetChanged()
                    delay(20.minutes)
                }
            }
            viewLifecycleOwner.lifecycleScope.launch {
                setCurrentTimeIndication()
                delay(1.minutes)
            }

            epgViewModel.hoursOfEpgDataPerScreen.observe(viewLifecycleOwner) {
                hoursPerScreen = it
                setCurrentTimeIndication()
            }
            epgViewModel.getFragmentStartAndEndTimes(fragmentId).observe(viewLifecycleOwner) { (start, _) ->
                startTime = start
                setCurrentTimeIndication()
            }

            view.addOnLayoutChangeListener { _, left, _, right, _, oldLeft, _, oldRight, _ ->
                if (left != oldLeft || right != oldRight) {
                    setCurrentTimeIndication()
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        recyclerViewLinearLayoutManager?.let {
            outState.putParcelable("layout", it.onSaveInstanceState())
        }
    }

    /**
     * Shows a vertical line in the program guide to indicate the current time.
     * It is only visible in the first screen. This method is called every minute.
     */
    private fun setCurrentTimeIndication() {
        // Get the difference between the current time and the given start time. Calculate
        // from this value in minutes the width in pixels. This will be horizontal offset
        // for the time indication. If channel icons are shown then we need to add a
        // the icon width to the offset.
        val width = view?.width ?: return
        val currentTime = Calendar.getInstance().timeInMillis
        val durationTime = (currentTime - startTime) / 1000 / 60
        val pixelsPerMinute = width.toFloat() / (60.0f * hoursPerScreen.toFloat())
        val offset = (durationTime * pixelsPerMinute).toInt()
        Timber.d("Fragment id: $fragmentId, current time: $currentTime, start time: $startTime, offset: $offset, durationTime: $durationTime, pixelsPerMinute: $pixelsPerMinute")

        // Set the left constraint of the time indication so it shows the actual time
        binding.currentTime.let {
            constraintSet.connect(it.id, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT, offset)
            constraintSet.connect(it.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, offset)
            constraintSet.applyTo(binding.constraintLayout)
        }
    }

    override fun onScroll(position: Int, offset: Int) {
        recyclerViewLinearLayoutManager?.scrollToPositionWithOffset(position, offset)
    }

    override fun onScrollStateChanged() {
        // NOP
    }

    companion object {
        fun newInstance(fragmentId: Int) = EpgViewPagerFragment().apply {
            arguments = bundleOf("fragmentId" to fragmentId)
        }
    }
}
