package org.tvheadend.tvhclient.ui.features.dvr.series_recordings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import org.tvheadend.data.entity.SeriesRecordingInterface
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.SeriesRecordingDetailsActivityBinding
import org.tvheadend.tvhclient.ui.base.BaseActivity
import org.tvheadend.tvhclient.util.applyNavigationBarPadding
import timber.log.Timber

class SeriesRecordingDetailsActivity : BaseActivity() {
    private lateinit var binding: SeriesRecordingDetailsActivityBinding
    private lateinit var seriesRecordingViewModel: SeriesRecordingViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = SeriesRecordingDetailsActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar(binding.toolbar, binding.appBar)
        binding.content.applyNavigationBarPadding()

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu_cancel)
        }

        seriesRecordingViewModel = ViewModelProvider(this)[SeriesRecordingViewModel::class.java]
        seriesRecordingViewModel.currentIdLiveData.value = intent.getStringExtra("id") ?: ""

        seriesRecordingViewModel.recordingLiveData.observe(this) { rec ->
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
        fun makeIntent(context: Context, recording: SeriesRecordingInterface) =
            Intent(context, SeriesRecordingDetailsActivity::class.java)
                .putExtra("id", recording.id)
    }
}