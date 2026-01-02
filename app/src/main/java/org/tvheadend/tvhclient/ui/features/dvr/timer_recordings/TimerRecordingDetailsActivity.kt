package org.tvheadend.tvhclient.ui.features.dvr.series_recordings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import org.tvheadend.data.entity.TimerRecordingInterface
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.TimerRecordingDetailsActivityBinding
import org.tvheadend.tvhclient.ui.base.BaseActivity
import org.tvheadend.tvhclient.ui.features.dvr.timer_recordings.TimerRecordingViewModel
import org.tvheadend.tvhclient.util.applyNavigationBarPadding
import timber.log.Timber

class TimerRecordingDetailsActivity : BaseActivity() {
    private lateinit var binding: TimerRecordingDetailsActivityBinding
    private lateinit var timerRecordingViewModel: TimerRecordingViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = TimerRecordingDetailsActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar(binding.toolbar, binding.appBar)
        binding.content.applyNavigationBarPadding()

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu_cancel)
        }

        timerRecordingViewModel = ViewModelProvider(this)[TimerRecordingViewModel::class.java]
        timerRecordingViewModel.currentIdLiveData.value = intent.getStringExtra("id") ?: ""

        timerRecordingViewModel.recordingLiveData.observe(this) { rec ->
            Timber.d("View model returned a recording")
            if (rec == null) {
                finish()
            } else {
                supportActionBar?.apply {
                    title = rec.title
                    subtitle = rec.name
                }
            }
        }
    }

    companion object {
        fun makeIntent(context: Context, recording: TimerRecordingInterface) =
            Intent(context, TimerRecordingDetailsActivity::class.java)
                .putExtra("id", recording.id)
    }
}