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

    private var recording: RecordingWithChannel? = null
    private var isConnectionToServerAvailable: Boolean = false

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
                recording = rec
                supportActionBar?.apply {
                    title = rec.title
                    subtitle = rec.subtitle
                }
                invalidateOptionsMenu()
            }
        }

        globalStatusViewModel.connectionToServerAvailableLiveData.observe(this) { isAvailable ->
            Timber.d("Received live data, connection to server availability changed to $isAvailable")
            isConnectionToServerAvailable = isAvailable
            invalidateOptionsMenu()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.external_search_options_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        preparePopupOrToolbarSearchMenu(menu, recording?.title, isConnectionToServerAvailable)
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }

        val recording = recording ?: return super.onOptionsItemSelected(item)

        return when (item.itemId) {
            R.id.menu_search_imdb -> searchTitleOnImdbWebsite(this, recording.title)
            R.id.menu_search_fileaffinity -> searchTitleOnFileAffinityWebsite(this, recording.title)
            R.id.menu_search_youtube -> searchTitleOnYoutube(this, recording.title)
            R.id.menu_search_google -> searchTitleOnGoogle(this, recording.title)
            R.id.menu_search_epg -> searchTitleInTheLocalDatabase(this, baseViewModel, recording.title)

            else -> return super.onOptionsItemSelected(item)
        }
    }

    companion object {
        fun makeIntent(context: Context, recording: RecordingInterface) =
            Intent(context, RecordingDetailsActivity::class.java)
                .putExtra("id", recording.id)
    }
}
