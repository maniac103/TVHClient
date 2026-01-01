package org.tvheadend.tvhclient.ui.features.dvr.recordings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.ViewModelProvider
import org.tvheadend.data.entity.RecordingInterface
import org.tvheadend.data.entity.RecordingWithChannel
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.RecordingDetailsActivityBinding
import org.tvheadend.tvhclient.ui.base.BaseActivity
import org.tvheadend.tvhclient.ui.common.preparePopupOrToolbarSearchMenu
import org.tvheadend.tvhclient.ui.common.searchTitleInTheLocalDatabase
import org.tvheadend.tvhclient.ui.common.searchTitleOnFileAffinityWebsite
import org.tvheadend.tvhclient.ui.common.searchTitleOnGoogle
import org.tvheadend.tvhclient.ui.common.searchTitleOnImdbWebsite
import org.tvheadend.tvhclient.ui.common.searchTitleOnYoutube
import org.tvheadend.tvhclient.util.applyNavigationBarPadding
import timber.log.Timber

class RecordingDetailsActivity : BaseActivity() {
    private lateinit var binding: RecordingDetailsActivityBinding

    private lateinit var recordingViewModel: RecordingViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = RecordingDetailsActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar(binding.toolbar, binding.appBar)
        binding.content.applyNavigationBarPadding()

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu_cancel)
        }

        recordingViewModel = ViewModelProvider(this)[RecordingViewModel::class.java]
        recordingViewModel.currentIdLiveData.value = intent.getIntExtra("id", 0)

        recordingViewModel.recordingLiveData.observe(this) { rec ->
            Timber.d("View model returned a recording")
            if (rec == null) {
                finish()
            } else {
                supportActionBar?.apply {
                    title = rec.title
                    subtitle = rec.subtitle
                }
            }
        }
    }

    companion object {
        fun makeIntent(context: Context, recording: RecordingInterface) =
            Intent(context, RecordingDetailsActivity::class.java)
                .putExtra("id", recording.id)
    }
}
